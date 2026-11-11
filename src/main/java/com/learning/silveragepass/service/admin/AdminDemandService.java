package com.learning.silveragepass.service.admin;

import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.admin.DemandListDTO;
import com.learning.silveragepass.dto.admin.VolunteerRankingDTO;
import com.learning.silveragepass.entity.Demand;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.repository.DemandRepository;
import com.learning.silveragepass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDemandService {
    
    private final DemandRepository demandRepository;
    private final UserRepository userRepository;
    
    /**
     * 获取任务列表
     */
    public PageResponse<DemandListDTO> getDemandList(int page, int pageSize, String search, String status, String type) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Demand> demandPage = demandRepository.findAll(pageable);
        
        List<DemandListDTO> content = demandPage.getContent().stream()
                .map(this::convertToDemandListDTO)
                .collect(Collectors.toList());
        
        return new PageResponse<>(
                content,
                demandPage.getTotalElements(),
                demandPage.getTotalPages(),
                demandPage.getNumber(),
                demandPage.getSize(),
                demandPage.isFirst(),
                demandPage.isLast(),
                demandPage.isEmpty()
        );
    }
    
    /**
     * 获取任务详情
     */
    public DemandListDTO getDemandDetail(Long id) {
        Demand demand = demandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("任务不存在"));
        return convertToDemandListDTO(demand);
    }
    
    /**
     * 获取今日任务总数
     */
    public Long getTodayTaskCount() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        return demandRepository.countByCreateTimeBetween(todayStart, todayEnd);
    }
    
    /**
     * 获取今日进行中的任务数
     */
    public Long getTodayInProgressCount() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        return demandRepository.countByStatusAndCreateTimeBetween(
                com.learning.silveragepass.entity.DemandStatus.IN_PROGRESS, 
                todayStart, 
                todayEnd
        );
    }
    
    /**
     * 获取今日已完成的任务数
     */
    public Long getTodayCompletedCount() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        return demandRepository.countByStatusAndCreateTimeBetween(
                com.learning.silveragepass.entity.DemandStatus.COMPLETED, 
                todayStart, 
                todayEnd
        );
    }
    
    /**
     * 获取志愿者活跃度排名（一周内）
     */
    public List<VolunteerRankingDTO> getVolunteerRanking() {
        // 计算一周前的时间
        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
        
        // 查询一周内的志愿者服务次数
        List<Object[]> results = demandRepository.findVolunteerRankingByWeek(weekStart);
        
        // 转换为DTO并添加排名
        List<VolunteerRankingDTO> rankings = new java.util.ArrayList<>();
        int ranking = 1;
        
        for (Object[] result : results) {
            Long volunteerId = (Long) result[0];
            Long serviceCount = (Long) result[1];
            
            // 获取志愿者信息
            userRepository.findById(volunteerId).ifPresent(user -> {
                VolunteerRankingDTO dto = new VolunteerRankingDTO();
                dto.setUserId(user.getId());
                dto.setUsername(user.getUsername());
                dto.setWarmCoin(user.getWarmCoin());
                dto.setServiceCount(serviceCount);
                dto.setRanking(ranking);
                rankings.add(dto);
            });
        }
        
        // 更新排名（考虑服务次数相同的情况）
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRanking(i + 1);
        }
        
        return rankings;
    }
    
    private DemandListDTO convertToDemandListDTO(Demand demand) {
        DemandListDTO dto = new DemandListDTO();
        
        try {
            // 设置基本信息，增加null检查
            dto.setId(demand.getId());
            dto.setTitle(demand.getTitle() != null ? demand.getTitle() : "未命名任务");
            dto.setPublishTime(demand.getCreateTime());
            dto.setWarmCoin(demand.getWarmCoinReward() != null ? demand.getWarmCoinReward() : 0);
            
            // 状态处理
            if (demand.getStatus() != null) {
                dto.setStatus(getStatusDescription(demand.getStatus()));
                dto.setStatusCode(demand.getStatus().name());
            } else {
                dto.setStatus("未知");
                dto.setStatusCode("UNKNOWN");
            }
            
            dto.setType(demand.getType() != null ? demand.getType().name() : null);
            dto.setServiceAddress(demand.getServiceAddress() != null ? demand.getServiceAddress() : "未设置");
            dto.setAcceptTime(demand.getAcceptTime());
            dto.setCompleteTime(demand.getEndTime());
            
            // 获取发布者信息，设置默认值
            dto.setPublisherName("未知用户");
            dto.setPublisherPhone("未知");
            if (demand.getPublisherId() != null) {
                try {
                    userRepository.findById(demand.getPublisherId()).ifPresent(user -> {
                        dto.setPublisherName(user.getUsername() != null ? user.getUsername() : "未知用户");
                        dto.setPublisherPhone(user.getPhone() != null ? user.getPhone() : "未知");
                    });
                } catch (Exception e) {
                    // 查询用户失败，使用默认值
                    System.err.println("查询发布者失败，Demand ID: " + demand.getId() + ", Publisher ID: " + demand.getPublisherId() + ", 错误: " + e.getMessage());
                }
            }
            
            // 获取志愿者信息，设置默认值
            dto.setVolunteerName("暂无");
            if (demand.getVolunteerId() != null) {
                try {
                    userRepository.findById(demand.getVolunteerId()).ifPresent(user -> {
                        dto.setVolunteerName(user.getUsername() != null ? user.getUsername() : "未知志愿者");
                    });
                } catch (Exception e) {
                    // 查询志愿者失败，使用默认值
                    System.err.println("查询志愿者失败，Demand ID: " + demand.getId() + ", Volunteer ID: " + demand.getVolunteerId() + ", 错误: " + e.getMessage());
                    dto.setVolunteerName("暂无");
                }
            }
            
        } catch (Exception e) {
            // 如果整个转换过程出错，记录错误并返回一个最小化的DTO
            System.err.println("转换Demand到DTO失败，Demand ID: " + demand.getId() + ", 错误: " + e.getMessage());
            e.printStackTrace();
            
            // 设置最基本的字段，避免返回null
            dto.setId(demand.getId());
            dto.setTitle("数据异常");
            dto.setPublisherName("未知");
            dto.setPublisherPhone("未知");
            dto.setVolunteerName("未知");
            dto.setStatus("数据异常");
            dto.setStatusCode("ERROR");
            dto.setWarmCoin(0);
            dto.setServiceAddress("未知");
        }
        
        return dto;
    }
    
    private String getStatusDescription(com.learning.silveragepass.entity.DemandStatus status) {
        return switch (status) {
            case WAITING -> "待接单";
            case ACCEPTED -> "已接单";
            case IN_PROGRESS -> "进行中";
            case COMPLETED -> "已完成";
            case CANCELLED -> "已取消";
        };
    }
}
