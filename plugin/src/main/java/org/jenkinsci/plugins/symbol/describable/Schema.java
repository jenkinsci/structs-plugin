package org.jenkinsci.plugins.symbol.describable;

import hudson.Util;
import hudson.model.Describable;
import hudson.model.Descriptor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Definition of how a particular class may be configured.
 *
 * @author Jesse Glick
 * @author Anderw Bayer
 */
public final class Schema<T> {

    private final Class<T> type;
    private final Map<String,Parameter> parameters = new TreeMap<String, Parameter>();
    private final Map<String,Parameter> parametersView = Collections.unmodifiableMap(parameters);

    /**
     * Loads a definition of the structure of a class: what kind of data you might get back from {@link #uninstantiate} on an instance,
     * or might want to pass to {@link #instantiate}.
     */
    public Schema(Class<T> clazz) {
        this.type = clazz;
        String[] names = DescribableHelper.loadConstructorParamNames(clazz);
        Type[] types = DescribableHelper.findConstructor(clazz, names.length).getGenericParameterTypes();
        for (int i = 0; i < names.length; i++) {
            addParameter(new Parameter(types[i], names[i], true));
        }
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.isAnnotationPresent(DataBoundSetter.class)) {
                    f.setAccessible(true);
                    addParameter(new Parameter(f.getGenericType(), f.getName(), false));
                }
            }
            for (Method m : c.getDeclaredMethods()) {
                if (m.isAnnotationPresent(DataBoundSetter.class)) {
                    Type[] parameterTypes = m.getGenericParameterTypes();
                    if (!m.getName().startsWith("set") || parameterTypes.length != 1) {
                        throw new IllegalStateException(m + " cannot be a @DataBoundSetter");
                    }
                    m.setAccessible(true);
                    addParameter(new Parameter(
                            m.getGenericParameterTypes()[0],
                            Introspector.decapitalize(m.getName().substring(3)), false));
                }
            }
        }
    }

    private void addParameter(Parameter p) {
        parameters.put(p.getName(),p);
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
     */
    public Collection<Parameter> parameters() {
        return parametersView.values();
    }

    public Parameter getParameter(String name) {
        return parameters.get(name);
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
        return super.toString()+"["+StringUtils.join(parameters(), ", ") + "]";
    }

}
