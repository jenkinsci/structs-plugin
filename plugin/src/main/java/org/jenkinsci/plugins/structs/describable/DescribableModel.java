package org.jenkinsci.plugins.structs.describable;

import com.google.common.primitives.Primitives;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersDefinitionProperty;
import hudson.util.ReflectionUtils;
import jenkins.model.Jenkins;
import net.java.sezpoz.Index;
import net.java.sezpoz.IndexItem;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.reflection.ReflectionCache;
import org.jvnet.tiger_types.Types;
import org.kohsuke.stapler.ClassDescriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.lang.Klass;

import javax.annotation.CheckForNull;
import java.beans.Introspector;
import java.io.IOException;
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
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Introspects a {@link Describable} with {@link DataBoundConstructor} and {@link DataBoundSetter}
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
 *         {@linkplain #getHelp() qccess help file}
 * </ul>
 *
 * @author Jesse Glick
 * @author Anderw Bayer
 * @author Kohsuke Kawaguchi
 */
public final class DescribableModel<T> {
    /**
     * Type that this model represents.
     */
    private final Class<T> type;

    private final Map<String,DescribableParameter> parameters = new LinkedHashMap<String, DescribableParameter>();

    /**
     * Read only view to {@link #parameters}
     */
    private final Map<String,DescribableParameter> parametersView = Collections.unmodifiableMap(parameters);

    /**
     * Data-bound constructor.
     */
    private final Constructor<T> constructor;

    /**
     * Name of the parameters of the {@link #constructor}
     */
    private final String[] constructorParamNames;

    /**
     * Loads a definition of the structure of a class: what kind of data
     * you might get back from {@link #uninstantiate} on an instance,
     * or might want to pass to {@link #instantiate(Map)}.
     */
    public DescribableModel(Class<T> clazz) {
        this.type = clazz;

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
     * Corresponds to {@link Descriptor#getDisplayName} where available.
     */
    public String getDisplayName() {
        for (Descriptor<?> d : getDescriptorList()) {
            if (d.clazz == type) {
                return d.getDisplayName();
            }
        }
        return type.getSimpleName();
    }

    /**
     * Creates an instance of a class via {@link DataBoundConstructor} and {@link DataBoundSetter}.
     * <p>The arguments may be primitives (as wrappers) or {@link String}s if that is their declared type.
     * {@link Character}s, {@link Enum}s, and {@link URL}s may be represented by {@link String}s.
     * Other object types may be passed in “raw” as well, but JSON-like structures are encouraged instead.
     * Specifically a {@link List} may be used to represent any list- or array-valued argument.
     * A {@link Map} with {@link String} keys may be used to represent any class which is itself data-bound.
     * In that case the special key {@link #CLAZZ} is used to specify the {@link Class#getName};
     * or it may be omitted if the argument is declared to take a concrete type;
     * or {@link Class#getSimpleName} may be used in case the argument type is {@link Describable}
     * and only one subtype is registered (as a {@link Descriptor}) with that simple name.
     */
    public T instantiate(Map<String,?> arguments) throws Exception {
        Object[] args = buildArguments(arguments, constructor.getGenericParameterTypes(), constructorParamNames, true);
        T o = constructor.newInstance(args);
        injectSetters(o, arguments);
        return o;
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
     * @return
     *      null if the method shouldn't be invoked at all. IOW, there's nothing in the bag.
     */
    private Object[] buildArguments(Map<String,?> bag, Type[] types, String[] names, boolean callEvenIfNoArgs) throws Exception {
        assert names.length==types.length;

        Object[] args = new Object[names.length];
        boolean hasArg = callEvenIfNoArgs;
        for (int i = 0; i < args.length; i++) {
            String name = names[i];
            hasArg |= bag.containsKey(name);
            Object a = bag.get(name);
            Type type = types[i];
            if (a != null) {
                args[i] = coerce(this.type.getName() + "." + name, type, a);
            } else if (type instanceof Class && ((Class) type).isPrimitive()) {
                args[i] = ReflectionUtils.getVmDefaultValueForPrimitiveType((Class)type);
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
    private void injectSetters(Object o, Map<String,?> arguments) throws Exception {
        for (DescribableParameter p : parameters.values()) {
            if (p.setter!=null) {
                if (arguments.containsKey(p.getName())) {
                    Object v = arguments.get(p.getName());
                    p.setter.set(o, coerce(p.setter.getDisplayName(), p.getRawType(), v));
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
     */
    @SuppressWarnings("unchecked")
    private Object coerce(String context, Type type, Object o) throws Exception {
        Class erased = Types.erasure(type);

        if (type instanceof Class) {
            o = ReflectionCache.getCachedClass(erased).coerceArgument(o);
        }
        if (erased.isPrimitive() && Primitives.wrap(erased).isInstance(o)) {
            return o;
        } else if (o==null) {
            return null;
        } else if (o instanceof Map) {
            Map<String,Object> m = new HashMap<String,Object>();
            for (Map.Entry<?,?> entry : ((Map<?,?>) o).entrySet()) {
                m.put((String) entry.getKey(), entry.getValue());
            }

            Class<?> clazz = resolveClass(erased, (String) m.remove(CLAZZ));
            return new DescribableModel(clazz).instantiate(m);
        } else if (o instanceof String && erased.isEnum()) {
            return Enum.valueOf(erased.asSubclass(Enum.class), (String) o);
        } else if (o instanceof String && erased == URL.class) {
            return new URL((String) o);
        } else if (o instanceof String && (erased == char.class || erased == Character.class) && ((String) o).length() == 1) {
            return ((String) o).charAt(0);
        } else if (o instanceof List && erased.isArray()) {
            Class<?> componentType = erased.getComponentType();
            List<Object> list = coerceList(context, componentType, (List) o);
            return list.toArray((Object[]) Array.newInstance(componentType, list.size()));
        } else if (o instanceof List && Collection.class.isAssignableFrom(erased)) {
            return coerceList(context,
                    Types.getTypeArgument(Types.getBaseClass(type,Collection.class), 0, Object.class), (List) o);
        } else if (erased.isInstance(o)) {
            return o;
        } else {
            throw new ClassCastException(context + " expects " + type + " but received " + o.getClass());
        }
    }

    /**
     * Resolves a class name to an actual {@link Class} object.
     *
     * @param name
     *      Either a simple name or a fully qualified class name.
     * @param base
     *      Signature of the type that the resolved class should be assignable to.
     */
    private Class<?> resolveClass(Class base, String name) throws ClassNotFoundException {
        if (name == null) {
            if (Modifier.isAbstract(base.getModifiers())) {
                throw new UnsupportedOperationException("must specify " + CLAZZ + " with an implementation of " + base);
            }
            return base;
        } else if (name.contains(".")) {// a fully qualified name
            Jenkins j = Jenkins.getInstance();
            ClassLoader loader = j != null ? j.getPluginManager().uberClassLoader : Thread.currentThread().getContextClassLoader();
            return Class.forName(name,true,loader);
        } else {
            Class<?> clazz = null;
            for (Class<?> c : findSubtypes(base)) {
                if (c.getSimpleName().equals(name)) {
                    if (clazz != null) {
                        throw new UnsupportedOperationException(name + " as a " + base +  " could mean either " + clazz.getName() + " or " + c.getName());
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

    /**
     * Apply {@link #coerce(String, Type, Object)} method to a collection item.
     */
    private List<Object> coerceList(String context, Type type, List<?> list) throws Exception {
        List<Object> r = new ArrayList<Object>();
        for (Object elt : list) {
            r.add(coerce(context, type, elt));
        }
        return r;
    }

    static Set<Class<?>> findSubtypes(Class<?> supertype) {
        Set<Class<?>> clazzes = new HashSet<Class<?>>();
        for (Descriptor<?> d : getDescriptorList()) {
            if (supertype.isAssignableFrom(d.clazz)) {
                clazzes.add(d.clazz);
            }
        }
        if (supertype == ParameterValue.class) { // TODO JENKINS-26093 hack, pending core change
            for (Class<?> d : findSubtypes(ParameterDefinition.class)) {
                String name = d.getName();
                if (name.endsWith("Definition")) {
                    try {
                        Class<?> c = d.getClassLoader().loadClass(name.replaceFirst("Definition$", "Value"));
                        if (supertype.isAssignableFrom(c)) {
                            clazzes.add(c);
                        }
                    } catch (ClassNotFoundException x) {
                        // ignore
                    }
                }
            }
        }
        return clazzes;
    }

    @SuppressWarnings("rawtypes")
    private static List<? extends Descriptor> getDescriptorList() {
        Jenkins j = Jenkins.getInstance();
        if (j != null) {
            // Jenkins.getDescriptorList does not work well since it is limited to descriptors declaring one supertype, and does not work at all for SimpleBuildStep.
            return j.getExtensionList(Descriptor.class);
        } else {
            // TODO should be part of ExtensionList.lookup in core, but here now for benefit of tests:
            List<Descriptor<?>> descriptors = new ArrayList<Descriptor<?>>();
            for (IndexItem<Extension,Object> item : Index.load(Extension.class, Object.class)) {
                try {
                    Object o = item.instance();
                    if (o instanceof Descriptor) {
                        descriptors.add((Descriptor) o);
                    }
                } catch (InstantiationException x) {
                    // ignore for now
                }
            }
            return descriptors;
        }
    }


    /**
     * Computes arguments suitable to pass to {@link #instantiate} to reconstruct this object.
     * @param o a data-bound object
     * @return constructor and/or setter parameters
     * @throws UnsupportedOperationException if the class does not follow the expected structure
     */
    public Map<String,Object> uninstantiate(T o) throws UnsupportedOperationException {
        Map<String, Object> r = new TreeMap<String, Object>();
        Map<String, Object> constructorOnlyDataBoundProps = new TreeMap<String, Object>();
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
        }

        Object control;
        try {
            control = instantiate(constructorOnlyDataBoundProps);
        } catch (Exception x) {
            LOGGER.log(Level.WARNING, "Cannot create control version of " + type + " using " + constructorOnlyDataBoundProps, x);
            return r;
        }

        for (DescribableParameter p : parameters.values()) {
            if (p.isRequired())
                continue;

            Object v = p.inspect(control);

            // if the control has the same value as our object, we won't need to keep it
            if (ObjectUtils.equals(v, r.get(p.getName()))) {
                r.remove(p.getName());
            }
        }

        return r;
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

    @Override public String toString() {
        return "("+StringUtils.join(getParameters(), ", ") + ")";
    }

    public static final String CLAZZ = "$class";

    private static final Logger LOGGER = Logger.getLogger(DescribableModel.class.getName());
}
