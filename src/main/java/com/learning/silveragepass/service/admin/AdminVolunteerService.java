package com.learning.silveragepass.service.admin;

import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.admin.ServiceTrendDTO;
import com.learning.silveragepass.dto.admin.VolunteerListDTO;
import com.learning.silveragepass.dto.admin.VolunteerRankingDTO;
import com.learning.silveragepass.entity.DemandStatus;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminVolunteerService {

    private final UserRepository userRepository;
    private final DemandRepository demandRepository;

    /**
     * 获取志愿者列表
     */
    public PageResponse<VolunteerListDTO> getVolunteerList(int page, int pageSize, String search) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));

        Page<User> userPage;
        if (search != null && !search.trim().isEmpty()) {
            userPage = userRepository.searchByKeyword(search, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        // 过滤出志愿者（通过角色判断）
        List<VolunteerListDTO> volunteers = userPage.getContent().stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName().equals("ROLE_VOLUNTEER")))
                .map(this::convertToVolunteerDTO)
                .collect(Collectors.toList());

        return new PageResponse<>(
                volunteers,
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                page - 1,
                pageSize,
                userPage.isFirst(),
                userPage.isLast(),
                userPage.isEmpty()
        );
    }

    /**
     * 获取志愿者详情
     */
    public VolunteerListDTO getVolunteerDetail(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("志愿者不存在"));

        return convertToVolunteerDTO(user);
    }

    /**
     * 获取志愿者排名
     */
    public List<VolunteerRankingDTO> getVolunteerRanking() {
        List<User> allUsers = userRepository.findAll();

        // 过滤出志愿者并按服务次数排序，取前10名
        List<VolunteerRankingDTO> rankings = allUsers.stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName().equals("ROLE_VOLUNTEER")))
                .map(user -> {
                    long serviceCount = countByVolunteerIdAndStatus(user.getId(), DemandStatus.COMPLETED);
                    return new VolunteerRankingDTO(
                            user.getId(),
                            user.getUsername(),
                            user.getWarmCoin(),
                            serviceCount,
                            0  // ranking will be set later
                    );
                })
                .sorted(Comparator.comparing(VolunteerRankingDTO::getServiceCount).reversed())
                .limit(10)
                .collect(Collectors.toList());
        
        // 设置排名
        for (int i = 0; i < rankings.size(); i++) {
            rankings.get(i).setRanking(i + 1);
        }
        
        return rankings;
    }

    /**
     * 获取本周服务量趋势
     * 只统计从本周一到今天的数据，不包含未来日期
     */
    public List<ServiceTrendDTO> getWeeklyServiceVolume() {
        List<ServiceTrendDTO> trends = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 获取本周一
        LocalDate monday = today.with(DayOfWeek.MONDAY);

        // 只统计从周一到今天的数据（不包含未来日期）
        LocalDate currentDate = monday;
        while (!currentDate.isAfter(today)) {
            // 从数据库实际查询该日期的任务数据
            long waitingCount = demandRepository.countByServiceDateAndStatus(currentDate, DemandStatus.WAITING);
            long acceptedCount = demandRepository.countByServiceDateAndStatus(currentDate, DemandStatus.ACCEPTED);
            long inProgressCount = demandRepository.countByServiceDateAndStatus(currentDate, DemandStatus.IN_PROGRESS);
            long completedCount = demandRepository.countByServiceDateAndStatus(currentDate, DemandStatus.COMPLETED);
            
            // 总数包含所有状态（不包括已取消的）
            long totalCount = waitingCount + acceptedCount + inProgressCount + completedCount;

            String dayOfWeek = getDayOfWeekChinese(currentDate.getDayOfWeek());
            String dateStr = currentDate.format(DateTimeFormatter.ofPattern("MM-dd"));

            trends.add(new ServiceTrendDTO(
                    dateStr,
                    dayOfWeek,
                    totalCount,
                    completedCount,
                    inProgressCount,
                    waitingCount
            ));
            
            currentDate = currentDate.plusDays(1);
        }

        return trends;
    }

    /**
     * 转换User为VolunteerListDTO
     */
    private VolunteerListDTO convertToVolunteerDTO(User user) {
        VolunteerListDTO dto = new VolunteerListDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setPhone(user.getPhone());
        dto.setRealName(user.getRealName());
        dto.setAge(user.getAge());
        dto.setAddress(user.getAddress());
        dto.setWarmCoin(user.getWarmCoin());

        // 统计任务数据
        long totalTasks = countByVolunteerId(user.getId());
        long completedTasks = countByVolunteerIdAndStatus(user.getId(), DemandStatus.COMPLETED);
        long inProgressTasks = countByVolunteerIdAndStatus(user.getId(), DemandStatus.IN_PROGRESS);
        long cancelledTasks = countByVolunteerIdAndStatus(user.getId(), DemandStatus.CANCELLED);

        dto.setTotalTasks(totalTasks);
        dto.setCompletedTasks(completedTasks);
        dto.setInProgressTasks(inProgressTasks);
        dto.setCancelledTasks(cancelledTasks);

        // 计算评分（简化处理，实际应该从评价表计算）
        double rating = completedTasks > 0 ? 4.5 : 0.0;
        dto.setRating(rating);

        // 计算信用分（基于完成任务数）
        int creditScore = (int) (completedTasks * 100);
        dto.setCreditScore(creditScore > 0 ? creditScore : 800);
        dto.setLastLoginTime(user.getLastLoginTime());
        dto.setRegisterTime(user.getCreateTime());
        dto.setEnabled(user.getEnabled());

        // 判断活跃状态
        String status = "活跃";
        if (user.getLastLoginTime() != null) {
            long daysSinceLogin = ChronoUnit.DAYS.between(user.getLastLoginTime().toLocalDate(), LocalDate.now());
            if (daysSinceLogin > 30) {
                status = "不活跃";
            }
        }
        dto.setStatus(status);

        return dto;
    }

    /**
     * 获取星期几的中文
     */
    private String getDayOfWeekChinese(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
    }

    private long countByVolunteerId(Long userId) {
        return demandRepository.findByVolunteerId(userId, Pageable.unpaged()).getTotalElements();
    }

    private long countByVolunteerIdAndStatus(Long userId, DemandStatus status) {
        return demandRepository.findByVolunteerIdAndStatus(userId, status, Pageable.unpaged()).getTotalElements();
    }
}
