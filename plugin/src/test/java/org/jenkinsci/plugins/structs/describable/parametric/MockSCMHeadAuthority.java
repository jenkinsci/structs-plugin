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
import hudson.model.AbstractDescribableImpl;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.mixin.SCMHeadMixin;
import jenkins.scm.api.trait.SCMSourceRequest;

import java.io.IOException;

public abstract class MockSCMHeadAuthority<S extends SCMSourceRequest, H extends SCMHeadMixin,
        R extends SCMRevision> extends AbstractDescribableImpl<MockSCMHeadAuthority<?, ?, ?>> {
    public MockSCMHeadAuthority() {
    }

    protected abstract boolean checkTrusted(@NonNull S var1, @NonNull H var2) throws IOException, InterruptedException;

    public MockSCMHeadAuthorityDescriptor getDescriptor() {
        return (MockSCMHeadAuthorityDescriptor)super.getDescriptor();
    }
}