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
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
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
 * @see CustomDescription
 */
@Restricted(Beta.class)
public interface CustomDescribableModel<T> {

    /**
     * The class to which {@link CustomDescription} is applied.
     */
    Class<T> getType();

    /**
     * Permits customization of the behavior of {@link DescribableModel#instantiate(Map)}.
     * @param standard offers the stock behavior for this object or some substructure
     */
    default T instantiate(Map<String, Object> arguments, StandardInstantiator standard) throws Exception {
        return standard.instantiate(getType(), arguments);
    }

    /**
     * Permits customization of the behavior of {@link DescribableModel#uninstantiate2(Object)}.
     * @param standard offers the stock behavior for this object or some substructure
     */
    default UninstantiatedDescribable uninstantiate(T object, StandardUninstantiator standard) throws UnsupportedOperationException {
        return standard.uninstantiate(object);
    }

    /**
     * Marker for a struct class that should have a special model.
     * Place on a {@link Describable}, <em>not</em> its {@link Descriptor}.
     * TODO better to make an optional interface of Descriptor
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Documented
    @interface CustomDescription {
        Class<? extends CustomDescribableModel<?>> value();
    }

    // TODO the StandardInstantiator/StandardUninstantiator pattern may be overkill; perhaps better to just let the customization process a Map

    /**
     * Permits delegation to the stock behavior of {@link DescribableModel#instantiate(Map)}.
     */
    interface StandardInstantiator {
        <T> T instantiate(Class<T> type, Map<String, Object> arguments) throws Exception;
    }

    /**
     * Permits delegation to the stock behavior of {@link DescribableModel#uninstantiate2(Object)}.
     */
    interface StandardUninstantiator {
        UninstantiatedDescribable uninstantiate(Object object) throws UnsupportedOperationException;
    }

}
