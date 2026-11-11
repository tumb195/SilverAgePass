package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.CharityPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 公益池数据访问接口
 */
@Repository
public interface CharityPoolRepository extends JpaRepository<CharityPool, Long> {

    /**
     * 获取公益池（只有一条记录，id=1）
     */
    default Optional<CharityPool> getPool() {
        return findById(1L);
    }

    /**
     * 增加公益池余额（捐赠时调用）
     */
    @Modifying
    @Query("UPDATE CharityPool c SET c.balance = c.balance + :amount, " +
           "c.totalDonated = c.totalDonated + :amount, c.updateTime = CURRENT_TIMESTAMP WHERE c.id = 1")
    int addDonation(@Param("amount") Integer amount);

    /**
     * 减少公益池余额（发放时调用）
     */
    @Modifying
    @Query("UPDATE CharityPool c SET c.balance = c.balance - :amount, " +
           "c.totalDistributed = c.totalDistributed + :amount, c.updateTime = CURRENT_TIMESTAMP " +
           "WHERE c.id = 1 AND c.balance >= :amount")
    int distribute(@Param("amount") Integer amount);

    /**
     * 获取当前余额
     */
    @Query("SELECT c.balance FROM CharityPool c WHERE c.id = 1")
    Integer getCurrentBalance();
}
