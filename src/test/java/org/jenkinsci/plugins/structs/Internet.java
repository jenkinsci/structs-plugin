package org.jenkinsci.plugins.structs;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * "net" symbols are on two descriptors.
 *
 * @author Kohsuke Kawaguchi
 */
public class Internet extends AbstractDescribableImpl<Internet> implements Tech {
    @DataBoundConstructor
    public Internet() {
    }

    @Extension @Symbol("net")
    public static class DescriptorImpl extends Descriptor<Internet> {
        @Override
        public String getDisplayName() {
            return "internet";
        }
    }
}
