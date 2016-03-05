package org.jenkinsci.plugins.symbol.describable;

/**
 *
 * @author Jesse Glick
 * @author Anderw Bayer
 */
public final class HomogeneousObjectType extends ParameterType {
    private final Schema type;

    HomogeneousObjectType(Class<?> actualClass) {
        super(actualClass);
        this.type = new Schema(actualClass);
    }

    public Class<?> getType() {
        return (Class) getActualType();
    }

    /**
     * The schema representing a type of nested object.
     */
    public Schema getSchemaType() {
        return type;
    }

    /**
     * The actual class underlying the type.
     */
    @Override
    public String toString() {
        return type.getType().getSimpleName() + type;
    }
}
