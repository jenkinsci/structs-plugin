package org.jenkinsci.plugins.structs;

import hudson.model.Descriptor;
import jenkins.tasks.SimpleBuildStep;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Sam Van Oort
 */
@WithJenkins
class SymbolLookupAnticacheTest {

    /** Verify that if we install a new plugin with a Symbol use, that symbol is found.
     *  Without the plugin we hit the "anticache" i.e. {@link SymbolLookup#noHitCache} that the symbol does not exist.
     *  Once we add the plugin it should hit the cache.
     */
    @Test
    void testAnticache(JenkinsRule rule) throws Exception{
        Descriptor d = SymbolLookup.get().findDescriptor(SimpleBuildStep.class, "trivialBuilder");
        Descriptor stepDescriptor = SymbolLookup.get().find(Descriptor.class, "trivialBuilder");
        assertNull(d);
        assertNull(stepDescriptor);

        // See the test-plugin folder to rebuild this
        File plugin = new File(SymbolLookupAnticacheTest.class.getResource("/structs-test-plugin.hpi").toURI());
        rule.jenkins.getPluginManager().dynamicLoad(plugin);

        d = SymbolLookup.get().findDescriptor(SimpleBuildStep.class, "trivialBuilder");
        stepDescriptor = SymbolLookup.get().find(Descriptor.class, "trivialBuilder");
        assertNotNull(d);
        assertNotNull(stepDescriptor);
        assertEquals("simpleBuild", d.getDisplayName());
        assertEquals("org.jenkinsci.plugins.structstest.TrivialBuilder", stepDescriptor.clazz.getName());

        // Once more just to confirm there's no wackiness when hitting the normal cache too
        assertEquals(SymbolLookup.get().findDescriptor(SimpleBuildStep.class, "trivialBuilder"), d);
    }

}
