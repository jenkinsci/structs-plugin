package org.jenkinsci.plugins.structs.describable;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Kohsuke Kawaguchi
 */
class UninstantiatedDescribableTest {

    @Test
    void _toString() {
        assertEquals("@symbol$class(x=4,y=hello)", make().toString());
    }

    @Test
    void equals() {
        assertEquals(make(),make());
        assertEquals(make().hashCode(),make().hashCode());
    }

    private Object make() {
        Map<String, Object> args = new TreeMap<>();
        args.put("x", 4);
        args.put("y", "hello");
        return new UninstantiatedDescribable("symbol", "class", args);
    }
}