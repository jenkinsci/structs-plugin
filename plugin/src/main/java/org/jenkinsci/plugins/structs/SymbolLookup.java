package org.jenkinsci.plugins.structs;

import hudson.Extension;
import hudson.PluginManager;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.codehaus.groovy.tools.Utilities;
import org.jenkinsci.Symbol;
import org.jvnet.hudson.annotation_indexer.Index;

import javax.inject.Inject;
import java.io.IOException;
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

    private static final Logger LOGGER = Logger.getLogger(SymbolLookup.class.getName());
}
