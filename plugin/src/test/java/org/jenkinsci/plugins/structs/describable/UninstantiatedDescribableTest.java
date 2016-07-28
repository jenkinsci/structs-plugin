package org.jenkinsci.plugins.structs.describable;

import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.*;

/**
 * @author Kohsuke Kawaguchi
 */
public class UninstantiatedDescribableTest {
    @Test
    public void _toString() {
        assertEquals("@symbol$class(x=4,y=hello)", make().toString());
    }

    @Test
    public void equals() {
        assertEquals(make(),make());
        assertEquals(make().hashCode(),make().hashCode());
    }

    private Object make() {
        Map args = new TreeMap();
        args.put("x",4);
        args.put("y","hello");
        return new UninstantiatedDescribable("symbol", "class", args);
    }
}