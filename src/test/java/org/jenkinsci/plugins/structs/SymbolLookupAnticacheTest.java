package org.jenkinsci.plugins.structs;

import hudson.model.Descriptor;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.plugins.structs.symbolLookupAnticacheTest.TrivialBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.jvnet.hudson.test.junit.jupiter.RealJenkinsExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Sam Van Oort
 */
class SymbolLookupAnticacheTest {

    @RegisterExtension
    private final RealJenkinsExtension rjr = new RealJenkinsExtension();

    /** Verify that if we install a new plugin with a Symbol use, that symbol is found.
     *  Without the plugin we hit the "anticache" i.e. {@link SymbolLookup#noHitCache} that the symbol does not exist.
     *  Once we add the plugin it should hit the cache.
     */
    @Test
    void testAnticache() throws Throwable {
        var plugin = rjr.createSyntheticPlugin(new RealJenkinsExtension.SyntheticPlugin(TrivialBuilder.class).shortName("SymbolLookupAnticacheTest").header("Plugin-Dependencies", "structs:0"));
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
