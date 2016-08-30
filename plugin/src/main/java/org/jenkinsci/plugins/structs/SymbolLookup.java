package org.jenkinsci.plugins.structs;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.Extension;
import hudson.PluginManager;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.codehaus.groovy.tools.Utilities;
import org.jenkinsci.Symbol;
import org.jvnet.hudson.annotation_indexer.Index;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Finds symbol by its name.
 *
 * @author Kohsuke Kawaguchi
 * @see Symbol#value()
 */
@Extension
public class SymbolLookup {
    private final ConcurrentMap<Key,Object> cache = new ConcurrentHashMap<Key, Object>();

    @Inject
    PluginManager pluginManager;

    @Inject
    Jenkins jenkins;

    /**
     * @param type
     *      Restrict the search to a subset of extensions.
     */
    public <T> T find(Class<T> type, String symbol) {
        try {
            Key k = new Key("find",type,symbol);
            Object i = cache.get(k);
            if (i!=null)    return type.cast(i);

            // not allowing @Symbol to use an invalid identifier.
            // TODO: compile time check
            if (!Utilities.isJavaIdentifier(symbol))
                return null;

            for (Class<?> e : Index.list(Symbol.class, pluginManager.uberClassLoader, Class.class)) {
                if (type.isAssignableFrom(e)) {
                    Symbol s = e.getAnnotation(Symbol.class);
                    if (s != null) {
                        for (String t : s.value()) {
                            if (t.equals(symbol)) {
                                i = jenkins.getInjector().getInstance(e);
                                cache.put(k, i);
                                return type.cast(i);
                            }
                        }
                    }
                }
            }

            // not caching negative result since new plugins might be added later
            return null;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Unable to find @Symbol",e);
            return null;
        }
    }

    /**
     * Looks for a {@link Descriptor} that has the given symbol
     *
     * @param type
     *      Restrict the search to a subset of {@link Describable}
     */
    public <T> Descriptor<? extends T> findDescriptor(Class<T> type, String symbol) {
        try {
            Key k = new Key("findDescriptor",type,symbol);
            Object i = cache.get(k);
            if (i!=null)    return (Descriptor)i;

            // not allowing @Symbol to use an invalid identifier.
            // TODO: compile time check
            if (!Utilities.isJavaIdentifier(symbol))
                return null;

            for (Class<?> e : Index.list(Symbol.class, pluginManager.uberClassLoader, Class.class)) {
                if (Descriptor.class.isAssignableFrom(e)) {
                    Symbol s = e.getAnnotation(Symbol.class);
                    if (s != null) {
                        for (String t : s.value()) {
                            if (t.equals(symbol)) {
                                Descriptor d = (Descriptor) jenkins.getInjector().getInstance(e);
                                if (type.isAssignableFrom(d.clazz)) {
                                    cache.put(k, d);
                                    return d;
                                }
                            }
                        }
                    }
                }
            }

            // not caching negative result since new plugins might be added later
            return null;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Unable to find @Symbol",e);
            return null;
        }
    }

    private static class Key {
        private final String tag;
        private final Class type;
        private final String name;

        public Key(String tag, Class type, String name) {
            this.tag = tag;
            this.type = type;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return type==key.type && tag.equals(key.tag) && name.equals(key.name);

        }

        @Override
        public int hashCode() {
            int h = type.hashCode();
            h = h*31 + tag.hashCode();
            h = h*31 + name.hashCode();
            return h;
        }
    }

    /**
     * Gets the singleton instance.
     */
    public static SymbolLookup get() {
        Jenkins j = Jenkins.getInstance();
        if (j == null) {
            throw new IllegalStateException();
        }
        return j.getInjector().getInstance(SymbolLookup.class);
    }

    /**
     * Get the {@link Symbol} value(s) for the class of the given object, generally a {@link Descriptor}, if the annotation
     * is present. If the object is in fact a {@link Describable}, we'll use its {@link Descriptor} class instead.
     *
     * @param o An object
     * @return The {@link Symbol} annotation value(s) for the class (generally a {@link Descriptor} that object represents,
     * or an empty {@link Set} if the annotation is not present.
     */
    @Nonnull public static Set<String> getSymbolValue(@Nonnull Object o) {
        if (o instanceof Describable) {
            return getSymbolValue(((Describable) o).getDescriptor().getClass());
        } else {
            return getSymbolValue(o.getClass());
        }
    }

    /**
     * Get the {@link Symbol} value(s) for the given class, if the annotation is present. Unlike {@link #getSymbolValue(Object)},
     * this will not get the {@link Descriptor} for {@link Describable} classes.
     *
     * @param c A class.
     * @return The {@link Symbol} annotation value(s) for the given class, or an empty {@link Set} if the annotation is not present.
     */
    @SuppressFBWarnings(value = "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
            justification = "Jenkins.getInstance() can return null in theory.")
    @Nonnull public static Set<String> getSymbolValue(@Nonnull Class<?> c) {
        Set<String> symbolValues = new LinkedHashSet<String>();
        if (Describable.class.isAssignableFrom(c) && !Descriptor.class.isAssignableFrom(c) && Jenkins.getInstance() != null) {
            Descriptor d = Jenkins.getInstance().getDescriptor(c.asSubclass(Describable.class));
            symbolValues.addAll(getSymbolValue(d));
        } else {
            Symbol s = c.getAnnotation(Symbol.class);
            if (s != null) {
                Collections.addAll(symbolValues, s.value());
            }
        }
        return symbolValues;
    }

    private static final Logger LOGGER = Logger.getLogger(SymbolLookup.class.getName());
}
