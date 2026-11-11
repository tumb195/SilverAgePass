package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.EmergencyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 紧急联系人数据访问接口
 */
@Repository
public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {

    /**
     * 根据用户ID查找紧急联系人
     * @param userId 用户ID
     * @return 紧急联系人（如果存在）
     */
    Optional<EmergencyContact> findByUserId(Long userId);

    /**
     * 检查用户是否已有紧急联系人
     * @param userId 用户ID
     * @return 是否存在
     */
    boolean existsByUserId(Long userId);
}
