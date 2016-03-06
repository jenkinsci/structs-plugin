package org.jenkinsci.plugins.symbol.describable;

import hudson.Util;
import hudson.model.Descriptor;
import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.lang.Klass;

import javax.annotation.CheckForNull;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.Map;

/**
 * A property of {@link Schema}
 *
 * @author Kohsuke Kawaguchi
 */
public final class Parameter {
    private final Schema parent;
    private final ParameterType type;
    private final String name;
    private final boolean required;

    /*package*/ Parameter(Schema parent, Type type, String name, boolean required) {
        this.parent = parent;
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
}
