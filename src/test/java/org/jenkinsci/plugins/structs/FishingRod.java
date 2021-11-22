package org.jenkinsci.plugins.structs;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * "rod" symbol
 *
 * @author Kohsuke Kawaguchi
 */
public class FishingRod extends AbstractDescribableImpl<FishingRod> implements Fishing {
    @DataBoundConstructor
    public FishingRod() {
    }


    @Extension @Symbol("rod")
    public static class DescriptorImpl extends Descriptor<FishingRod> {
        @Override
        public String getDisplayName() {
            return "fishing rod";
        }
    }
}
