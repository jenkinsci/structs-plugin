package org.jenkinsci;

import org.jvnet.hudson.annotation_indexer.Indexed;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines a unique identifier to refer to an constant value
 *
 * @since TODO
 */
@Indexed
@Retention(RUNTIME)
@Target({FIELD})
@Documented
public @interface ConstSymbol {
    String[] value();
}
