/*
 * The MIT License
 *
 * Copyright 2019 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.structs.describable;

import hudson.model.Describable;
import hudson.model.Descriptor;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jenkins.model.Jenkins;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.Beta;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * Allows the usage of {@link DescribableModel} to be fine-tuned to cover special cases such as backwards compatibility.
 * Normally introspection of a struct class with {@link DataBoundConstructor} and {@link DataBoundSetter} suffices for databinding.
 * This facility allows the definer of the struct to accept variant inputs to {@link DescribableModel#instantiate(Map)},
 * or recommend variant outputs for {@link DescribableModel#uninstantiate2(Object)}.
 * This is somewhat analogous to implementing a {@code readResolve} method to customize XStream serialization behavior.
 * Implement this interface on a {@link Descriptor}.
 * TODO document that these are syntactic-level transformations
 * TODO document that arguments are immutable
 */
@Restricted(Beta.class)
public interface CustomDescribableModel /* extends Descriptor */ {

    /**
     * Permits customization of the behavior of {@link DescribableModel#instantiate(Map)}.
     */
    default @Nonnull Map<String, Object> customInstantiate(@Nonnull Map<String, Object> arguments) {
        return arguments;
    }

    /**
     * Permits customization of the behavior of {@link DescribableModel#uninstantiate2(Object)}.
     */
    default @Nonnull UninstantiatedDescribable customUninstantiate(@Nonnull UninstantiatedDescribable ud) {
        return ud;
    }

    static @CheckForNull CustomDescribableModel of(Class<?> type) {
        if (Describable.class.isAssignableFrom(type)) {
            Jenkins j = Jenkins.getInstanceOrNull();
            if (j != null) {
                Descriptor<?> d = j.getDescriptor(type.asSubclass(Describable.class));
                if (d instanceof CustomDescribableModel) {
                    return (CustomDescribableModel) d;
                }
            }
        }
        return null;
    }

}
