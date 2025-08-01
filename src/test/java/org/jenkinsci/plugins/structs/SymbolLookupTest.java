package org.jenkinsci.plugins.structs;

import hudson.model.AbstractDescribableImpl;
import hudson.model.BooleanParameterValue;
import hudson.model.Descriptor;
import java.util.Collections;
import java.util.Set;
import jakarta.inject.Inject;
import jenkins.model.GlobalConfiguration;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import org.jenkinsci.Symbol;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class SymbolLookupTest {
    @TestExtension @Symbol("foo")
    public static class Foo {}

    @TestExtension @Symbol("bar")
    public static class Bar {}

    private JenkinsRule rule;

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

    @BeforeEach
    void setUp(JenkinsRule r) {
        rule = r;
        rule.jenkins.getInjector().injectMembers(this);
    }

    @Test
    void test() {
        assertNull(lookup.find(Object.class, "zoo"));
        assertThat((Foo) lookup.find(Object.class, "foo"), is(sameInstance(this.foo)));
        assertThat((Bar) lookup.find(Object.class, "bar"), is(sameInstance(this.bar)));

        // even if the symbol matches, if the type isn't valid the return value will be null
        assertNull(lookup.find(String.class, "foo"));
    }

    @Test
    void descriptorLookup() {
        assertThat(lookup.findDescriptor(Fishing.class, "net"), is(sameInstance((Descriptor)fishingNetDescriptor)));
        assertThat(lookup.findDescriptor(Tech.class, "net"),    is(sameInstance((Descriptor)internetDescriptor)));
    }

    @Test
    void symbolValueFromObject() {
        Set<String> netSet = Collections.singleton("net");
        Set<String> fooSet = Collections.singleton("foo");

        assertEquals(Collections.emptySet(), SymbolLookup.getSymbolValue("some-string"));

        FishingNet fishingNet = new FishingNet();
        assertEquals(netSet, SymbolLookup.getSymbolValue(fishingNet));

        assertEquals(netSet, SymbolLookup.getSymbolValue(fishingNetDescriptor));
        assertEquals(fooSet, SymbolLookup.getSymbolValue(foo));
    }

    @Test
    void symbolValueFromClass() {
        Set<String> netSet = Collections.singleton("net");
        Set<String> fooSet = Collections.singleton("foo");

        assertEquals(Collections.emptySet(), SymbolLookup.getSymbolValue(String.class));
        assertEquals(netSet, SymbolLookup.getSymbolValue(FishingNet.class));
        assertEquals(netSet, SymbolLookup.getSymbolValue(FishingNet.DescriptorImpl.class));
        assertEquals(fooSet, SymbolLookup.getSymbolValue(Foo.class));
    }

    @Issue("JENKINS-26093")
    @Test
    void parameters() {
        assertEquals(Collections.singleton("booleanParam"), SymbolLookup.getSymbolValue(BooleanParameterValue.class));
        assertEquals(Collections.singleton("booleanParam"), SymbolLookup.getSymbolValue(new BooleanParameterValue("flag", true)));
    }

    @Issue("JENKINS-37820")
    @Test
    void descriptorIsDescribable() {
        assertEquals(Collections.singleton("whatever"), SymbolLookup.getSymbolValue(SomeConfiguration.class));
        assertEquals(Collections.singleton("whatever"), SymbolLookup.getSymbolValue(rule.jenkins.getDescriptorByType(SomeConfiguration.class)));
    }
    @TestExtension("descriptorIsDescribable")
    @Symbol("whatever")
    public static class SomeConfiguration extends GlobalConfiguration {}

    @Issue("JENKINS-57218")
    @Test
    void descriptorSansExtension() {
        SymbolLookup sl = rule.jenkins.getExtensionList(SymbolLookup.class).get(0);
        assertAll(
                () -> assertThat("A is registered", sl.findDescriptor(Stuff.class, "a"), is(instanceOf(StuffA.DescriptorImpl.class))),
                () -> assertThat("B is not", sl.findDescriptor(Stuff.class, "b"), nullValue()),
                () -> assertThat("C is, but the registration is broken", sl.findDescriptor(Stuff.class, "c"), nullValue()),
                () -> assertThat("A (cached)", sl.findDescriptor(Stuff.class, "a"), is(instanceOf(StuffA.DescriptorImpl.class))),
                () -> assertThat("B (cached)", sl.findDescriptor(Stuff.class, "b"), nullValue()),
                () -> assertThat("C (cached)", sl.findDescriptor(Stuff.class, "c"), nullValue())
        );
    }

    public abstract static class Stuff extends AbstractDescribableImpl<Stuff> {}
    public static final class StuffA extends Stuff {
        @Symbol("a")
        @TestExtension("descriptorSansExtension") public static final class DescriptorImpl extends Descriptor<Stuff> {}
    }
    public static final class StuffB extends Stuff {
        @Symbol("b")
        /* no extension */ public static final class DescriptorImpl extends Descriptor<Stuff> {}
    }
    public static final class StuffC extends Stuff {
        @Symbol("c")
        @TestExtension("descriptorSansExtension") public static final class DescriptorImpl extends Descriptor<Stuff> {
            public DescriptorImpl() {
                throw new Error("oops");
            }
        }
    }

}
