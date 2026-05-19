package com.hospitalinfo.hospitalinformationsystem.service;

/**
 * 排班号源库存服务接口
 * 管理Redis中的号源库存缓存，实现预扣减和库存回补
 */
public interface IScheduleStockService {

    /**
     * 初始化号源库存到Redis（每天定时任务调用）
     * @param doctorId 医生ID
     * @param scheduleDate 日期
     * @param timeSlot 时段
     * @param maxPatients 最大号源数
     */
    void initStock(Long doctorId, String scheduleDate, String timeSlot, int maxPatients);

    /**
     * 批量初始化号源库存
     * @param scheduleKey 排班标识 (doctorId_date_timeslot)
     * @param maxPatients 最大号源数
     */
    void initStockBatch(String scheduleKey, int maxPatients);

    /**
     * Redis预扣减库存
     * @param doctorId 医生ID
     * @param scheduleDate 日期
     * @param timeSlot 时段
     * @return 预扣减是否成功
     */
    boolean preDecrementStock(Long doctorId, String scheduleDate, String timeSlot);

    /**
     * Redis预扣减库存（通过排班标识）
     * @param scheduleKey 排班标识 (doctorId_date_timeslot)
     * @return 预扣减是否成功
     */
    boolean preDecrementStock(String scheduleKey);

    /**
     * 取消预约时回补库存
     * @param doctorId 医生ID
     * @param scheduleDate 日期
     * @param timeSlot 时段
     */
    void incrementStock(Long doctorId, String scheduleDate, String timeSlot);

    /**
     * 取消预约时回补库存（通过排班标识）
     * @param scheduleKey 排班标识 (doctorId_date_timeslot)
     */
    void incrementStock(String scheduleKey);

    /**
     * 获取当前库存
     * @param doctorId 医生ID
     * @param scheduleDate 日期
     * @param timeSlot 时段
     * @return 剩余号源数，-1表示未初始化
     */
    int getStock(Long doctorId, String scheduleDate, String timeSlot);

    /**
     * 获取当前库存（通过排班标识）
     * @param scheduleKey 排班标识
     * @return 剩余号源数，-1表示未初始化
     */
    int getStock(String scheduleKey);

    /**
     * 检查库存是否已初始化
     * @param scheduleKey 排班标识
     * @return 是否已初始化
     */
    boolean isStockInitialized(String scheduleKey);

    /**
     * 删除库存缓存
     * @param scheduleKey 排班标识
     */
    void deleteStock(String scheduleKey);
}
