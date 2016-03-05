package org.jenkinsci.plugins.symbol.describable;

import java.util.Map;

/**
 * A parameter (or array element) which could take any of the indicated concrete object types.
 *
 * @author Jesse Glick
 * @author Anderw Bayer
 */
public final class HeterogeneousObjectType extends ParameterType {
    private final Map<String,Schema> types;
    HeterogeneousObjectType(Class<?> supertype, Map<String, Schema> types) {
        super(supertype);
        this.types = types;
    }

    public Class<?> getType() {
        return (Class) getActualType();
    }

    /**
     * A map from names which could be passed to {@link #CLAZZ} to types of allowable nested objects.
     */
    public Map<String,Schema> getTypes() {
        return types;
    }
    @Override public String toString() {
        return getType().getSimpleName() + types;
    }
}
