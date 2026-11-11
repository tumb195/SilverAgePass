package com.learning.silveragepass.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 公益池实体类
 */
@Data
@Entity
@Table(name = "charity_pool")
public class CharityPool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer balance = 0;  // 当前余额

    @Column(name = "total_donated", nullable = false)
    private Integer totalDonated = 0;  // 累计捐赠总额

    @Column(name = "total_distributed", nullable = false)
    private Integer totalDistributed = 0;  // 累计发放总额

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}
