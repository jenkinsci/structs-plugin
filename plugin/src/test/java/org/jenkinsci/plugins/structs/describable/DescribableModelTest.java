/*
 * The MIT License
 *
 * Copyright 2014 Jesse Glick.
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

import hudson.Extension;
import hudson.Main;
import hudson.model.AbstractDescribableImpl;
import hudson.model.BooleanParameterValue;
import hudson.model.Descriptor;
import hudson.model.ParameterValue;
import hudson.model.ParametersDefinitionProperty;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.extensions.impl.CleanBeforeCheckout;
import org.codehaus.groovy.runtime.GStringImpl;
import org.junit.BeforeClass;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jenkinsci.plugins.structs.describable.DescribableModel.*;
import static org.junit.Assert.*;

@SuppressWarnings("unchecked") // generic array construction
public class DescribableModelTest {

    @BeforeClass
    public static void isUnitTest() {
        Main.isUnitTest = true; // suppress HsErrPidList
    }

    private static final Logger logger = Logger.getLogger(DescribableModel.class.getPackage().getName());

    @BeforeClass public static void logging() {
        logger.setLevel(Level.ALL);
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        logger.addHandler(handler);
    }

    @Test
    public void instantiate() throws Exception {
        Map<String,Object> args = map("text", "hello", "flag", true, "ignored", "!");
        assertEquals("C:hello/true", instantiate(C.class, args).toString());
        args.put("value", "main");
        assertEquals("I:main/hello/true", instantiate(I.class, args).toString());
        assertEquals("C:goodbye/false", instantiate(C.class, map("text", "goodbye")).toString());
    }

    private <T> T instantiate(Class<T> type, Map<String, Object> args) throws Exception {
        return new DescribableModel<T>(type).instantiate(args);
    }

    private Map uninstantiate(Object o) throws Exception {
        return new DescribableModel(o.getClass()).uninstantiate(o);
    }

    @Test public void uninstantiate() throws Exception {
        assertEquals("{flag=true, text=stuff}", uninstantiate(new C("stuff", true)).toString());
        I i = new I("stuff");
        i.setFlag(true);
        i.text = "more";
        assertEquals("{flag=true, text=more, value=stuff}", uninstantiate(i).toString());
    }

    @Test public void mismatchedTypes() throws Exception {
        try {
            instantiate(I.class, map("value", 99));
            fail();
        } catch (ClassCastException x) {
            String message = x.getMessage();
            assertTrue(message, message.contains(I.class.getName()));
            assertTrue(message, message.contains("value"));
            assertTrue(message, message.contains("java.lang.String"));
            assertTrue(message, message.contains("java.lang.Integer"));
        }
    }

    @Test public void schemaFor() throws Exception {
        schema(C.class, "(text: String, flag: boolean, shorty?: short)");
        schema(I.class, "(value: String, flag?: boolean, text?: String)");
        DescribableModel<?> schema = new DescribableModel(Impl1.class);
        assertEquals("Implementation #1", schema.getDisplayName());
        assertEquals("<div>Overall help.</div>", schema.getHelp());
        assertEquals("<div>The text to display.</div>", schema.getParameter("text").getHelp());
        schema = new DescribableModel(C.class);
        assertEquals("C", schema.getDisplayName());
        assertNull(schema.getHelp());
        assertNull(schema.getParameter("text").getHelp());
    }

    public static final class C {
        public final String text;
        private final boolean flag;
        @DataBoundConstructor
        public C(String text, boolean flag) {
            this.text = text;
            this.flag = flag;
        }
        public boolean isFlag() {
            return flag;
        }
        @Override public String toString() {
            return "C:" + text + "/" + flag;
        }
        // Are not actually trying to inject it; just making sure that unhandled @DataBoundSetter types are ignored if unused.
        public short getShorty() {return 0;}
        @DataBoundSetter
        public void setShorty(short s) {throw new UnsupportedOperationException();}
    }

    public static final class I {
        private final String value;
        @DataBoundSetter private String text;
        private boolean flag;
        @DataBoundConstructor public I(String value) {
            this.value = value;
        }
        public String getValue() {
            return value;
        }
        public String getText() {
            return text;
        }
        public boolean isFlag() {
            return flag;
        }
        @DataBoundSetter public void setFlag(boolean f) {
            this.flag = f;
        }
        @Override public String toString() {
            return "I:" + value + "/" + text + "/" + flag;
        }
    }

    @Test public void findSubtypes() throws Exception {
        assertEquals(new HashSet<Class<?>>(Arrays.asList(Impl1.class, Impl2.class, Impl3.class)), DescribableModel.findSubtypes(Base.class));
        assertEquals(Collections.singleton(Impl1.class), DescribableModel.findSubtypes(Marker.class));
    }

    @Test public void bindMapsFQN() throws Exception {
        assertEquals("UsesBase[Impl1[hello]]", instantiate(UsesBase.class, map("base", map(CLAZZ, Impl1.class.getName(), "text", "hello"))).toString());
    }

    // TODO also check case that a FQN is needed

    @Test public void gstring() throws Exception {
        assertEquals("UsesBase[Impl1[hello world]]", instantiate(UsesBase.class, map("base", map(CLAZZ, "Impl1", "text", new GStringImpl(new Object[]{"hello", "world"}, new String[]{"", " "})))).toString());
    }

    @Test public void nestedStructs() throws Exception {
        roundTrip(UsesBase.class, map("base", map(CLAZZ, "Impl1", "text", "hello")));
        roundTrip(UsesBase.class, map("base", map(CLAZZ, "Impl2", "flag", true)));
        roundTrip(UsesImpl2.class, map("impl2", map()));
        schema(UsesBase.class, "(base: Base{Impl1=(text: String), Impl2=(flag?: boolean), Impl3=(base: Base{Impl1=(text: String), Impl2=(flag?: boolean)})})");
        schema(UsesImpl2.class, "(impl2: Impl2(flag?: boolean))");
        schema(UsesUnimplementedExtensionPoint.class, "(delegate: UnimplementedExtensionPoint{})");
        schema(UsesSomeImplsBroken.class, "(delegate: SomeImplsBroken{FineImpl=()})");
    }

    public static class UsesBase {
        public final Base base;
        @DataBoundConstructor public UsesBase(Base base) {
            this.base = base;
        }
        @Override public String toString() {
            return "UsesBase[" + base + "]";
        }
    }

    public static class UsesImpl2 {
        public final Impl2 impl2;
        @DataBoundConstructor public UsesImpl2(Impl2 impl2) {
            this.impl2 = impl2;
        }
        @Override public String toString() {
            return "UsesImpl2[" + impl2 + "]";
        }
    }

    public static abstract class Base extends AbstractDescribableImpl<Base> {}

    public interface Marker {}

    public static final class Impl1 extends Base implements Marker {
        private final String text;
        @DataBoundConstructor public Impl1(String text) {
            this.text = text;
        }
        public String getText() {
            return text;
        }
        @Override public String toString() {
            return "Impl1[" + text + "]";
        }
        @Extension
        public static final class DescriptorImpl extends Descriptor<Base> {
            @Override public String getDisplayName() {
                return "Implementation #1";
            }
        }
    }

    public static final class Impl2 extends Base {
        private boolean flag;
        @DataBoundConstructor public Impl2() {}
        public boolean isFlag() {
            return flag;
        }
        @DataBoundSetter public void setFlag(boolean flag) {
            this.flag = flag;
        }
        @Override public String toString() {
            return "Impl2[" + flag + "]";
        }
        @Extension public static final class DescriptorImpl extends Descriptor<Base> {
            @Override public String getDisplayName() {
                return "Impl2";
            }
        }
    }

    //use to trigger recursion subcases
    public static final class Impl3 extends Base {
        private final Base base;
        @DataBoundConstructor public Impl3(Base base) {
            this.base = base;
        }
        public Base getBase() {
            return base;
        }
        @Override public String toString() {
            return "Impl3[" + base.toString() + "]";
        }
        @Extension public static final class DescriptorImpl extends Descriptor<Base> {
            @Override public String getDisplayName() {
                return "Impl3";
            }
        }
    }

    public static abstract class UnimplementedExtensionPoint extends AbstractDescribableImpl<UnimplementedExtensionPoint> {}
    public static final class UsesUnimplementedExtensionPoint {
        @DataBoundConstructor public UsesUnimplementedExtensionPoint(UnimplementedExtensionPoint delegate) {}
    }

    public static abstract class SomeImplsBroken extends AbstractDescribableImpl<SomeImplsBroken> {}
    public static class BrokenImpl extends SomeImplsBroken {
        @Extension public static class DescriptorImpl extends Descriptor<SomeImplsBroken> {
            @Override public String getDisplayName() {
                return "BrokenImpl";
            }
        }
    }
    public static class FineImpl extends SomeImplsBroken {
        @DataBoundConstructor public FineImpl() {}
        @Extension public static class DescriptorImpl extends Descriptor<SomeImplsBroken> {
            @Override public String getDisplayName() {
                return "FineImpl";
            }
        }
    }
    public static class UsesSomeImplsBroken {
        @DataBoundConstructor public UsesSomeImplsBroken(SomeImplsBroken delegate) {}
    }

    @Test public void enums() throws Exception {
        roundTrip(UsesEnum.class, map("e", "ZERO"));
        schema(UsesEnum.class, "(e: E[ZERO])");
    }

    public static final class UsesEnum {
        private final E e;
        @DataBoundConstructor public UsesEnum(E e) {
            this.e = e;
        }
        public E getE() {
            return e;
        }
    }
    public enum E {
        ZERO() {@Override public int v() {return 0;}};
        public abstract int v();
    }

    @Test public void urls() throws Exception {
        roundTrip(UsesURL.class, map("u", "http://nowhere.net/"));
        schema(UsesURL.class, "(u?: String)");
    }

    public static final class UsesURL {
        @DataBoundConstructor public UsesURL() {}
        @DataBoundSetter public URL u;
    }

    @Test public void chars() throws Exception {
        roundTrip(UsesCharacter.class, map("c", "!"));
        schema(UsesCharacter.class, "(c?: char)");
    }

    public static final class UsesCharacter {
        @DataBoundConstructor public UsesCharacter() {}
        @DataBoundSetter public char c;
    }

    @Test public void stringArray() throws Exception {
        roundTrip(UsesStringArray.class, map("strings", Arrays.asList("one", "two")));
        schema(UsesStringArray.class, "(strings: String[])");
    }

    @Test public void stringList() throws Exception {
        roundTrip(UsesStringList.class, map("strings", Arrays.asList("one", "two")));
        schema(UsesStringList.class, "(strings: String[])");
    }

    public static final class UsesStringArray {
        private final String[] strings;
        @DataBoundConstructor public UsesStringArray(String[] strings) {
            this.strings = strings;
        }
        public String[] getStrings() {
            return strings;
        }
    }

    public static final class UsesStringList {
        private final List<String> strings;
        @DataBoundConstructor public UsesStringList(List<String> strings) {
            this.strings = strings;
        }
        public List<String> getStrings() {
            return strings;
        }
    }

    @Test public void structArrayHomo() throws Exception {
        roundTrip(UsesStructArrayHomo.class, map("impls", Arrays.asList(map(), map("flag", true))), "UsesStructArrayHomo[Impl2[false], Impl2[true]]");
        schema(UsesStructArrayHomo.class, "(impls: Impl2(flag?: boolean)[])");
    }

    public static final class UsesStructArrayHomo {
        private final Impl2[] impls;
        @DataBoundConstructor public UsesStructArrayHomo(Impl2[] impls) {
            this.impls = impls;
        }
        public Impl2[] getImpls() {
            return impls;
        }
        @Override public String toString() {
            return "UsesStructArrayHomo" + Arrays.toString(impls);
        }
    }

    @Test public void structListHomo() throws Exception {
        roundTrip(UsesStructListHomo.class, map("impls", Arrays.asList(map(), map("flag", true))), "UsesStructListHomo[Impl2[false], Impl2[true]]");
        schema(UsesStructListHomo.class, "(impls: Impl2(flag?: boolean)[])");
    }

    public static final class UsesStructListHomo {
        private final List<Impl2> impls;
        @DataBoundConstructor public UsesStructListHomo(List<Impl2> impls) {
            this.impls = impls;
        }
        public List<Impl2> getImpls() {
            return impls;
        }
        @Override public String toString() {
            return "UsesStructListHomo" + impls;
        }
    }

    @Test public void structCollectionHomo() throws Exception {
        roundTrip(UsesStructCollectionHomo.class, map("impls", Arrays.asList(map(), map("flag", true))), "UsesStructCollectionHomo[Impl2[false], Impl2[true]]");
        schema(UsesStructCollectionHomo.class, "(impls: Impl2(flag?: boolean)[])");
    }

    public static final class UsesStructCollectionHomo {
        private final Collection<Impl2> impls;
        @DataBoundConstructor public UsesStructCollectionHomo(Collection<Impl2> impls) {
            this.impls = impls;
        }
        public Collection<Impl2> getImpls() {
            return impls;
        }
        @Override public String toString() {
            return "UsesStructCollectionHomo" + impls;
        }
    }

    @Test public void structArrayHetero() throws Exception {
        roundTrip(UsesStructArrayHetero.class, map("bases", Arrays.asList(map(CLAZZ, "Impl1", "text", "hello"), map(CLAZZ, "Impl2", "flag", true))), "UsesStructArrayHetero[Impl1[hello], Impl2[true]]");
        schema(UsesStructArrayHetero.class, "(bases: Base{Impl1=(text: String), Impl2=(flag?: boolean), Impl3=(base: Base{Impl1=(text: String), Impl2=(flag?: boolean)})}[])");
    }

    public static final class UsesStructArrayHetero {
        private final Base[] bases;
        @DataBoundConstructor public UsesStructArrayHetero(Base[] bases) {
            this.bases = bases;
        }
        public Base[] getBases() {
            return bases;
        }
        @Override public String toString() {
            return "UsesStructArrayHetero" + Arrays.toString(bases);
        }
    }

    @Test public void structListHetero() throws Exception {
        roundTrip(UsesStructListHetero.class, map("bases", Arrays.asList(map(CLAZZ, "Impl1", "text", "hello"), map(CLAZZ, "Impl2", "flag", true))), "UsesStructListHetero[Impl1[hello], Impl2[true]]");
        schema(UsesStructListHetero.class, "(bases: Base{Impl1=(text: String), Impl2=(flag?: boolean), Impl3=(base: Base{Impl1=(text: String), Impl2=(flag?: boolean)})}[])");
    }

    public static final class UsesStructListHetero {
        private final List<Base> bases;
        @DataBoundConstructor public UsesStructListHetero(List<Base> bases) {
            this.bases = bases;
        }
        public List<Base> getBases() {
            return bases;
        }
        @Override public String toString() {
            return "UsesStructListHetero" + bases;
        }
    }

    @Test public void structCollectionHetero() throws Exception {
        roundTrip(UsesStructCollectionHetero.class, map("bases", Arrays.asList(map(CLAZZ, "Impl1", "text", "hello"), map(CLAZZ, "Impl2", "flag", true))), "UsesStructCollectionHetero[Impl1[hello], Impl2[true]]");
        schema(UsesStructCollectionHetero.class, "(bases: Base{Impl1=(text: String), Impl2=(flag?: boolean), Impl3=(base: Base{Impl1=(text: String), Impl2=(flag?: boolean)})}[])");
    }

    public static final class UsesStructCollectionHetero {
        private final Collection<Base> bases;
        @DataBoundConstructor public UsesStructCollectionHetero(Collection<Base> bases) {
            this.bases = bases;
        }
        public Collection<Base> getBases() {
            return bases;
        }
        @Override public String toString() {
            return "UsesStructCollectionHetero" + bases;
        }
    }

    @Test public void defaultValuesStructCollectionCommon() throws Exception {
        roundTrip(DefaultStructCollection.class, map("bases", Arrays.asList(map(CLAZZ, "Impl1", "text", "special"))), "DefaultStructCollection[Impl1[special]]");
    }

    @Test public void defaultValuesStructCollectionEmpty() throws Exception {
        roundTrip(DefaultStructCollection.class, map("bases", Collections.emptyList()), "DefaultStructCollection[]");
    }

    @Issue("JENKINS-25779")
    @Test public void defaultValuesStructCollection() throws Exception {
        roundTrip(DefaultStructCollection.class, map(), "DefaultStructCollection[Impl1[default]]");
    }

    @Issue("JENKINS-25779")
    @Test public void defaultValuesNestedStruct() throws Exception {
        roundTrip(DefaultStructCollection.class, map("bases", Arrays.asList(map(CLAZZ, "Impl2"), map(CLAZZ, "Impl2", "flag", true))), "DefaultStructCollection[Impl2[false], Impl2[true]]");
    }

    @Issue("JENKINS-25779")
    @Test public void defaultValuesNullSetter() throws Exception {
        roundTrip(DefaultStructCollection.class, map("bases", null), "DefaultStructCollectionnull");
    }

    public static final class DefaultStructCollection {
        private Collection<Base> bases = Arrays.<Base>asList(new Impl1("default"));
        @DataBoundConstructor public DefaultStructCollection() {}
        public Collection<Base> getBases() {return bases;}
        @DataBoundSetter public void setBases(Collection<Base> bases) {this.bases = bases;}
        @Override public String toString() {return "DefaultStructCollection" + bases;}
    }

    @Test public void defaultValuesStructArrayCommon() throws Exception {
        roundTrip(DefaultStructArray.class, map("bases", Arrays.asList(map(CLAZZ, "Impl1", "text", "special")), "stuff", "val"), "DefaultStructArray[Impl1[special]];stuff=val");
    }

    @Issue("JENKINS-25779")
    @Test public void defaultValuesStructArray() throws Exception {
        roundTrip(DefaultStructArray.class, map("stuff", "val"), "DefaultStructArray[Impl1[default], Impl2[true]];stuff=val");
    }

    @Issue("JENKINS-25779")
    @Test public void defaultValuesNullConstructorParameter() throws Exception {
        roundTrip(DefaultStructArray.class, map(), "DefaultStructArray[Impl1[default], Impl2[true]];stuff=null");
    }

    public static final class DefaultStructArray {
        private final String stuff;
        private Base[] bases;
        @DataBoundConstructor public DefaultStructArray(String stuff) {
            this.stuff = stuff;
            Impl2 impl2 = new Impl2();
            impl2.setFlag(true);
            bases = new Base[] {new Impl1("default"), impl2};
        }
        public Base[] getBases() {return bases;}
        @DataBoundSetter public void setBases(Base[] bases) {this.bases = bases;}
        public String getStuff() {return stuff;}
        @Override public String toString() {return "DefaultStructArray" + Arrays.toString(bases) + ";stuff=" + stuff;}
    }

    @Issue("JENKINS-26093")
    @Test public void parameterValues() throws Exception {
        assertTrue(DescribableModel.findSubtypes(ParameterValue.class).contains(BooleanParameterValue.class)); //  do not want to enumerate ListSubversionTagsParameterValue etc.
        // Omitting RunParameterValue since it is not friendly for a unit test.
        // JobParameterDefinition is not registered as an extension, so not supporting FileParameterValue.
        // FileParameterValue is unsupportable as an input to a WorkflowJob since it requires createBuildWrapper to work;
        // as an argument to BuildTriggerStep it could perhaps work, but we would need to provide a custom FileItem implementation.
        // PasswordParameterValue requires Secret.fromString and thus JenkinsRule.
        // For others: https://github.com/search?type=Code&q=user%3Ajenkinsci+user%3Acloudbees+%22extends+ParameterDefinition%22
        roundTrip(TakesParams.class, map("parameters", Arrays.asList(
                map(CLAZZ, "BooleanParameterValue", "name", "flag", "value", true),
                map(CLAZZ, "StringParameterValue", "name", "n", "value", "stuff"),
                map(CLAZZ, "TextParameterValue", "name", "text", "value", "here\nthere"))),
            "TakesParams;BooleanParameterValue:flag=true;StringParameterValue:n=stuff;TextParameterValue:text=here\nthere");
    }
    public static final class TakesParams {
        public final List<ParameterValue> parameters;
        @DataBoundConstructor public TakesParams(List<ParameterValue> parameters) {
            this.parameters = parameters;
        }
        @Override public String toString() {
            StringBuilder b = new StringBuilder("TakesParams");
            for (ParameterValue v : parameters) {
                b.append(';').append(v.getClass().getSimpleName()).append(':').append(v.getShortDescription());
            }
            return b.toString();
        }
    }

    @Test public void parametersDefinitionProperty() throws Exception {
        roundTrip(ParametersDefinitionProperty.class, map("parameterDefinitions", Arrays.asList(map(CLAZZ, "BooleanParameterDefinition", "name", "flag", "defaultValue", false), map(CLAZZ, "StringParameterDefinition", "name", "text"))));
    }

    @Issue("JENKINS-26619")
    @Test public void getterDescribableList() throws Exception {
        roundTrip(GitSCM.class, map(
            "extensions", Arrays.asList(map(CLAZZ, CleanBeforeCheckout.class.getSimpleName())),
            // Default values for these things do not work because GitSCM fails to use @DataBoundSetter:
            "branches", Arrays.asList(map("name", "*/master")),
            "doGenerateSubmoduleConfigurations", false,
            "submoduleCfg", Collections.emptyList(),
            "userRemoteConfigs", Collections.emptyList()));
    }

    @Issue("JENKINS-32925") // but Base3 usages are the more realistic case
    @Test
    public void recursion() throws Exception {
        schema(Recursion.class, "(foo?: Recursion(foo?: java.lang.IllegalArgumentException: type that refers to itself))");
    }

    public static class Recursion {
        @DataBoundConstructor
        public Recursion() {}
        @DataBoundSetter
        public void setFoo(Recursion r) {}
    }

    private static Map<String,Object> map(Object... keysAndValues) {
        if (keysAndValues.length % 2 != 0) {
            throw new IllegalArgumentException();
        }
        Map<String,Object> m = new TreeMap<String,Object>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            m.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return m;
    }

    private void roundTrip(Class<?> c, Map<String,Object> m) throws Exception {
        roundTrip(c, m, null);
    }

    private void roundTrip(Class<?> c, Map<String,Object> m, String toString) throws Exception {
        Object o = instantiate(c, m);
        if (toString != null) {
            assertEquals(toString, o.toString());
        }
        Map<String,Object> m2 = uninstantiate(o);
        assertEquals(m, m2);
    }

    private static void schema(Class<?> c, String schema) throws Exception {
        assertEquals(schema, new DescribableModel(c).toString());
    }

}