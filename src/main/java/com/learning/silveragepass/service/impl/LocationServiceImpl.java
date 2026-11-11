package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.location.LocationReportDTO;
import com.learning.silveragepass.dto.location.LocationRequestDTO;
import com.learning.silveragepass.dto.location.LocationResponseDTO;
import com.learning.silveragepass.entity.LocationRequest;
import com.learning.silveragepass.entity.LocationRequestStatus;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.exception.BadRequestException;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.repository.LocationRequestRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.service.LocationAuthorizationService;
import com.learning.silveragepass.service.LocationService;
import com.learning.silveragepass.websocket.WebSocketSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 位置服务实现类
 * Requirements: 3.2, 3.4, 3.6, 7.2, 7.3, 7.4
 */
@Slf4j
@Service
public class LocationServiceImpl implements LocationService {

    @Autowired
    private LocationRequestRepository locationRequestRepository;

    @Autowired
    private LocationAuthorizationService locationAuthorizationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebSocketSessionManager sessionManager;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 请求老人当前位置
     * Requirements: 3.2, 7.4
     */
    @Override
    @Transactional
    public LocationRequestDTO requestLocation(Long requesterId, Long elderId) {
        // 验证定位授权
        if (!locationAuthorizationService.hasValidAuthorization(requesterId, elderId)) {
            throw new BadRequestException("您没有获取该老人位置的授权");
        }

        // 验证老人用户存在
        User elder = userRepository.findById(elderId)
                .orElseThrow(() -> new ResourceNotFoundException("老人用户不存在"));

        // 检查老人是否在线
        boolean isOnline = sessionManager.isUserOnline(elderId);
        
        // 创建位置请求
        LocationRequest request = new LocationRequest();
        request.setRequestId(UUID.randomUUID().toString());
        request.setRequesterId(requesterId);
        request.setElderId(elderId);
        
        if (!isOnline) {
            // 老人离线，直接标记为OFFLINE
            request.setStatus(LocationRequestStatus.OFFLINE);
            request.setFailureReason("老人设备离线，无法获取位置");
            request.setResponseTime(LocalDateTime.now());
            locationRequestRepository.save(request);
            
            log.info("位置请求失败 - 老人离线: requestId={}, elderId={}", request.getRequestId(), elderId);
            return convertToDTO(request, elder);
        }

        request.setStatus(LocationRequestStatus.PENDING);
        LocationRequest savedRequest = locationRequestRepository.save(request);

        // 向老人App发送WebSocket消息
        sendLocationRequestToElder(savedRequest, requesterId);

        log.info("位置请求已发送: requestId={}, requesterId={}, elderId={}", 
                savedRequest.getRequestId(), requesterId, elderId);
        
        return convertToDTO(savedRequest, elder);
    }

    /**
     * 上报位置
     * Requirements: 7.2, 7.3
     */
    @Override
    @Transactional
    public LocationRequestDTO reportLocation(LocationReportDTO reportDTO) {
        // 查找请求
        LocationRequest request = locationRequestRepository.findByRequestId(reportDTO.getRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("位置请求不存在"));

        // 验证请求状态
        if (request.getStatus() != LocationRequestStatus.PENDING) {
            throw new BadRequestException("该位置请求已处理");
        }

        User elder = userRepository.findById(request.getElderId())
                .orElseThrow(() -> new ResourceNotFoundException("老人用户不存在"));

        // 更新请求状态
        request.setResponseTime(LocalDateTime.now());
        
        if (Boolean.TRUE.equals(reportDTO.getSuccess())) {
            // 定位成功
            request.setStatus(LocationRequestStatus.SUCCESS);
            request.setLatitude(reportDTO.getLatitude());
            request.setLongitude(reportDTO.getLongitude());
            request.setAddress(reportDTO.getAddress());
            
            log.info("位置上报成功: requestId={}, lat={}, lng={}", 
                    request.getRequestId(), reportDTO.getLatitude(), reportDTO.getLongitude());
        } else {
            // 定位失败
            request.setStatus(LocationRequestStatus.FAILED);
            request.setFailureReason(reportDTO.getErrorMessage());
            
            log.info("位置上报失败: requestId={}, errorCode={}, errorMessage={}", 
                    request.getRequestId(), reportDTO.getErrorCode(), reportDTO.getErrorMessage());
        }

        LocationRequest savedRequest = locationRequestRepository.save(request);

        // 向亲属推送位置结果
        sendLocationResponseToRequester(savedRequest, elder);

        return convertToDTO(savedRequest, elder);
    }

