package org.jenkinsci.plugins.structs;

import hudson.Extension;
import hudson.PluginManager;
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
            Key k = new Key(type,symbol);
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

    private static class Key {
        private final Class type;
        private final String name;

        public Key(Class type, String name) {
            this.type = type;
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return type==key.type && name.equals(key.name);

        }

        @Override
        public int hashCode() {
            return 31 * type.hashCode() + name.hashCode();
        }
    }

    /**
     * Gets the singleton instance.
     */
    public static SymbolLookup get() {
        return Jenkins.getInstance().getInjector().getInstance(SymbolLookup.class);
    }

    private static final Logger LOGGER = Logger.getLogger(SymbolLookup.class.getName());
}
