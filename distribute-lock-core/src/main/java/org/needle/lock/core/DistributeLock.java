package org.needle.lock.core;

import java.lang.annotation.*;

/**
 * @author nanshan
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DistributeLock {

    /**
     * 过期时间，默认60s
     */
    long timeout() default 60000;

    /**
     * 名称
     */
    String name();
}
