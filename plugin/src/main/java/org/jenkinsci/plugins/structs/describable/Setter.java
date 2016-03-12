package org.jenkinsci.plugins.structs.describable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Abstracts away how to set a value to field or via a setter method.
 *
 * @author Kohsuke Kawaguchi
 */
abstract class Setter {
    /**
     * Sets the given value to the method/field that this {@link Setter} encapsulates.
     */
    abstract void set(Object instance, Object value) throws Exception;

    /**
     * Human readable display name use to report an error
     */
    abstract String getDisplayName();

    static Setter create(final Method m) {
        m.setAccessible(true);

        return new Setter() {
            @Override
            void set(Object instance, Object value) throws Exception {
                m.invoke(instance,value);
            }

            @Override
            String getDisplayName() {
                return m.getDeclaringClass()+"."+m.getName()+"()";
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

            @Override
            String getDisplayName() {
                return f.getDeclaringClass()+"."+f.getName();
            }
        };
    }
}
