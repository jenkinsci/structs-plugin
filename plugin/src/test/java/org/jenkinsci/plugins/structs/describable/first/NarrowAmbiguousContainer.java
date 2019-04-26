/*
 * The MIT License
 *
 * Copyright (c) 2018, CloudBees, Inc.
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

package org.jenkinsci.plugins.structs.describable.first;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Describable;
import hudson.model.Descriptor;
import org.jenkinsci.plugins.structs.describable.AbstractSharedName;
import org.jenkinsci.plugins.structs.describable.AbstractThirdSharedName;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class NarrowAmbiguousContainer extends AbstractDescribableImpl<NarrowAmbiguousContainer> {
    public final Describable<?> ambiguous;
    public final Describable<?> unambiguous;

    @DataBoundConstructor
    public NarrowAmbiguousContainer(Describable<?> ambiguous, Describable<?> unambiguous) {
        this.ambiguous = ambiguous;
        this.unambiguous = unambiguous;
    }

    @Override
    public String toString() {
        return "NarrowAmbiguousContainer[ambiguous[" + ambiguous.toString() + "], unambiguous[" + unambiguous.toString() + "]]";
    }
    @Extension
    public static class DescriptorImpl extends Descriptor<NarrowAmbiguousContainer> {
        @Override
        public String getDisplayName() {
            return "narrow ambiguous container";
        }
    }

    public static class ThirdSharedName extends AbstractThirdSharedName {
        private final String one;
        private String two;

        @DataBoundConstructor
        public ThirdSharedName(String one) {
            this.one = one;
        }

        public String getOne() {
            return one;
        }

        public String getTwo() {
            return two;
        }

        @DataBoundSetter
        public void setTwo(String two) {
            this.two = two;
        }

        public String getLegacyTwo() {
            return two;
        }

        @Deprecated
        @DataBoundSetter
        public void setLegacyTwo(String two) {
            this.two = two;
        }

        @Override
        public String toString() {
            return "ThirdSharedName[one[" + one + "], [two[" + two + "]]";
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<AbstractThirdSharedName> {
            @Override
            public String getDisplayName() {
                return "inner.ThirdSharedName";
            }
        }
    }
}