    /**
     * 获取位置请求状态
     * Requirements: 3.4
     */
    @Override
    public LocationRequestDTO getRequestStatus(String requestId) {
        LocationRequest request = locationRequestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("位置请求不存在"));

        User elder = userRepository.findById(request.getElderId())
                .orElseThrow(() -> new ResourceNotFoundException("老人用户不存在"));

        return convertToDTO(request, elder);
    }

    /**
     * 处理超时请求
     * Requirements: 3.6
     */
    @Override
    @Transactional
    public void processTimeoutRequests() {
        LocalDateTime now = LocalDateTime.now();
        List<LocationRequest> timeoutRequests = locationRequestRepository
                .findByStatusAndExpireTimeBefore(LocationRequestStatus.PENDING, now);

        for (LocationRequest request : timeoutRequests) {
            request.setStatus(LocationRequestStatus.TIMEOUT);
            request.setFailureReason("获取位置超时，请稍后重试");
            request.setResponseTime(now);
            locationRequestRepository.save(request);

            // 向亲属推送超时通知
            User elder = userRepository.findById(request.getElderId()).orElse(null);
            if (elder != null) {
                sendLocationResponseToRequester(request, elder);
            }

            log.info("位置请求超时: requestId={}", request.getRequestId());
        }

        if (!timeoutRequests.isEmpty()) {
            log.info("处理了 {} 个超时的位置请求", timeoutRequests.size());
        }
    }

    /**
     * 向老人App发送位置请求
     */
    private void sendLocationRequestToElder(LocationRequest request, Long requesterId) {
        User requester = userRepository.findById(requesterId).orElse(null);
        String requesterName = requester != null ? 
                (requester.getRealName() != null ? requester.getRealName() : requester.getUsername()) : "未知";

        Map<String, Object> payload = new HashMap<>();
        payload.put("requestId", request.getRequestId());
        payload.put("requesterId", requesterId);
        payload.put("requesterName", requesterName);
        payload.put("timestamp", System.currentTimeMillis());

        Map<String, Object> message = new HashMap<>();
        message.put("type", "location_request");
        message.put("payload", payload);

        messagingTemplate.convertAndSendToUser(
                String.valueOf(request.getElderId()),
                "/queue/notifications",
                message
        );

        log.debug("已向老人 {} 发送位置请求消息", request.getElderId());
    }

    /**
     * 向亲属推送位置结果
     */
    private void sendLocationResponseToRequester(LocationRequest request, User elder) {
        String elderName = elder.getRealName() != null ? elder.getRealName() : elder.getUsername();

        LocationResponseDTO response = LocationResponseDTO.builder()
                .requestId(request.getRequestId())
                .elderId(request.getElderId())
                .elderName(elderName)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .address(request.getAddress())
                .status(request.getStatus().name())
                .failureReason(request.getFailureReason())
                .timestamp(request.getResponseTime())
                .build();

        String messageType = request.getStatus() == LocationRequestStatus.SUCCESS ? 
                "location_response" : "location_failed";

        Map<String, Object> message = new HashMap<>();
        message.put("type", messageType);
        message.put("payload", response);

        messagingTemplate.convertAndSendToUser(
                String.valueOf(request.getRequesterId()),
                "/queue/notifications",
                message
        );

        log.debug("已向亲属 {} 推送位置结果: status={}", request.getRequesterId(), request.getStatus());
    }

    /**
     * 转换为DTO
     */
    private LocationRequestDTO convertToDTO(LocationRequest request, User elder) {
        LocationRequestDTO dto = new LocationRequestDTO();
        dto.setRequestId(request.getRequestId());
        dto.setElderId(request.getElderId());
        dto.setElderName(elder.getRealName() != null ? elder.getRealName() : elder.getUsername());
        dto.setStatus(request.getStatus());
        dto.setLatitude(request.getLatitude());
        dto.setLongitude(request.getLongitude());
        dto.setAddress(request.getAddress());
        dto.setFailureReason(request.getFailureReason());
        dto.setRequestTime(request.getRequestTime());
        dto.setResponseTime(request.getResponseTime());
        return dto;
    }
}
