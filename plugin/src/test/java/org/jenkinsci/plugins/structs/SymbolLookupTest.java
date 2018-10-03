package org.jenkinsci.plugins.structs;

import hudson.model.BooleanParameterValue;
import hudson.model.Descriptor;
import java.util.Collections;
import java.util.Set;
import javax.inject.Inject;
import jenkins.model.GlobalConfiguration;
import static org.hamcrest.CoreMatchers.*;
import org.jenkinsci.Symbol;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;

public class SymbolLookupTest {
    @TestExtension @Symbol("foo")
    public static class Foo {}

    @TestExtension @Symbol("bar")
    public static class Bar {}

    @TestExtension @Symbol("ambiguous")
    public static class FirstAmbiguous {}

    @TestExtension @Symbol(value = "ambiguous", context = Bar.class)
    public static class SecondAmbiguous {}

    @Rule
    public JenkinsRule rule = new JenkinsRule();

    @Inject
    SymbolLookup lookup;

    @Inject
    Foo foo;

    @Inject
    Bar bar;

    @Inject
    FirstAmbiguous firstAmbiguous;

    @Inject
    SecondAmbiguous secondAmbiguous;

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

    @Test
    public void symbolValueFromObject() {
        Set<String> netSet = Collections.singleton("net");
        Set<String> fooSet = Collections.singleton("foo");

        assertEquals(Collections.emptySet(), SymbolLookup.getSymbolValue("some-string"));

        FishingNet fishingNet = new FishingNet();
        assertEquals(netSet, SymbolLookup.getSymbolValue(fishingNet));

        assertEquals(netSet, SymbolLookup.getSymbolValue(fishingNetDescriptor));
        assertEquals(fooSet, SymbolLookup.getSymbolValue(foo));
    }
    
    @Test
    public void symbolValueFromClass() {
        Set<String> netSet = Collections.singleton("net");
        Set<String> fooSet = Collections.singleton("foo");

        assertEquals(Collections.emptySet(), SymbolLookup.getSymbolValue(String.class));
        assertEquals(netSet, SymbolLookup.getSymbolValue(FishingNet.class));
        assertEquals(netSet, SymbolLookup.getSymbolValue(FishingNet.DescriptorImpl.class));
        assertEquals(fooSet, SymbolLookup.getSymbolValue(Foo.class));
    }

    @Issue("JENKINS-26093")
    @Test
    public void parameters() {
        assertEquals(Collections.singleton("booleanParam"), SymbolLookup.getSymbolValue(BooleanParameterValue.class));
        assertEquals(Collections.singleton("booleanParam"), SymbolLookup.getSymbolValue(new BooleanParameterValue("flag", true)));
    }

    @Issue("JENKINS-37820")
    @Test
    public void descriptorIsDescribable() {
        assertEquals(Collections.singleton("whatever"), SymbolLookup.getSymbolValue(SomeConfiguration.class));
        assertEquals(Collections.singleton("whatever"), SymbolLookup.getSymbolValue(rule.jenkins.getDescriptorByType(SomeConfiguration.class)));
    }
    @TestExtension("descriptorIsDescribable")
    @Symbol("whatever")
    public static class SomeConfiguration extends GlobalConfiguration {}

    @Issue("JENKINS-53825")
    @Test
    public void context() {
        assertThat(lookup.find(Object.class, "ambiguous"), is(sameInstance(this.firstAmbiguous)));
        assertThat(lookup.find(Object.class, "ambiguous", Bar.class), is(sameInstance(this.secondAmbiguous)));
    }

    @TestExtension("descriptorContext")
    @Symbol("ambiguousDescriptor")
    public static class FirstAmbiguousConfiguration extends GlobalConfiguration {}

    @TestExtension("descriptorContext")
    @Symbol(value = "ambiguousDescriptor", context = Bar.class)
    public static class SecondAmbiguousConfiguration extends GlobalConfiguration {}

    @Issue("JENKINS-53825")
    @Test
    public void descriptorContext() {
        FirstAmbiguousConfiguration first = rule.jenkins.getDescriptorByType(FirstAmbiguousConfiguration.class);
        SecondAmbiguousConfiguration second = rule.jenkins.getDescriptorByType(SecondAmbiguousConfiguration.class);

        assertThat(lookup.find(Object.class, "ambiguousDescriptor"), is(sameInstance(first)));
        assertThat(lookup.find(Object.class, "ambiguousDescriptor", Bar.class), is(sameInstance(second)));
    }

}
