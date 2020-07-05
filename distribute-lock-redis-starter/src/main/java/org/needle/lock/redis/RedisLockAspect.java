package org.needle.lock.redis;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.needle.lock.core.DistributeLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Aspect
@Order
public class RedisLockAspect {
    Logger logger = LoggerFactory.getLogger(RedisLockAspect.class);

    @Autowired
    private RedisLockRegistry redisLockRegistry;


    @Around(value = "@annotation(org.needle.lock.core.DistributeLock)")
    public synchronized Object redisLock(ProceedingJoinPoint joinPoint) {
        logger.info("start redis lock...");
        Object output = null;
        try {

            MethodSignature targetMethod = (MethodSignature) joinPoint.getSignature();
            Method method = targetMethod.getMethod();
            DistributeLock distributeLock = AnnotationUtils.findAnnotation(method, DistributeLock.class);
            // 获取锁的key
            long timeout = distributeLock.timeout();
            String name = distributeLock.name();

            Lock lock = redisLockRegistry.obtain(name);

            try {
                boolean ifLock = lock.tryLock(timeout, TimeUnit.SECONDS);

                if (ifLock) {
                    output = joinPoint.proceed();
                } else {
                    throw new RuntimeException("服务器异常");
                }
            } catch (Exception e) {
//                mLog.error("执行核心奖励扫描时出错:{}", e.getMessage());
            } finally {
//                mLog.info("尝试解锁[{}]", lockKey);
                try {
                    lock.unlock();
//                    mLog.info("[{}]解锁成功", lockKey);
                } catch (Exception e) {
//                    mLog.error("解锁dealAction出错:{}", e.getMessage());
                }
            }
        } catch (Throwable e) {
            logger.error("aop redis distributed lock error:{}", e.getLocalizedMessage());
        }
        return output;
    }

}
