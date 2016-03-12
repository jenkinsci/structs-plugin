package org.jenkinsci.plugins.structs.describable;

import java.util.Arrays;

/**
 * @author Jesse Glick
 * @author Anderw Bayer
 */
public final class EnumType extends ParameterType {
    private final String[] values;

    EnumType(Class<?> clazz, String[] values) {
        super(clazz);
        this.values = values;
    }

    public Class<?> getType() {
        return (Class) getActualType();
    }

    /**
     * A list of enumeration values.
     */
    public String[] getValues() {
        return values.clone();
    }

    @Override
    public String toString() {
        return ((Class) getActualType()).getSimpleName() + Arrays.toString(values);
    }
}
