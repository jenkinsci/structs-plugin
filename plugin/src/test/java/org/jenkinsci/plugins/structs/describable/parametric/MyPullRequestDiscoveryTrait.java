/*
 * The MIT License
 *
 * Copyright (c) 2021, Jenkins contributors
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

package org.jenkinsci.plugins.structs.describable.parametric;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.TaskListener;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.mixin.ChangeRequestSCMHead2;
import jenkins.scm.api.trait.SCMSourceContext;
import jenkins.scm.api.trait.SCMSourceRequest;
import org.kohsuke.stapler.DataBoundConstructor;

public class MyPullRequestDiscoveryTrait {
    private final int strategyId;
    private final MockSCMHeadAuthority<? super MySCMSourceRequest,? extends ChangeRequestSCMHead2,? extends SCMRevision> trust;

    @DataBoundConstructor
    public MyPullRequestDiscoveryTrait(int strategyId,
                                         @NonNull MockSCMHeadAuthority<? super MySCMSourceRequest, ? extends ChangeRequestSCMHead2, ? extends SCMRevision> trust) {
        this.strategyId = strategyId;
        this.trust = trust;
    }

    public int getStrategyId() {
        return strategyId;
    }

    public MockSCMHeadAuthority<? super MySCMSourceRequest,? extends ChangeRequestSCMHead2,? extends SCMRevision> getTrust() {
        return trust;
    }

    public static class MySCMSourceRequest extends SCMSourceRequest {
        protected MySCMSourceRequest(@NonNull SCMSource source, @NonNull SCMSourceContext<?, ?> context, TaskListener listener) {
            super(source, context, listener);
        }
    }

    public static class OtherSCMHeadAuthority extends MockSCMHeadAuthority<SCMSourceRequest, MySCMHead, SCMRevision> {
        @DataBoundConstructor
        public OtherSCMHeadAuthority() {
            // expose to stapler
        }
        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean checkTrusted(@NonNull SCMSourceRequest request, @NonNull MySCMHead head) {
            return true;
        }

        @Extension
        public static class DescriptorImpl extends MockSCMHeadAuthorityDescriptor {
            public DescriptorImpl() {
            }

            public String getDisplayName() {
                return "OtherPullRequestDiscovery";
            }

        }

    }

}
