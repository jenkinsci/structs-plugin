package org.jenkinsci.plugins.symbol.describable;

/**
 *
 * @author Jesse Glick
 * @author Anderw Bayer
 */
public final class HomogeneousObjectType extends ParameterType {
    private final DescribableModel type;

    HomogeneousObjectType(Class<?> actualClass) {
        super(actualClass);
        this.type = new DescribableModel(actualClass);
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
