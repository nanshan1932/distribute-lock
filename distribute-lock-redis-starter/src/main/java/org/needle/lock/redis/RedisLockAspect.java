package org.needle.lock.redis;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.needle.lock.core.DistributeLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.util.StringUtils;

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

            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Object[] arguments = joinPoint.getArgs();
            Field[] field = arguments[0].getClass().getDeclaredFields();
            String value = "";
            for (int j = 0; j < field.length; j++) {

                boolean fieldHasAnno = field[j].isAnnotationPresent(DistributeLock.class);
                if (fieldHasAnno) {
                    DistributeLock fieldAnno = field[j].getAnnotation(DistributeLock.class);
                    //输出注解属性
                    String name = field[j].getName();
                    name = name.substring(0, 1).toUpperCase() + name.substring(1);
                    Method m = arguments[0].getClass().getMethod("get" + name);
                    value = (String) m.invoke(arguments[0]);
                    System.out.println(value);
                }
            }
            // 获取锁的key
            Object lockKey = value;
            if (lockKey == null || StringUtils.isEmpty(lockKey)) {
                lockKey = "publistLock";
            }
            Lock lock = redisLockRegistry.obtain(lockKey);

            try {
                boolean ifLock = lock.tryLock(3, TimeUnit.SECONDS);
//                mLog.info("线程[{}]是否获取到了锁：{ }", Thread.currentThread().getName(), ifLock);
                /*
                 * 可以获取到锁，说明当前没有线程在执行该方法
                 */
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
