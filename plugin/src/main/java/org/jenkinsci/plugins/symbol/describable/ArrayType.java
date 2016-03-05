package org.jenkinsci.plugins.symbol.describable;

import java.lang.reflect.Type;

/**
 * @author Jesse Glick
 * @author Anderw Bayer
 */
public final class ArrayType extends ParameterType {
    private final ParameterType elementType;

    ArrayType(Class<?> actualClass) {
        this(actualClass, of(actualClass.getComponentType()));
    }

    ArrayType(Type actualClass, ParameterType elementType) {
        super(actualClass);
        this.elementType = elementType;
    }

    /**
     * The element type of the array or list.
     */
    public ParameterType getElementType() {
        return elementType;
    }

    @Override
    public String toString() {
        return elementType + "[]";
    }
}
