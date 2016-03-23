package org.jenkinsci.plugins.structs.describable;

/**
 *
 * @author Jesse Glick
 * @author Anderw Bayer
 */
public final class HomogeneousObjectType extends ParameterType {
    private final DescribableModel type;

    HomogeneousObjectType(Class<?> actualClass, DescribableModel type) {
        super(actualClass);
        this.type = type;
    }

    public Class<?> getType() {
        return (Class) getActualType();
    }

    /**
     * The schema representing a type of nested object.
     */
    public DescribableModel getSchemaType() {
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
