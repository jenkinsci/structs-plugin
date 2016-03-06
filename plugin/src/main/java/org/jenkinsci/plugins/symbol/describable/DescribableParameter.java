package org.jenkinsci.plugins.symbol.describable;

import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.lang.reflect.Type;

/**
 * A property of {@link DescribableModel}
 *
 * @author Kohsuke Kawaguchi
 */
public final class DescribableParameter {
    private final DescribableModel parent;
    private final ParameterType type;
    private final String name;

    /**
     * If this property is optional, the {@link Setter} that abstracts away how to set
     * the value to this property. Otherwise this parameter must be injected via the constructor.
     */
    /*package*/ final Setter setter;

    /*package*/ DescribableParameter(DescribableModel parent, Type type, String name, Setter setter) {
        this.parent = parent;
        this.type = ParameterType.of(type);
        this.name = name;
        this.setter = setter;
    }

    public ParameterType getType() {
        return type;
    }

    public Type getActualType() {
        return getType().getActualType();
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
        return setter==null;
    }

    /**
     * Loads help defined for this parameter.
     *
     * @return some HTML (in English locale), if available, else null
     * @see Descriptor#doHelp
     */
    public @CheckForNull
    String getHelp() throws IOException {
        return parent.getHelp("help-" + name + ".html");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder().append(name);
        if (!isRequired())   sb.append('?');
        return sb.append(": ").append(type).toString();
    }
}
