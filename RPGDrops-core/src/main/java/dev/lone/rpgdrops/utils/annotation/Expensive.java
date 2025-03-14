package dev.lone.rpgdrops.utils.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Expensive method invocation
 */
@Retention(RetentionPolicy.SOURCE)
public @interface Expensive
{
    boolean singleCall() default true;
    boolean calledInLoop() default true;
    boolean calledInLambda() default true;
}