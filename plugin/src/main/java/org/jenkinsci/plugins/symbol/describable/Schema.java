package org.jenkinsci.plugins.symbol.describable;

import hudson.Util;
import hudson.model.Describable;
import hudson.model.Descriptor;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.symbol.describable.DescribableHelper.ParameterType;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.lang.Klass;

import javax.annotation.CheckForNull;
import java.beans.Introspector;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Definition of how a particular class may be configured.
 */
public final class Schema {

    /**
     * Loads a definition of the structure of a class: what kind of data you might get back from {@link #uninstantiate} on an instance,
     * or might want to pass to {@link #instantiate}.
     */
    public static Schema schemaFor(Class<?> clazz) {
        return new Schema(clazz);
    }

    private final Class<?> type;
    private final Map<String,ParameterType> parameters;
    private final List<String> mandatoryParameters;

    Schema(Class<?> clazz) {
        this.type = clazz;
        mandatoryParameters = new ArrayList<String>();
        parameters = new TreeMap<String,ParameterType>();
        String[] names = DescribableHelper.loadConstructorParamNames(clazz);
        Type[] types = DescribableHelper.findConstructor(clazz, names.length).getGenericParameterTypes();
        for (int i = 0; i < names.length; i++) {
            mandatoryParameters.add(names[i]);
            parameters.put(names[i], ParameterType.of(types[i]));
        }
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.isAnnotationPresent(DataBoundSetter.class)) {
                    f.setAccessible(true);
                    parameters.put(f.getName(), ParameterType.of(f.getGenericType()));
                }
            }
            for (Method m : c.getDeclaredMethods()) {
                if (m.isAnnotationPresent(DataBoundSetter.class)) {
                    Type[] parameterTypes = m.getGenericParameterTypes();
                    if (!m.getName().startsWith("set") || parameterTypes.length != 1) {
                        throw new IllegalStateException(m + " cannot be a @DataBoundSetter");
                    }
                    m.setAccessible(true);
                    parameters.put(Introspector.decapitalize(m.getName().substring(3)), ParameterType.of(m.getGenericParameterTypes()[0]));
                }
            }
        }
    }

    /**
     * A concrete class, usually {@link Describable}.
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * A map from parameter names to types.
     * A parameter name is either the name of an argument to a {@link DataBoundConstructor},
     * or the JavaBeans property name corresponding to a {@link DataBoundSetter}.
     */
    public Map<String,ParameterType> parameters() {
        return parameters;
    }

    /**
     * Mandatory (constructor) parameters, in order.
     * Parameters at the end of the list may be omitted, in which case they are assumed to be null or some other default value
     * (in these cases it would be better to use {@link DataBoundSetter} on the type definition).
     * Will be keys in {@link #parameters}.
     */
    public List<String> mandatoryParameters() {
        return mandatoryParameters;
    }

    /**
     * Corresponds to {@link Descriptor#getDisplayName} where available.
     */
    public String getDisplayName() {
        for (Descriptor<?> d : DescribableHelper.getDescriptorList()) {
            if (d.clazz == type) {
                return d.getDisplayName();
            }
        }
        return type.getSimpleName();
    }

    /**
     * Loads help defined for this object as a whole or one of its parameters.
     * Note that you may need to use {@link Util#replaceMacro(String, Map)}
     * to replace {@code ${rootURL}} with some other value.
     * @param parameter if specified, one of {@link #parameters}; else for the whole object
     * @return some HTML (in English locale), if available, else null
     * @see Descriptor#doHelp
     */
    public @CheckForNull
    String getHelp(@CheckForNull String parameter) throws IOException {
        for (Klass<?> c = Klass.java(type); c != null; c = c.getSuperClass()) {
            URL u = c.getResource(parameter == null ? "help.html" : "help-" + parameter + ".html");
            if (u != null) {
                return IOUtils.toString(u, "UTF-8");
            }
        }
        return null;
    }

    @Override public String toString() {
        StringBuilder b = new StringBuilder("(");
        boolean first = true;
        Map<String,ParameterType> params = new TreeMap<String,ParameterType>(parameters());
        for (String param : mandatoryParameters()) {
            if (first) {
                first = false;
            } else {
                b.append(", ");
            }
            b.append(param).append(": ").append(params.remove(param));
        }
        for (Map.Entry<String,ParameterType> entry : params.entrySet()) {
            if (first) {
                first = false;
            } else {
                b.append(", ");
            }
            b.append('[').append(entry.getKey()).append(": ").append(entry.getValue()).append(']');
        }
        return b.append(')').toString();
    }

}
