package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.MedicationReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用药提醒数据访问层
 */
@Repository
public interface MedicationReminderRepository extends JpaRepository<MedicationReminder, Long> {

    /**
     * 查询指定用户的所有活跃用药提醒
     */
    List<MedicationReminder> findByUserIdAndActiveTrue(Long userId);

    /**
     * 查询指定用户的所有用药提醒（包括已停用的）
     */
    List<MedicationReminder> findByUserId(Long userId);
}
