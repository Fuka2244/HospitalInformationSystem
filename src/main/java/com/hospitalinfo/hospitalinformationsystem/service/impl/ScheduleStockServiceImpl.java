package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.hospitalinfo.hospitalinformationsystem.service.IScheduleStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 排班号源库存服务实现
 * 使用Redis管理号源库存，支持原子扣减和回补
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleStockServiceImpl implements IScheduleStockService {

    private final RedisTemplate<String, Object> redisTemplate;

    /** 库存Key前缀 */
    private static final String STOCK_KEY_PREFIX = "schedule:stock:";
    /** 库存初始化标记Key前缀 */
    private static final String STOCK_INIT_PREFIX = "schedule:stock:init:";
    /** 库存默认过期时间：7天 */
    private static final long STOCK_TTL_DAYS = 7;

    /**
     * Lua脚本：原子扣减库存（防止超卖）
     * 返回值：1=成功, 0=库存不足, -1=未初始化
     */
    private static final String DECREMENT_SCRIPT =
            "local stockKey = KEYS[1] " +
            "local stock = redis.call('GET', stockKey) " +
            "if stock == false then " +
            "   return -1 " +  // 未初始化
            "end " +
            "stock = tonumber(stock) " +
            "if stock <= 0 then " +
            "   return 0 " +  // 库存不足
            "end " +
            "redis.call('DECR', stockKey) " +
            "return 1";      // 成功

    /**
     * Lua脚本：原子回补库存
     */
    private static final String INCREMENT_SCRIPT =
            "local stockKey = KEYS[1] " +
            "local maxKey = KEYS[2] " +
            "local stock = redis.call('GET', stockKey) " +
            "local maxStock = redis.call('GET', maxKey) " +
            "if stock == false or maxStock == false then " +
            "   return 0 " +
            "end " +
            "stock = tonumber(stock) " +
            "maxStock = tonumber(maxStock) " +
            "if stock >= maxStock then " +
            "   return 0 " +  // 已达到最大库存
            "end " +
            "redis.call('INCR', stockKey) " +
            "return 1";

    @Override
    public void initStock(Long doctorId, String scheduleDate, String timeSlot, int maxPatients) {
        String scheduleKey = buildScheduleKey(doctorId, scheduleDate, timeSlot);
        initStockBatch(scheduleKey, maxPatients);
    }

    @Override
    public void initStockBatch(String scheduleKey, int maxPatients) {
        String stockKey = STOCK_KEY_PREFIX + scheduleKey;
        String initKey = STOCK_INIT_PREFIX + scheduleKey;

        try {
            // 设置库存值
            redisTemplate.opsForValue().set(stockKey, maxPatients, STOCK_TTL_DAYS, TimeUnit.DAYS);
            // 设置初始化标记（包含最大库存信息）
            redisTemplate.opsForValue().set(initKey, maxPatients, STOCK_TTL_DAYS, TimeUnit.DAYS);

            log.info("号源库存初始化成功: scheduleKey={}, maxPatients={}", scheduleKey, maxPatients);
        } catch (Exception e) {
            log.error("号源库存初始化失败: scheduleKey={}, error={}", scheduleKey, e.getMessage());
            throw new RuntimeException("号源库存初始化失败");
        }
    }

    @Override
    public boolean preDecrementStock(Long doctorId, String scheduleDate, String timeSlot) {
        String scheduleKey = buildScheduleKey(doctorId, scheduleDate, timeSlot);
        return preDecrementStock(scheduleKey);
    }

    @Override
    public boolean preDecrementStock(String scheduleKey) {
        String stockKey = STOCK_KEY_PREFIX + scheduleKey;

        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(DECREMENT_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script, Collections.singletonList(stockKey));

            if (result == null || result == -1) {
                log.warn("库存未初始化: scheduleKey={}", scheduleKey);
                return false;
            }

            if (result == 0) {
                log.info("库存不足: scheduleKey={}", scheduleKey);
                return false;
            }

            log.debug("库存预扣减成功: scheduleKey={}", scheduleKey);
            return true;
        } catch (Exception e) {
            log.error("库存预扣减失败: scheduleKey={}, error={}", scheduleKey, e.getMessage());
            return false;
        }
    }

    @Override
    public void incrementStock(Long doctorId, String scheduleDate, String timeSlot) {
        String scheduleKey = buildScheduleKey(doctorId, scheduleDate, timeSlot);
        incrementStock(scheduleKey);
    }

    @Override
    public void incrementStock(String scheduleKey) {
        String stockKey = STOCK_KEY_PREFIX + scheduleKey;
        String maxKey = STOCK_INIT_PREFIX + scheduleKey;

        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(INCREMENT_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script,
                    java.util.Arrays.asList(stockKey, maxKey));

            if (result != null && result == 1) {
                log.debug("库存回补成功: scheduleKey={}", scheduleKey);
            } else {
                log.warn("库存回补失败（已达上限或未初始化）: scheduleKey={}", scheduleKey);
            }
        } catch (Exception e) {
            log.error("库存回补失败: scheduleKey={}, error={}", scheduleKey, e.getMessage());
        }
    }

    @Override
    public int getStock(Long doctorId, String scheduleDate, String timeSlot) {
        String scheduleKey = buildScheduleKey(doctorId, scheduleDate, timeSlot);
        return getStock(scheduleKey);
    }

    @Override
    public int getStock(String scheduleKey) {
        String stockKey = STOCK_KEY_PREFIX + scheduleKey;

        try {
            Object value = redisTemplate.opsForValue().get(stockKey);
            if (value == null) {
                return -1;
            }
            if (value instanceof Integer) {
                return (Integer) value;
            }
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            log.error("获取库存失败: scheduleKey={}, error={}", scheduleKey, e.getMessage());
            return -1;
        }
    }

    @Override
    public boolean isStockInitialized(String scheduleKey) {
        String initKey = STOCK_INIT_PREFIX + scheduleKey;
        return Boolean.TRUE.equals(redisTemplate.hasKey(initKey));
    }

    @Override
    public void deleteStock(String scheduleKey) {
        String stockKey = STOCK_KEY_PREFIX + scheduleKey;
        String initKey = STOCK_INIT_PREFIX + scheduleKey;

        redisTemplate.delete(stockKey);
        redisTemplate.delete(initKey);
        log.debug("库存缓存已删除: scheduleKey={}", scheduleKey);
    }

    /**
     * 构建排班标识Key
     */
    private String buildScheduleKey(Long doctorId, String scheduleDate, String timeSlot) {
        return doctorId + "_" + scheduleDate + "_" + timeSlot;
    }
}
