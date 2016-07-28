package org.jenkinsci.plugins.structs;

import hudson.model.Descriptor;
import org.jenkinsci.Symbol;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;

import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Kohsuke Kawaguchi
 */
public class SymbolLookupTest {
    @TestExtension @Symbol("foo")
    public static class Foo {}

    @TestExtension @Symbol("bar")
    public static class Bar {}

    @Rule
    public JenkinsRule rule = new JenkinsRule();

    @Inject
    SymbolLookup lookup;

    @Inject
    Foo foo;

    @Inject
    Bar bar;

    @Inject
    FishingNet.DescriptorImpl fishingNetDescriptor;

    @Inject
    Internet.DescriptorImpl internetDescriptor;

    @Before
    public void setUp() {
        rule.jenkins.getInjector().injectMembers(this);
    }

    @Test
    public void test() {
        assertNull(lookup.find(Object.class, "zoo"));
        assertThat((Foo) lookup.find(Object.class, "foo"), is(sameInstance(this.foo)));
        assertThat((Bar) lookup.find(Object.class, "bar"), is(sameInstance(this.bar)));

        // even if the symbol matches, if the type isn't valid the return value will be null
        assertNull(lookup.find(String.class, "foo"));
    }

    @Test
    public void descriptorLookup() {
        assertThat(lookup.findDescriptor(Fishing.class, "net"), is(sameInstance((Descriptor)fishingNetDescriptor)));
        assertThat(lookup.findDescriptor(Tech.class, "net"),    is(sameInstance((Descriptor)internetDescriptor)));
    }
}
