package org.jenkinsci.plugins.symbol.describable;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.lang.reflect.Type;

/**
 * A property of {@link Schema}
 *
 * @author Kohsuke Kawaguchi
 */
public final class Parameter {
    private final ParameterType type;
    private final String name;
    private final boolean required;

    /*package*/ Parameter(Type type, String name, boolean required) {
        this.required = required;
        this.type = ParameterType.of(type);
        this.name = name;
    }

    public ParameterType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    /**
     * True if this parameter is required.
     *
     * <p>
     * A parameter set via {@link DataBoundSetter} is considered optional.
     * Right now, all the parameters set via {@link DataBoundConstructor} is
     * considered mandatory, but this might change in the future.
     */
    public boolean isRequired() {
        return required;
    }
}
