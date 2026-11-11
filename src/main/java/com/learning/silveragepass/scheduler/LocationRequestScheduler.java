package com.learning.silveragepass.scheduler;

import com.learning.silveragepass.service.LocationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 位置请求超时调度任务
 * 定期检查并处理超时的位置请求
 * Requirements: 3.6
 */
@Slf4j
@Component
public class LocationRequestScheduler {

    @Autowired
    private LocationService locationService;

    /**
     * 每10秒检查一次超时的位置请求
     * 将超时的PENDING请求标记为TIMEOUT并通知亲属
     */
    @Scheduled(fixedRate = 10000)
    public void processTimeoutRequests() {
        try {
            locationService.processTimeoutRequests();
        } catch (Exception e) {
            log.error("处理超时位置请求时发生错误", e);
        }
    }
}
