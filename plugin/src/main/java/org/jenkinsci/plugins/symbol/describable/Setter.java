package org.jenkinsci.plugins.symbol.describable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Abstracts away how to set a value to field or via a setter method.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class Setter {
    abstract void set(Object instance, Object value) throws Exception;

    static Setter create(final Method m) {
        m.setAccessible(true);

        return new Setter() {
            @Override
            void set(Object instance, Object value) throws Exception {
                m.invoke(instance,value);
            }
        };
    }

    static Setter create(final Field f) {
        f.setAccessible(true);

        return new Setter() {
            @Override
            void set(Object instance, Object value) throws Exception {
                f.set(instance,value);
            }
        };
    }
}
