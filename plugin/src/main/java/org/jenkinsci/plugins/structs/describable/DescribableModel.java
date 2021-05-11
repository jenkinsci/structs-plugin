package org.jenkinsci.plugins.structs.describable;

import com.google.common.primitives.Primitives;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.GString;
import hudson.ExtensionList;
import hudson.Main;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.util.LogTaskListener;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.codehaus.groovy.reflection.ReflectionCache;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.structs.SymbolLookup;
import org.jvnet.tiger_types.Types;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.ClassDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.lang.Klass;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.beans.Introspector;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jenkinsci.plugins.structs.describable.UninstantiatedDescribable.*;

/**
 * Introspects a {@link Describable} with {@link DataBoundConstructor} and {@link DataBoundSetter}.
 *
 * <p>
 * Provides such operations like
 *
 * <ul>
 *     <li>
 *         {@linkplain #instantiate(Map) instantiate this class from a JSON-like map}
 *     <li>
 *         {@linkplain #uninstantiate(Object) take an existing instance and produces a JSON-like map}
 *         that can be then fed back to the above instantiation call
 *     <li>
 *         {@linkplain #getParameters() enumerate parameters of this Describable} that are defined
 *         either through {@link DataBoundConstructor} or {@link DataBoundSetter}.
 *         See {@link DescribableParameter} for more details
 *     <li>
 *         {@linkplain #getHelp() access help file}
 * </ul>
 *
 * Note that some structures are recursive or mutually recursive.
 * It is up a caller to defend against stack overflows when traversing a model graph,
 * for example by keeping a stack of types which have already been encountered.
 *
 * @author Jesse Glick
 * @author Andrew Bayer
 * @author Kohsuke Kawaguchi
 */
@SuppressFBWarnings("SE_BAD_FIELD") // defines writeReplace
public final class DescribableModel<T> implements Serializable {
    @SuppressFBWarnings("MS_SHOULD_BE_FINAL") // Used to control warnings that instantiate parameters will be ignored
    @Restricted(NoExternalUse.class)
    public static boolean STRICT_PARAMETER_CHECKING = Main.isUnitTest;
    /**
     * Type that this model represents.
     */
    private final Class<T> type;

    private Map<String,DescribableParameter> parameters = new LinkedHashMap<String, DescribableParameter>(4);

    /**
     * Read only view to {@link #parameters}
     */
    private Map<String,DescribableParameter> parametersView;

    /**
     * Data-bound constructor.
     */
    private final Constructor<T> constructor;

    /**
     * Name of the parameters of the {@link #constructor}
     */
    private final String[] constructorParamNames;

    /** Binds type parameter, preferred means of obtaining a DescribableModel. */
    public static <T> DescribableModel<T> of(Class<T> clazz) {
        DescribableModel mod = modelCache.get(clazz.getName());
        if (mod != null && mod.type == clazz) {
            return mod;
        }
        mod = new DescribableModel<T>(clazz);
        modelCache.put(clazz.getName(), mod);
        return mod;
    }

    /** Map class name to cached model. */
    static ConcurrentHashMap<String, DescribableModel> modelCache = new ConcurrentHashMap<String, DescribableModel>();

