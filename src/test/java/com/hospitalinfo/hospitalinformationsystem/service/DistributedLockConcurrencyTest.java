package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.utils.RedisDistributedLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDate;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分布式锁并发测试
 * 需要启动本地 Redis 服务
 */
@SpringBootTest
@DisplayName("分布式锁 - 并发测试")
class DistributedLockConcurrencyTest {

    @Autowired
    private RedisDistributedLock distributedLock;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void cleanUp() {
        // 清理测试用的锁Key
        var keys = redisTemplate.keys("test:lock:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("基本加锁解锁 - 成功")
    void testLockAndUnlock() {
        String lockKey = "test:lock:basic";
        String lockValue = distributedLock.tryLock(lockKey);

        assertNotNull(lockValue, "应该成功获取锁");

        // 验证锁存在于 Redis
        Object redisValue = redisTemplate.opsForValue().get(lockKey);
        assertEquals(lockValue, redisValue, "Redis中应存储锁值");

        // 解锁
        boolean unlocked = distributedLock.unlock(lockKey, lockValue);
        assertTrue(unlocked, "应该成功释放锁");

        // 验证锁已从 Redis 删除
        assertNull(redisTemplate.opsForValue().get(lockKey), "锁应已从Redis删除");
    }

    @Test
    @DisplayName("互斥性 - 同一Key只能有一个持有者")
    void testMutex() {
        String lockKey = "test:lock:mutex";
        String lockValue1 = distributedLock.tryLock(lockKey);

        assertNotNull(lockValue1, "第一个线程应成功获取锁");

        try {
            // 第二次获取同一Key的锁（不重试）
            String lockValue2 = distributedLock.tryLock(lockKey, 10, 1, 0);
            assertNull(lockValue2, "同一Key的锁应被互斥，第二次获取应失败");
        } finally {
            distributedLock.unlock(lockKey, lockValue1);
        }
    }

    @Test
    @DisplayName("安全性 - 不能释放别人的锁")
    void testUnlockSafety() {
        String lockKey = "test:lock:safety";
        String lockValue = distributedLock.tryLock(lockKey);

        assertNotNull(lockValue);

        try {
            // 用错误的 value 尝试释放锁
            boolean result = distributedLock.unlock(lockKey, "wrong-value");
            assertFalse(result, "用错误的锁值释放应失败");

            // 验证锁仍然存在
            assertNotNull(redisTemplate.opsForValue().get(lockKey), "锁不应被误删");
        } finally {
            distributedLock.unlock(lockKey, lockValue);
        }
    }

    @Test
    @DisplayName("自动过期 - 锁超时后自动释放，防止死锁")
    void testAutoExpire() throws InterruptedException {
        String lockKey = "test:lock:expire";
        // 设置1秒过期
        String lockValue = distributedLock.tryLock(lockKey, 1, 3, 100);

        assertNotNull(lockValue);

        // 不手动释放，等待2秒让锁自动过期
        Thread.sleep(2000);

        // 验证锁已被自动清除
        assertNull(redisTemplate.opsForValue().get(lockKey), "锁应已自动过期");

        // 应该可以重新获取锁
        String newLockValue = distributedLock.tryLock(lockKey);
        assertNotNull(newLockValue, "锁过期后应可重新获取");
        distributedLock.unlock(lockKey, newLockValue);
    }

    @Test
    @DisplayName("并发场景 - 100个线程争抢同一把锁，只有一个成功")
    void testConcurrentLockContention() throws InterruptedException {
        String lockKey = "test:lock:concurrent";
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // 等待所有线程就绪
                    String lockValue = distributedLock.tryLock(lockKey, 30, 1, 0);
                    if (lockValue != null) {
                        try {
                            successCount.incrementAndGet();
                            Thread.sleep(50); // 模拟业务耗时
                        } finally {
                            distributedLock.unlock(lockKey, lockValue);
                        }
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 忽略
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 同时开始
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(1, successCount.get(), "只应有一个线程成功获取锁");
        assertEquals(threadCount - 1, failCount.get(), "其余线程应获取失败");
    }

    @Test
    @DisplayName("预约场景模拟 - 并发预约同一排班，号源不超卖")
    void testAppointmentConcurrency() throws InterruptedException {
        String lockKeyPrefix = "lock:appointment:";
        Long doctorId = 100L;
        LocalDate date = LocalDate.of(2025, 6, 15);
        String timeSlot = "09:00-10:00";
        int maxPatients = 5; // 只剩5个号
        int threadCount = 20; // 20人抢

        // 模拟 booked_count
        String countKey = "test:appointment:count:" + doctorId;
        redisTemplate.opsForValue().set(countKey, 0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int patientIndex = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    String lockKey = lockKeyPrefix + doctorId + ":" + date + ":" + timeSlot;
                    String lockValue = distributedLock.tryLock(lockKey, 10, 10, 200);

                    if (lockValue != null) {
                        try {
                            // 模拟原子递增（用Redis INCR代替数据库）
                            Long currentCount = redisTemplate.opsForValue().increment(countKey);
                            if (currentCount != null && currentCount <= maxPatients) {
                                successCount.incrementAndGet();
                            } else {
                                // 超出名额，回滚
                                redisTemplate.opsForValue().decrement(countKey);
                            }
                        } finally {
                            distributedLock.unlock(lockKey, lockValue);
                        }
                    }
                } catch (Exception e) {
                    // 忽略
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals(maxPatients, successCount.get(),
                "成功预约数应等于最大号源数，不应超卖");

        // 验证最终 booked_count == maxPatients
        Object finalCount = redisTemplate.opsForValue().get(countKey);
        assertEquals(maxPatients, Integer.parseInt(finalCount.toString()),
                "最终已预约数应等于最大号源数");

        // 清理
        redisTemplate.delete(countKey);
    }

    @Test
    @DisplayName("不同排班互不影响 - 不同Key的锁可并行获取")
    void testDifferentKeysParallel() throws InterruptedException {
        int pairCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(pairCount * 2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(pairCount * 2);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < pairCount; i++) {
            final int index = i;
            // 每对线程争抢不同的锁
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String lockValue = distributedLock.tryLock("test:lock:parallel:" + index);
                    if (lockValue != null) {
                        successCount.incrementAndGet();
                        Thread.sleep(100);
                        distributedLock.unlock("test:lock:parallel:" + index, lockValue);
                    }
                } catch (Exception e) { /* ignore */ }
                finally { doneLatch.countDown(); }
            });

            executor.submit(() -> {
                try {
                    startLatch.await();
                    String lockValue = distributedLock.tryLock("test:lock:parallel:" + index);
                    if (lockValue != null) {
                        successCount.incrementAndGet();
                        Thread.sleep(100);
                        distributedLock.unlock("test:lock:parallel:" + index, lockValue);
                    }
                } catch (Exception e) { /* ignore */ }
                finally { doneLatch.countDown(); }
            });
        }

        startLatch.countDown();
        doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // 5对线程各有一人成功（先抢到的），共5个
        assertEquals(pairCount, successCount.get(),
                "不同Key的锁互不影响，每对应有1个成功");
    }
}
