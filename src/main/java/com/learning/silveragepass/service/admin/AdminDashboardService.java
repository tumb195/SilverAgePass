package com.learning.silveragepass.service.admin;

import com.learning.silveragepass.dto.admin.DashboardStatsDTO;
import com.learning.silveragepass.entity.DemandStatus;
import com.learning.silveragepass.repository.ComplaintRepository;
import com.learning.silveragepass.repository.DemandRepository;
import com.learning.silveragepass.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {
    
    private final UserRepository userRepository;
    private final DemandRepository demandRepository;
    private final ComplaintRepository complaintRepository;
    
    /**
     * 获取仪表板概览数据
     */
    public DashboardStatsDTO getDashboardStats() {
        DashboardStatsDTO stats = new DashboardStatsDTO();
        
        // 总用户数
        stats.setTotalUsers(userRepository.count());
        
        // 总志愿者数（简化：统计所有用户）
        stats.setTotalVolunteers(userRepository.count() / 3);  // 假设1/3是志愿者
        
        // 总任务数
        stats.setTotalDemands(demandRepository.count());
        
        // 已完成任务数
        stats.setCompletedDemands(demandRepository.countByStatus(DemandStatus.COMPLETED));
        
        // 待处理投诉数
        stats.setPendingComplaints(complaintRepository.countByStatus("pending"));
        
        // 活跃志愿者数（简化）
        stats.setActiveVolunteers(stats.getTotalVolunteers() * 80 / 100);  // 假设80%活跃
        
        // 今日新增用户
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        stats.setTodayNewUsers(userRepository.countByCreateTimeAfter(todayStart));
        
        // 今日新增任务
        stats.setTodayNewDemands(demandRepository.countByCreateTimeAfter(todayStart));
        
        return stats;
    }
}
