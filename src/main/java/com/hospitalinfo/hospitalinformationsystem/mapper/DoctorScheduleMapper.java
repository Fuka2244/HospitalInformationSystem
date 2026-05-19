package com.hospitalinfo.hospitalinformationsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hospitalinfo.hospitalinformationsystem.entity.DoctorSchedule;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface DoctorScheduleMapper extends BaseMapper<DoctorSchedule> {

    /**
     * 原子性递增已预约数（防止并发超卖）
     * @param doctorId 医生ID
     * @param scheduleDate 排班日期
     * @param timeSlot 时段
     * @return 影响行数，0表示已约满或排班不存在
     */
    @Update("UPDATE doctor_schedule SET booked_count = booked_count + 1 " +
            "WHERE doctor_id = #{doctorId} AND schedule_date = #{scheduleDate} " +
            "AND time_slot = #{timeSlot} AND booked_count < max_patients AND status = 1")
    int incrementBookedCount(@Param("doctorId") Long doctorId,
                             @Param("scheduleDate") java.time.LocalDate scheduleDate,
                             @Param("timeSlot") String timeSlot);

    /**
     * 原子性递增已预约数（带乐观锁版本控制）
     * @param doctorId 医生ID
     * @param scheduleDate 排班日期
     * @param timeSlot 时段
     * @param currentVersion 当前版本号
     * @return 影响行数，0表示版本冲突或已约满
     */
    @Update("UPDATE doctor_schedule SET booked_count = booked_count + 1, version = version + 1 " +
            "WHERE doctor_id = #{doctorId} AND schedule_date = #{scheduleDate} " +
            "AND time_slot = #{timeSlot} AND version = #{currentVersion} " +
            "AND booked_count < max_patients AND status = 1")
    int incrementBookedCountWithVersion(@Param("doctorId") Long doctorId,
                                        @Param("scheduleDate") java.time.LocalDate scheduleDate,
                                        @Param("timeSlot") String timeSlot,
                                        @Param("currentVersion") Integer currentVersion);

    /**
     * 原子性递减已预约数（防止并发计数不一致）
     * @param doctorId 医生ID
     * @param scheduleDate 排班日期
     * @param timeSlot 时段
     * @return 影响行数
     */
    @Update("UPDATE doctor_schedule SET booked_count = booked_count - 1 " +
            "WHERE doctor_id = #{doctorId} AND schedule_date = #{scheduleDate} " +
            "AND time_slot = #{timeSlot} AND booked_count > 0")
    int decrementBookedCount(@Param("doctorId") Long doctorId,
                             @Param("scheduleDate") java.time.LocalDate scheduleDate,
                             @Param("timeSlot") String timeSlot);

    /**
     * 原子性递减已预约数（带乐观锁版本控制）
     * @param doctorId 医生ID
     * @param scheduleDate 排班日期
     * @param timeSlot 时段
     * @param currentVersion 当前版本号
     * @return 影响行数，0表示版本冲突
     */
    @Update("UPDATE doctor_schedule SET booked_count = booked_count - 1, version = version + 1 " +
            "WHERE doctor_id = #{doctorId} AND schedule_date = #{scheduleDate} " +
            "AND time_slot = #{timeSlot} AND version = #{currentVersion} AND booked_count > 0")
    int decrementBookedCountWithVersion(@Param("doctorId") Long doctorId,
                                         @Param("scheduleDate") java.time.LocalDate scheduleDate,
                                         @Param("timeSlot") String timeSlot,
                                         @Param("currentVersion") Integer currentVersion);

    /**
     * 查询排班信息（包含版本号）
     * @param doctorId 医生ID
     * @param scheduleDate 排班日期
     * @param timeSlot 时段
     * @return 排班信息
     */
    @Select("SELECT id, doctor_id, schedule_date, time_slot, max_patients, booked_count, status, version " +
            "FROM doctor_schedule " +
            "WHERE doctor_id = #{doctorId} AND schedule_date = #{scheduleDate} AND time_slot = #{timeSlot} " +
            "LIMIT 1")
    DoctorSchedule selectScheduleWithVersion(@Param("doctorId") Long doctorId,
                                            @Param("scheduleDate") java.time.LocalDate scheduleDate,
                                            @Param("timeSlot") String timeSlot);
}
