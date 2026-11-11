package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.location.LocationReportDTO;
import com.learning.silveragepass.dto.location.LocationRequestDTO;

/**
 * 位置服务接口
 * 处理位置请求和上报
 * Requirements: 3.2, 3.4, 3.6, 7.2, 7.3
 */
public interface LocationService {

    /**
     * 请求老人当前位置
     * 向老人App发送WebSocket消息请求位置
     * Requirements: 3.2
     *
     * @param requesterId 请求者ID（亲属）
     * @param elderId     老人用户ID
     * @return 位置请求信息
     */
    LocationRequestDTO requestLocation(Long requesterId, Long elderId);

    /**
     * 上报位置
     * 老人App上报位置信息
     * Requirements: 7.2, 7.3
     *
     * @param reportDTO 位置上报信息
     * @return 处理结果
     */
    LocationRequestDTO reportLocation(LocationReportDTO reportDTO);

    /**
     * 获取位置请求状态
     * Requirements: 3.4
     *
     * @param requestId 请求ID
     * @return 位置请求信息
     */
    LocationRequestDTO getRequestStatus(String requestId);

    /**
     * 处理超时请求
     * 将超时的PENDING请求标记为TIMEOUT
     * Requirements: 3.6
     */
    void processTimeoutRequests();
}
