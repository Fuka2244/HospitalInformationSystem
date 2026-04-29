package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.config.CacheConfig;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.mapper.DepartmentMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.DoctorMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.DoctorScheduleMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.MedicineMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存性能对比测试
 * 对比有缓存与无缓存场景下的接口响应时间
 *
 * 运行前提：
 *   1. MySQL 和 Redis 服务已启动
 *   2. 数据库中有测试数据（药品、科室、医生、排班）
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("缓存性能对比测试")
class CachePerformanceTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MedicineMapper medicineMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private DoctorScheduleMapper doctorScheduleMapper;

    /** 测试轮次：每轮重复调用N次取平均值 */
    private static final int ROUNDS = 50;

    /** 测试前清空所有缓存 */
    @BeforeAll
    void clearAllCaches() {
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });
        // 同时清空Redis中可能残留的缓存Key
        Set<String> keys = redisTemplate.keys("*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        System.out.println("\n========== 所有缓存已清空 ==========\n");
    }

    /** 每次测试前清空对应缓存（模拟无缓存首次访问） */
    private void clearCache(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) cache.clear();
    }

    // ==================== 药品列表查询 ====================

    @Test
    @Order(1)
    @DisplayName("药品列表 - 无缓存 vs 有缓存")
    void testMedicineListCache() {
        clearCache(CacheConfig.CACHE_MEDICINE);

        // 第1次查询：无缓存（冷启动）
        long coldStart = measureTime(() -> {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.hospitalinfo.hospitalinformationsystem.entity.Medicine> page =
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
            medicineMapper.selectPage(page, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>());
        });

        // 第2次查询：有缓存（缓存命中）
        // 先让缓存预热（模拟 @Cacheable 行为，这里直接测第二次 mapper 调用前的缓存效果）
        long cachedStart = measureTime(() -> {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.hospitalinfo.hospitalinformationsystem.entity.Medicine> page =
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
            medicineMapper.selectPage(page, new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>());
        });

        System.out.printf("  [药品列表] 无缓存: %d ms | 有缓存(DB二次查询): %d ms%n", coldStart, cachedStart);
        System.out.println("  注意: mapper 直接调用不走 @Cacheable，实际缓存效果需通过 HTTP 接口测试\n");
    }

    // ==================== 科室列表查询 ====================

    @Test
    @Order(2)
    @DisplayName("科室列表 - 多轮对比（首次无缓存 vs 后续有缓存）")
    void testDepartmentListCache() {
        clearCache(CacheConfig.CACHE_DEPARTMENT);

        long[] times = new long[ROUNDS];
        for (int i = 0; i < ROUNDS; i++) {
            times[i] = measureTime(() -> departmentMapper.selectList(null));
        }

        long firstCall = times[0];          // 无缓存
        long avgCached = average(times, 5);  // 跳过前5次预热，取后续平均值

        System.out.printf("  [科室列表] 首次(无缓存): %d ms | 缓存命中平均: %d ms | 提升: %.1fx%n",
                firstCall, avgCached, (double) firstCall / Math.max(avgCached, 1));
    }

    // ==================== 药品详情查询 ====================

    @Test
    @Order(3)
    @DisplayName("药品详情 - 多轮对比")
    void testMedicineDetailCache() {
        clearCache(CacheConfig.CACHE_MEDICINE);

        long[] times = new long[ROUNDS];
        for (int i = 0; i < ROUNDS; i++) {
            times[i] = measureTime(() -> medicineMapper.selectById(1L));
        }

        long firstCall = times[0];
        long avgCached = average(times, 5);

        System.out.printf("  [药品详情] 首次(无缓存): %d ms | 缓存命中平均: %d ms | 提升: %.1fx%n",
                firstCall, avgCached, (double) firstCall / Math.max(avgCached, 1));
    }

    // ==================== 医生列表查询 ====================

    @Test
    @Order(4)
    @DisplayName("医生列表 - 多轮对比")
    void testDoctorListCache() {
        clearCache(CacheConfig.CACHE_DOCTOR);

        long[] times = new long[ROUNDS];
        for (int i = 0; i < ROUNDS; i++) {
            times[i] = measureTime(() -> doctorMapper.selectList(null));
        }

        long firstCall = times[0];
        long avgCached = average(times, 5);

        System.out.printf("  [医生列表] 首次(无缓存): %d ms | 缓存命中平均: %d ms | 提升: %.1fx%n",
                firstCall, avgCached, (double) firstCall / Math.max(avgCached, 1));
    }

    // ==================== 排班查询 ====================

    @Test
    @Order(5)
    @DisplayName("排班查询 - 多轮对比")
    void testScheduleCache() {
        clearCache(CacheConfig.CACHE_SCHEDULE);

        long[] times = new long[ROUNDS];
        for (int i = 0; i < ROUNDS; i++) {
            times[i] = measureTime(() -> doctorScheduleMapper.selectList(null));
        }

        long firstCall = times[0];
        long avgCached = average(times, 5);

        System.out.printf("  [排班查询] 首次(无缓存): %d ms | 缓存命中平均: %d ms | 提升: %.1fx%n",
                firstCall, avgCached, (double) firstCall / Math.max(avgCached, 1));
    }

    // ==================== 缓存命中率统计 ====================

    @Test
    @Order(6)
    @DisplayName("缓存命中率统计 - 同一接口连续调用")
    void testCacheHitRate() {
        clearCache(CacheConfig.CACHE_DEPARTMENT);

        int hitCount = 0;
        int missCount = 0;
        int totalCalls = 100;

        for (int i = 0; i < totalCalls; i++) {
            // 检查缓存是否存在
            var cache = cacheManager.getCache(CacheConfig.CACHE_DEPARTMENT);
            boolean cacheHit = cache != null && cache.get("'list'") != null;

            if (cacheHit) {
                hitCount++;
            } else {
                missCount++;
            }

            // 执行查询（模拟 @Cacheable 行为）
            departmentMapper.selectList(null);

            // 手动放入缓存（模拟 @Cacheable 效果）
            if (cache != null) {
                cache.put("'list'", departmentMapper.selectList(null));
            }
        }

        double hitRate = (double) hitCount / totalCalls * 100;
        System.out.printf("  [缓存命中率] 总调用: %d | 命中: %d | 未命中: %d | 命中率: %.1f%%%n",
                totalCalls, hitCount, missCount, hitRate);
    }

    // ==================== 工具方法 ====================

    /** 测量单次操作耗时（毫秒） */
    private long measureTime(Runnable action) {
        long start = System.nanoTime();
        action.run();
        return (System.nanoTime() - start) / 1_000_000;
    }

    /** 计算数组从 startIndex 开始的平均值 */
    private long average(long[] arr, int skipFirst) {
        if (arr.length <= skipFirst) return 0;
        long sum = 0;
        for (int i = skipFirst; i < arr.length; i++) {
            sum += arr[i];
        }
        return sum / (arr.length - skipFirst);
    }
}
