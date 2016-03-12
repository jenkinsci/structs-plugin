package org.jenkinsci.plugins.structs.describable;

import com.google.common.primitives.Primitives;

/**
 * @author Jesse Glick
 * @author Anderw Bayer
 */
public final class AtomicType extends ParameterType {
    AtomicType(Class<?> clazz) {
        super(clazz);
    }

    public Class<?> getType() {
        return (Class) getActualType();
    }

    @Override
    public String toString() {
        return Primitives.unwrap((Class) getActualType()).getSimpleName();
    }
}
