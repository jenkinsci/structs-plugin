package org.jenkinsci.plugins.symbol;

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
    private final ConcurrentMap<String,Object> cache = new ConcurrentHashMap<String, Object>();

    @Inject
    PluginManager pluginManager;

    @Inject
    Jenkins jenkins;

    public Object find(String symbol) {
        try {
            Object i = cache.get(symbol);
            if (i!=null)    return i;

            // not allowing @Symbol to use an invalid identifier.
            // TODO: compile time check
            if (!Utilities.isJavaIdentifier(symbol))
                return null;

            for (Class<?> e : Index.list(Symbol.class, pluginManager.uberClassLoader, Class.class)) {
                Symbol s = e.getAnnotation(Symbol.class);
                if (s!=null) {
                    for (String t : s.value()) {
                        if (t.equals(symbol)) {
                            i = jenkins.getInjector().getInstance(e);
                            cache.put(symbol, i);
                            return i;
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

    private static final Logger LOGGER = Logger.getLogger(SymbolLookup.class.getName());
}