    /**
     * Loads a definition of the structure of a class: what kind of data
     * you might get back from {@link #uninstantiate} on an instance,
     * or might want to pass to {@link #instantiate(Map)}.
     *
     * Use {@link #of(Class)} instead -- that will returned cached instances.
     */
    public DescribableModel(Class<T> clazz) {
        this.type = clazz;

        DescribableModel mod = modelCache.get(clazz.getName());
        if (mod != null && mod.type == clazz) {
            constructor = mod.constructor;
            parameters = mod.parameters;
            constructorParamNames = mod.constructorParamNames;
            parametersView = mod.parametersView;
            return;
        }

        if (type == ParametersDefinitionProperty.class) { // TODO pending core fix
            constructorParamNames = new String[] {"parameterDefinitions"};
        } else {
            constructorParamNames = new ClassDescriptor(type).loadConstructorParamNames();
        }

        constructor = findConstructor(constructorParamNames.length);

        Type[] types = constructor.getGenericParameterTypes();
        for (int i = 0; i < constructorParamNames.length; i++) {
            addParameter(parameters, types[i], constructorParamNames[i], null);
        }

        // rest of the properties will be sorted alphabetically
        Map<String,DescribableParameter> rest = new TreeMap<String, DescribableParameter>();

        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.isAnnotationPresent(DataBoundSetter.class)) {
                    addParameter(rest, f.getGenericType(), f.getName(), Setter.create(f));
                }
            }
            for (Method m : c.getDeclaredMethods()) {
                if (m.isAnnotationPresent(DataBoundSetter.class)) {
                    Type[] parameterTypes = m.getGenericParameterTypes();
                    if (!m.getName().startsWith("set") || parameterTypes.length != 1) {
                        throw new IllegalStateException(m + " cannot be a @DataBoundSetter");
                    }
                    addParameter(rest, m.getGenericParameterTypes()[0],
                            Introspector.decapitalize(m.getName().substring(3)), Setter.create(m));
                }
            }
        }
        parameters.putAll(rest);
        parametersView = Collections.unmodifiableMap(parameters);
        modelCache.putIfAbsent(clazz.getName(), this);
    }

    private void addParameter(Map<String,DescribableParameter> props, Type type, String name, Setter setter) {
        props.put(name, new DescribableParameter(this, type, name, setter));
    }

    /**
     * A concrete class, usually {@link Describable}.
     */
    public Class<T> getType() {
        return type;
    }

    /**
     * A map from parameter names to types.
     * A parameter name is either the name of an argument to a {@link DataBoundConstructor},
     * or the JavaBeans property name corresponding to a {@link DataBoundSetter}.
     *
     * <p>
     * Sorted by the mandatory parameters first (in the order they are specified in the code),
     * followed by optional arguments.
     */
    public Collection<DescribableParameter> getParameters() {
        return parametersView.values();
    }

    public DescribableParameter getParameter(String name) {
        return parameters.get(name);
    }

    /**
     * Returns true if this model has one and only one required parameter.
     *
     * @see UninstantiatedDescribable#ANONYMOUS_KEY
     */
    public boolean hasSingleRequiredParameter() {
        return getSoleRequiredParameter()!=null;
    }

    /**
     * If this model has one and only one required parameter, return it.
     * Otherwise null.
     *
     * @see UninstantiatedDescribable#ANONYMOUS_KEY
     */
    public @CheckForNull DescribableParameter getSoleRequiredParameter() {
        DescribableParameter rp = null;
        for (DescribableParameter p : getParameters()) {
            if (p.isRequired()) {
                if (rp!=null)   return null;
                rp = p;
            }
        }
        return rp;
    }

    /**
     * If this model has any required parameter, return the first one.
     * Otherwise null.
     */
    public @CheckForNull DescribableParameter getFirstRequiredParameter() {
        for (DescribableParameter p : getParameters()) {
            if (p.isRequired()) {
                return p;
            }
        }
        return null;
    }

    /**
     * Corresponds to {@link Descriptor#getDisplayName} where available.
     */
    public String getDisplayName() {
        for (Descriptor<?> d : ExtensionList.lookup(Descriptor.class)) {
            if (d.clazz == type) {
                return d.getDisplayName();
            }
        }
        return type.getSimpleName();
    }

    /**
     * @deprecated instead use {@link #instantiate(Map, TaskListener)}
     */
    @Deprecated
    public T instantiate(Map<String,?> arguments) throws IllegalArgumentException {
        return instantiate(arguments, new LogTaskListener(LOGGER, Level.FINE));
    }

    /**
     * Creates an instance of a class via {@link DataBoundConstructor} and {@link DataBoundSetter}.
     * <p>
     * The arguments may be primitives (as wrappers) or {@link String}s if that is their declared type.
     * {@link Character}s, {@link Enum}s, and {@link URL}s may be represented by {@link String}s.
     * Other object types may be passed in “raw” as well, but JSON-like structures are encouraged instead.
     * Specifically a {@link List} may be used to represent any list- or array-valued argument.
     * A {@link Map} with {@link String} keys may be used to represent any class which is itself data-bound.
     * In that case the special key {@link #CLAZZ} is used to specify the {@link Class#getName};
     * or it may be omitted if the argument is declared to take a concrete type;
     * or {@link Class#getSimpleName} may be used in case the argument type is {@link Describable}
     * and only one subtype is registered (as a {@link Descriptor}) with that simple name.
     *
     * @param arguments
     *      The arguments used to create the instance
     * @param listener
     *      Listener to record any instantiation warnings
     * @return
     *      The instantiated object
     * @throws IllegalArgumentException
     */
    public T instantiate(Map<String,?> arguments, @CheckForNull TaskListener listener) throws IllegalArgumentException {
        if (listener == null) {
            listener = new LogTaskListener(LOGGER, Level.WARNING);
        }

        CustomDescribableModel cdm = CustomDescribableModel.of(type);
        if (cdm != null) {
            Map<String, Object> input = deeplyImmutable(arguments);
            arguments = cdm.customInstantiate(input);
            LOGGER.log(Level.FINE, "{0} translated {1} to {2}", new Object[] {cdm.getClass(), input, arguments});
        }
        if (arguments.containsKey(ANONYMOUS_KEY)) {
            if (arguments.size()!=1)
                throw new IllegalArgumentException("All arguments have to be named but it has "+ANONYMOUS_KEY);

            DescribableParameter rp = getSoleRequiredParameter();
            if (rp==null)
                throw new IllegalArgumentException("Arguments to "+type+" have to be explicitly named");
            arguments = Collections.singletonMap(rp.getName(),arguments.get(ANONYMOUS_KEY));
        }

        Set<String> erroneous =  new TreeSet<>(arguments.keySet());
        erroneous.removeAll(parameters.keySet());
        if (erroneous.size() > 0) {
            String msg = "WARNING: Unknown parameter(s) found for class type '" + this.type.getName() + "': " + String.join(",", erroneous);
            if (STRICT_PARAMETER_CHECKING) {
                throw new IllegalArgumentException(msg);
            } else {
                listener.getLogger().println(msg);
            }
        }

        try {
            Object[] args = buildArguments(arguments, constructor.getGenericParameterTypes(), constructorParamNames, true, listener);
            T o = constructor.newInstance(args);
            injectSetters(o, arguments, listener);
            return o;
        } catch (Exception x) {
            throw new IllegalArgumentException("Could not instantiate " + arguments + " for " + this.type.getName() + ": " + x, x);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> deeplyImmutable(Map<String, ?> m) {
        Map<String, Object> r = new HashMap<>();
        for (Map.Entry<String, ?> e : m.entrySet()) {
            Object v = e.getValue();
            if (v instanceof UninstantiatedDescribable) {
                v = deeplyImmutable((UninstantiatedDescribable) v);
            } else if (v instanceof Map) {
                v = deeplyImmutable((Map) v);
            }
            r.put(e.getKey(), v);
        }
        return Collections.unmodifiableMap(r);
    }

    private static UninstantiatedDescribable deeplyImmutable(UninstantiatedDescribable ud) {
        return ud.withArguments(deeplyImmutable(ud.getArguments()));
    }

        // adapted from RequestImpl
    @SuppressWarnings("unchecked")
    private Constructor<T> findConstructor(int length) {
        try { // may work without this, but only if the JVM happens to return the right overload first
            if (type == ParametersDefinitionProperty.class && length == 1) { // TODO pending core fix
                return (Constructor<T>) ParametersDefinitionProperty.class.getConstructor(List.class);
            }
        } catch (NoSuchMethodException x) {
            throw new AssertionError(x);
        }
        Constructor<T>[] ctrs = (Constructor<T>[]) type.getConstructors();
        for (Constructor<T> c : ctrs) {
            if (c.getAnnotation(DataBoundConstructor.class) != null) {
                if (c.getParameterTypes().length != length) {
                    throw new IllegalArgumentException(c + " has @DataBoundConstructor but it doesn't match with your .stapler file. Try clean rebuild");
                }
                return c;
            }
        }
        for (Constructor<T> c : ctrs) {
            if (c.getParameterTypes().length == length) {
                return c;
            }
        }
        throw new IllegalArgumentException(type + " does not have a constructor with " + length + " arguments");
    }

    /**
     * Give a method/constructor, take values specified in the bag and build up the arguments to invoke it with.
     *
     * @param types
     *      Types of the parameters
     * @param names
     *      Names of the parameters
     * @param callEvenIfNoArgs
     *      true for constructor, false for a method call
     * @param listener
     *      The listener to log information
     * @return
     *      null if the method shouldn't be invoked at all. IOW, there's nothing in the bag.
     */
    private Object[] buildArguments(Map<String,?> bag, Type[] types, String[] names, boolean callEvenIfNoArgs, TaskListener listener) throws Exception {
        assert names.length==types.length;

        Object[] args = new Object[names.length];
        boolean hasArg = callEvenIfNoArgs;
        for (int i = 0; i < args.length; i++) {
            String name = names[i];
            hasArg |= bag.containsKey(name);
            Object a = bag.get(name);
            Type type = types[i];
            if (a != null) {
                args[i] = coerce(this.type.getName() + "." + name, type, a, listener);
            } else if (type instanceof Class && ((Class) type).isPrimitive()) {
                args[i] = getVmDefaultValueForPrimitiveType((Class)type);
                if (args[i]==null && callEvenIfNoArgs)
                    throw new UnsupportedOperationException("not yet handling @DataBoundConstructor default value of " + type + "; pass an explicit value for " + name);
            } else {
                // TODO this might be fine (ExecutorStep.label), or not (GenericSCMStep.scm); should inspect parameter annotations for @Nonnull and throw an UOE if found
            }
        }
        return hasArg ? args : null;
    }

    /**
     * Injects via {@link DataBoundSetter}
     */
    private void injectSetters(Object o, Map<String,?> arguments, TaskListener listener) throws Exception {
        for (DescribableParameter p : parameters.values()) {
            if (p.setter!=null) {
                if (arguments.containsKey(p.getName())) {
                    Object v = arguments.get(p.getName());
                    p.setter.set(o, coerce(p.setter.getDisplayName(), p.getRawType(), v, listener));
                }
            }
        }
    }

    /**
     * Take an object of random type and tries to convert it into another type
     *
     * @param context
     *      Human readable location of coercion when reporting a problem.
     * @param type
     *      The type to convert the object to.
     * @param o
     *      Source object to be converted.
     * @param listener
     *      The listener to log information
     */
    @SuppressWarnings("unchecked")
    private Object coerce(String context, Type type, Object o, TaskListener listener) throws Exception {
        Class erased = Types.erasure(type);

        if (type instanceof Class) {
            o = ReflectionCache.getCachedClass(erased).coerceArgument(o);
        }
        if (o instanceof GString) {
            o = o.toString();
        }
        if (o instanceof List && Collection.class.isAssignableFrom(erased)) {
            return coerceList(context,
                    Types.getTypeArgument(Types.getBaseClass(type, Collection.class),
                            0, Object.class), (List) o, listener);
        } else if (Primitives.wrap(erased).isInstance(o)) {
            return o;
        } else if (o==null) {
            return null;
        } else if (o instanceof UninstantiatedDescribable) {
            return ((UninstantiatedDescribable)o).instantiate(erased, listener);
        } else if (o instanceof Map) {
            Map<String,Object> m = new HashMap<String,Object>();
            for (Map.Entry<?,?> entry : ((Map<?,?>) o).entrySet()) {
                m.put((String) entry.getKey(), entry.getValue());
            }

            Class<?> clazz = resolveClass(erased, (String) m.remove(CLAZZ), null);
            return new DescribableModel(clazz).instantiate(m, listener);
        } else if (o instanceof String && erased.isEnum()) {
            return Enum.valueOf(erased.asSubclass(Enum.class), (String) o);
        } else if (o instanceof String && erased == URL.class) {
            return new URL((String) o);
        } else if (o instanceof String && erased == Result.class) {
            return Result.fromString((String)o);
        } else if (o instanceof String && (erased == char.class || erased == Character.class) && ((String) o).length() == 1) {
            return ((String) o).charAt(0);
        } else if (o instanceof String && ClassUtils.isAssignable(ClassUtils.primitiveToWrapper(erased), Number.class)) {
            return coerceStringToNumber(context, ClassUtils.primitiveToWrapper(erased), (String)o);
        } else if (o instanceof String && (erased == boolean.class || erased == Boolean.class)) {
            return Boolean.valueOf((String)o);
        } else if (o instanceof List && erased.isArray()) {
            Class<?> componentType = erased.getComponentType();
            List<Object> list = coerceList(context, componentType, (List) o, listener);
            return list.toArray((Object[]) Array.newInstance(componentType, list.size()));
        } else {
            throw new ClassCastException(context + " expects " + type + " but received " + o.getClass());
        }
    }

    private Object coerceStringToNumber(@Nonnull String context, @Nonnull Class numberClass, @Nonnull String o)
            throws ClassCastException {
        try {
            if (numberClass.equals(Integer.class)) {
                return Integer.valueOf(o);
            } else if (numberClass.equals(Float.class)) {
                return Float.valueOf(o);
            } else if (numberClass.equals(Double.class)) {
                return Double.valueOf(o);
            } else if (numberClass.equals(Long.class)) {
                return Long.valueOf(o);
            } else if (numberClass.equals(Byte.class)) {
                return Byte.valueOf(o);
            } else if (numberClass.equals(Short.class)) {
                return Short.valueOf(o);
            } else {
                // Fallback for any other Number - just return the original string.
                return o;
            }
        } catch (NumberFormatException nfe) {
            throw new ClassCastException(context + " expects " + numberClass + " but was unable to coerce the received value \"" + o + "\" to that type");
        }
    }

    /**
     * Resolves a class name to an actual {@link Class} object.
     *
     * @param symbol
     *      {@linkplain Symbol symbol name} of the class to resolve.
     * @param name
     *      Either a simple name or a fully qualified class name.
     * @param base
     *      Signature of the type that the resolved class should be assignable to.
     */
    /*package*/ static Class<?> resolveClass(Class<?> base, @Nullable String name, @Nullable String symbol) throws ClassNotFoundException {
        // TODO: if both name & symbol are present, should we verify its consistency?

        if (name != null) {
            if (name.contains(".")) {// a fully qualified name
                Jenkins j = Jenkins.getInstanceOrNull();
                ClassLoader loader = j != null ? j.getPluginManager().uberClassLoader : Thread.currentThread().getContextClassLoader();
                return Class.forName(name, true, loader);
            } else {
                Class<?> clazz = null;
                for (Class<?> c : findSubtypes(base)) {
                    if (c.getSimpleName().equals(name)) {
                        if (clazz != null) {
                            throw new UnsupportedOperationException(name + " as a " + base + " could mean either " + clazz.getName() + " or " + c.getName());
                        }
                        clazz = c;
                    }
                }
                if (clazz == null) {
                    throw new UnsupportedOperationException("no known implementation of " + base + " is named " + name);
                }
                return clazz;
            }
        }

        if (symbol != null) {
            // The normal case: the Descriptor is marked, but the name applies to its Describable.
            Descriptor d = SymbolLookup.get().findDescriptor(base, symbol);
            if (d != null) {
                return d.clazz;
            }
            if (base == ParameterValue.class) { // TODO JENKINS-26093 workaround
                d = SymbolLookup.get().findDescriptor(ParameterDefinition.class, symbol);
                if (d != null) {
                    Class<?> c = parameterValueClass(d.clazz);
                    if (c != null) {
                        return c;
                    }
                }
            }
            throw new UnsupportedOperationException("no known implementation of " + base + " is using symbol ‘" + symbol + "’");
        }

        if (Modifier.isAbstract(base.getModifiers())) {
            throw new UnsupportedOperationException("must specify " + CLAZZ + " with an implementation of " + base);
        }
        return base;
    }

    /**
     * Apply {@link #coerce(String, Type, Object, TaskListener)} method to a collection item.
     */
    private List<Object> coerceList(String context, Type type, List<?> list, TaskListener listener) throws Exception {
        List<Object> r = new ArrayList<Object>();
        for (Object elt : list) {
            r.add(coerce(context, type, elt, listener));
        }
        return r;
    }

    /** Tries to find the {@link ParameterValue} type corresponding to a {@link ParameterDefinition} by assuming conventional naming. */
    private static @CheckForNull Class<?> parameterValueClass(@Nonnull Class<?> parameterDefinitionClass) { // TODO JENKINS-26093
        String name = parameterDefinitionClass.getName();
        if (name.endsWith("Definition")) {
            try {
                Class<?> parameterValueClass = parameterDefinitionClass.getClassLoader().loadClass(name.replaceFirst("Definition$", "Value"));
                if (ParameterValue.class.isAssignableFrom(parameterValueClass)) {
                    return parameterValueClass;
                }
            } catch (ClassNotFoundException x) {
                // ignore
            }
        }
        return null;
    }

    static Set<Class<?>> findSubtypes(Type supertype) {
        Set<Class<?>> clazzes = new HashSet<Class<?>>();
        // Jenkins.getDescriptorList does not work well since it is limited to descriptors declaring one supertype, and does not work at all for SimpleBuildStep.
        for (Descriptor<?> d : ExtensionList.lookup(Descriptor.class)) {
            if (TypeUtils.isAssignable(d.clazz, supertype)) {
                clazzes.add(d.clazz);
            }
        }
        if (supertype == ParameterValue.class) { // TODO JENKINS-26093 hack, pending core change
            for (Class<?> d : findSubtypes(ParameterDefinition.class)) {
                Class<?> c = parameterValueClass(d);
                if (c != null) {
                    clazzes.add(c);
                }
            }
        }
        return clazzes;
    }

    /**
     * Computes arguments suitable to pass to {@link #instantiate} to reconstruct this object.
     * @param o a data-bound object
     * @return constructor and/or setter parameters
     * @throws UnsupportedOperationException if the class does not follow the expected structure
     * @deprecated as of 1.2
     *      Use {@link #uninstantiate2(Object)}
     */
    public Map<String,Object> uninstantiate(T o) throws UnsupportedOperationException {
        return uninstantiate2(o).toMap();
    }

    /**
     * Disects a given instance into {@link UninstantiatedDescribable} that you can re-instantiate
     * via {@link UninstantiatedDescribable#instantiate()}.
     *
     * @param o a data-bound object
     * @return constructor and/or setter parameters
     * @throws UnsupportedOperationException if the class does not follow the expected structure
     */
    public UninstantiatedDescribable uninstantiate2(T o) throws UnsupportedOperationException {
        if (o==null)
            throw new IllegalArgumentException("Expected "+type+" but got null");
        if (!type.isInstance(o))
            throw new IllegalArgumentException("Expected "+type+" but got an instance of "+o.getClass());

        Map<String, Object> r = new TreeMap<String, Object>();
        Map<String, Object> constructorOnlyDataBoundProps = new TreeMap<String, Object>();
        Map<String, Object> nonDeprecatedDataBoundProps = new TreeMap<String, Object>();
        for (DescribableParameter p : parameters.values()) {
            Object v = p.inspect(o);
            if (p.isRequired() && v==null) {
                // instantiate() method treats missing properties as nulls, so we don't need to keep it
                // but if it's for the setter, explicit null invocation is needed, so we need to keep it
                continue;
            }
            r.put(p.getName(), v);
            if (p.isRequired()) {
                constructorOnlyDataBoundProps.put(p.getName(),v);
            }
            if (!p.isDeprecated()) {
                nonDeprecatedDataBoundProps.put(p.getName(),v);
            }
        }

        Object control = null;
        try {
            control = instantiate(constructorOnlyDataBoundProps, null);
        } catch (Exception x) {
            LOGGER.log(Level.WARNING, "Cannot create control version of " + type + " using " + constructorOnlyDataBoundProps, x);
        }

        if (control!=null) {
            for (DescribableParameter p : parameters.values()) {
                if (p.isRequired())
                    continue;

                Object v = p.inspect(control);

                // if the control has the same value as our object, we won't need to keep it
                if (ObjectUtils.equals(v, r.get(p.getName()))) {
                    r.remove(p.getName());
                    nonDeprecatedDataBoundProps.remove(p.getName());
                }
            }
        }

        if (!nonDeprecatedDataBoundProps.keySet().equals(r.keySet())) {
            // we have some deprecated properties
            control = null;
            try {
                control = instantiate(nonDeprecatedDataBoundProps, null);
            } catch (Exception x) {
                LOGGER.log(Level.WARNING,
                        "Cannot create control version of " + type + " using " + nonDeprecatedDataBoundProps, x);
            }

            if (control != null) {
                for (DescribableParameter p : parameters.values()) {
                    if (!p.isDeprecated())
                        continue;

                    Object v = p.inspect(control);

                    // if the control has the same value as our object, we won't need to keep it
                    if (ObjectUtils.equals(v, r.get(p.getName()))) {
                        r.remove(p.getName());
                    }
                }
            }
        }
        UninstantiatedDescribable ud = new UninstantiatedDescribable(symbolOf(o), null, r);
        ud.setModel(this);
        CustomDescribableModel cdm = CustomDescribableModel.of(type);
        if (cdm != null) {
            UninstantiatedDescribable input = deeplyImmutable(ud);
            ud = cdm.customUninstantiate(input);
            LOGGER.log(Level.FINE, "{0} translated {1} to {2}", new Object[] {cdm.getClass(), input, ud});
        }
        return ud;
    }

    /**
     * Finds a symbol for an instance if there's one, or return null.
     */
    /*package*/ static String symbolOf(Object o) {
        Set<String> symbols = SymbolLookup.getSymbolValue(o);
        return symbols.isEmpty() ? null : symbols.iterator().next();
    }

    /**
     * In case if you just need to uninstantiate one object and be done with it.
     *
     * @deprecated as of 1.2. Use {@link #uninstantiate2_(Object)}
     */
    public static Map<String,Object> uninstantiate_(Object o) {
        return uninstantiate__(o, o.getClass());
    }
    private static <T> Map<String,Object> uninstantiate__(Object o, Class<T> clazz) {
        return of(clazz).uninstantiate(clazz.cast(o));
    }

    /**
     * In case if you just need to uninstantiate one object and be done with it.
     */
    @SuppressWarnings("unchecked")
    public static UninstantiatedDescribable uninstantiate2_(Object o) {
        return new DescribableModel(o.getClass()).uninstantiate2(o);
    }

    /**
     * True if this model's type is deprecated.
     *
     * A model is deprecated if it's {@link #getType() type} is marked as {@link Deprecated}.
     */
    public boolean isDeprecated() {
        return type.getAnnotation(Deprecated.class) != null;
    }

    /**
     * Loads help defined for this object as a whole
     *
     * @return some HTML (in English locale), if available, else null
     * @see Descriptor#doHelp
     */
    public @CheckForNull String getHelp() throws IOException {
        return getHelp("help.html");
    }

    /*package*/ @CheckForNull
    String getHelp(String name) throws IOException {
        for (Klass<?> c = Klass.java(type); c != null; c = c.getSuperClass()) {
            URL u = c.getResource(name);
            if (u != null) {
                return IOUtils.toString(u, "UTF-8");
            }
        }
        return null;
    }

    void toString(StringBuilder b, Stack<Class<?>> modelTypes) {
        b.append(type.getSimpleName());
        if (modelTypes.contains(type)) {
            b.append('…');
        } else {
            modelTypes.push(type);
            try {
                b.append('(');
                boolean first = true;
                for (DescribableParameter dp : getParameters()) {
                    if (first) {
                        first = false;
                    } else {
                        b.append(", ");
                    }
                    dp.toString(b, modelTypes);
                }
                b.append(')');
            } finally {
                modelTypes.pop();
            }
        }
    }

    @Override public String toString() {
        StringBuilder b = new StringBuilder();
        toString(b, new Stack<Class<?>>());
        return b.toString();
    }

    private Object writeReplace() {
        return new SerializedForm(type);
    }

    /**
     * Serialized form of {@link DescribableModel}, which is just its class as everything else
     * can be computed.
     */
    private static class SerializedForm implements Serializable {
        private final Class type;

        public SerializedForm(Class type) {
            this.type = type;
        }

        private Object readResolve() {
            return DescribableModel.of(type);
        }

        private static final long serialVersionUID = 1L;
    }

    public static final String CLAZZ = "$class";

    private static final Logger LOGGER = Logger.getLogger(DescribableModel.class.getName());

    private static final long serialVersionUID = 1L;

    // TODO: switch to use {@link hudson.util.ReflectionUtils}
    /**
     * Given the primitive type, returns the VM default value for that type in a boxed form.
     */
    private static Object getVmDefaultValueForPrimitiveType(Class<?> type) {
        return defaultPrimitiveValue.get(type);
    }

    private static final Map<Class,Object> defaultPrimitiveValue = new HashMap<Class, Object>();
    static {
        defaultPrimitiveValue.put(boolean.class, false);
        defaultPrimitiveValue.put(byte.class, (byte) 0);
        defaultPrimitiveValue.put(short.class, (short) 0);
        defaultPrimitiveValue.put(int.class, 0);
        defaultPrimitiveValue.put(long.class, 0L);
        defaultPrimitiveValue.put(float.class, 0.0f);
        defaultPrimitiveValue.put(double.class, 0.0d);
    }
}
