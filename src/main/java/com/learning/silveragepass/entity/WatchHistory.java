package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 观看历史实体
 * 记录用户观看视频的历史记录，包括观看位置和次数
 */
@Data
@Entity
@Table(name = "watch_history",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "video_id"}))
@NoArgsConstructor
@AllArgsConstructor
public class WatchHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 观看用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * 观看的视频
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;
    
    /**
     * 上次观看位置（秒）
     */
    @Column(name = "last_position")
    private Integer lastPosition = 0;
    
    /**
     * 观看次数
     */
    @Column(name = "watch_count")
    private Integer watchCount = 1;
    
    /**
     * 首次观看时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "first_watch_time")
    private LocalDateTime firstWatchTime;
    
    /**
     * 最后观看时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "last_watch_time")
    private LocalDateTime lastWatchTime;
    
    /**
     * 创建时设置默认值
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.firstWatchTime = now;
        this.lastWatchTime = now;
        if (this.lastPosition == null) {
            this.lastPosition = 0;
        }
        if (this.watchCount == null) {
            this.watchCount = 1;
        }
    }
    
    /**
     * 更新时自动更新最后观看时间
     */
    @PreUpdate
    public void preUpdate() {
        this.lastWatchTime = LocalDateTime.now();
    }
    
    /**
     * 增加观看次数
     */
    public void incrementWatchCount() {
        this.watchCount = (this.watchCount == null ? 0 : this.watchCount) + 1;
    }
    
    /**
     * 更新观看位置
     * @param position 新的观看位置（秒）
     */
    public void updatePosition(Integer position) {
        if (position != null && position >= 0) {
            this.lastPosition = position;
        }
    }
    
    /**
     * 便捷构造函数
     * @param user 用户
     * @param video 视频
     */
    public WatchHistory(User user, Video video) {
        this.user = user;
        this.video = video;
    }
}
