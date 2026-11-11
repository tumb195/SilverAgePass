package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.FollowupReminder;
import com.learning.silveragepass.entity.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 复诊提醒数据访问层
 */
@Repository
public interface FollowupReminderRepository extends JpaRepository<FollowupReminder, Long> {

    /**
     * 查询指定用户的所有复诊提醒，按复诊日期和时间升序排列
     * Requirements: 2.1
     */
    List<FollowupReminder> findByUserIdOrderByAppointmentDateAscAppointmentTimeAsc(Long userId);

    /**
     * 查询指定用户指定状态的复诊提醒
     * Requirements: 2.2
     */
    List<FollowupReminder> findByUserIdAndStatus(Long userId, ReminderStatus status);

    /**
     * 查询指定用户指定状态且复诊日期在指定范围内的复诊提醒
     * 用于获取即将到来的复诊提醒（7天内）
     * Requirements: 4.1
     */
    List<FollowupReminder> findByUserIdAndStatusAndAppointmentDateBetween(
            Long userId, 
            ReminderStatus status, 
            LocalDate startDate, 
            LocalDate endDate
    );
}
