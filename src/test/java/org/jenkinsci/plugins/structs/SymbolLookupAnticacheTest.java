package org.jenkinsci.plugins.structs;

import hudson.model.Descriptor;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.plugins.structs.symbolLookupAnticacheTest.TrivialBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.RealJenkinsRule;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Sam Van Oort
 */
public class SymbolLookupAnticacheTest {

    @Rule public RealJenkinsRule rjr = new RealJenkinsRule();

    /** Verify that if we install a new plugin with a Symbol use, that symbol is found.
     *  Without the plugin we hit the "anticache" i.e. {@link SymbolLookup#noHitCache} that the symbol does not exist.
     *  Once we add the plugin it should hit the cache.
     */
    @Test public void testAnticache() throws Throwable {
        var plugin = rjr.createSyntheticPlugin(new RealJenkinsRule.SyntheticPlugin(TrivialBuilder.class).shortName("SymbolLookupAnticacheTest").header("Plugin-Dependencies", "structs:0"));
        rjr.then(rule -> {
        Descriptor d = SymbolLookup.get().findDescriptor(SimpleBuildStep.class, "trivialBuilder");
        Descriptor stepDescriptor = SymbolLookup.get().find(Descriptor.class, "trivialBuilder");
        assertNull(d);
        assertNull(stepDescriptor);

        rule.jenkins.getPluginManager().dynamicLoad(plugin);

        d = SymbolLookup.get().findDescriptor(SimpleBuildStep.class, "trivialBuilder");
        stepDescriptor = SymbolLookup.get().find(Descriptor.class, "trivialBuilder");
        assertNotNull(d);
        assertNotNull(stepDescriptor);
        assertEquals("simpleBuild", d.getDisplayName());
        assertEquals("TrivialBuilder", stepDescriptor.clazz.getSimpleName());

        // Once more just to confirm there's no wackiness when hitting the normal cache too
        assertEquals(SymbolLookup.get().findDescriptor(SimpleBuildStep.class, "trivialBuilder"), d);
        });
    }

}
