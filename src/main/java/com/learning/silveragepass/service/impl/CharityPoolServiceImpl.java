package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.CharityPoolDTO;
import com.learning.silveragepass.dto.DonateResponseDTO;
import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.admin.CharityPoolStatsDTO;
import com.learning.silveragepass.dto.admin.DonationQueryDTO;
import com.learning.silveragepass.dto.admin.DonationRecordDTO;
import com.learning.silveragepass.entity.CharityPool;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.entity.WarmCoinRecord;
import com.learning.silveragepass.exception.BusinessException;
import com.learning.silveragepass.repository.CharityPoolRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.repository.WarmCoinRecordRepository;
import com.learning.silveragepass.service.CharityPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 公益池服务实现类
 */
@Service
public class CharityPoolServiceImpl implements CharityPoolService {

    private static final Logger logger = LoggerFactory.getLogger(CharityPoolServiceImpl.class);

    private final CharityPoolRepository charityPoolRepository;
    private final UserRepository userRepository;
    private final WarmCoinRecordRepository warmCoinRecordRepository;

    @Autowired
    public CharityPoolServiceImpl(CharityPoolRepository charityPoolRepository,
                                   UserRepository userRepository,
                                   WarmCoinRecordRepository warmCoinRecordRepository) {
        this.charityPoolRepository = charityPoolRepository;
        this.userRepository = userRepository;
        this.warmCoinRecordRepository = warmCoinRecordRepository;
    }

    @Override
    public CharityPoolDTO getPoolInfo() {
        CharityPool pool = charityPoolRepository.getPool()
                .orElseThrow(() -> new BusinessException("POOL_NOT_FOUND", "公益池未初始化"));
        
        return new CharityPoolDTO(
                pool.getBalance(),
                pool.getTotalDonated(),
                pool.getTotalDistributed(),
                pool.getUpdateTime()
        );
    }

    @Override
    @Transactional
    public DonateResponseDTO donateToPool(Long userId, Integer amount, String message) {
        // 1. 参数校验
        if (amount == null || amount <= 0) {
            throw new BusinessException("INVALID_AMOUNT", "捐赠金额必须大于0");
        }

        // 2. 查询用户并验证余额
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "用户不存在"));

        Integer currentBalance = user.getWarmCoin();
        if (currentBalance == null) {
            currentBalance = 0;
        }

        if (currentBalance < amount) {
            throw new BusinessException("INSUFFICIENT_BALANCE", 
                    "暖龄币余额不足，当前余额: " + currentBalance + "，需要: " + amount);
        }

        // 3. 扣减用户暖龄币
        user.setWarmCoin(currentBalance - amount);
        userRepository.save(user);

        // 4. 增加公益池余额
        int updated = charityPoolRepository.addDonation(amount);
        if (updated == 0) {
            throw new BusinessException("POOL_NOT_FOUND", "公益池未初始化，请联系管理员");
        }

        // 5. 创建暖龄币流水记录
        WarmCoinRecord record = new WarmCoinRecord();
        record.setUserId(userId);
        record.setAmount(-amount);  // 负数表示支出
        record.setType(WarmCoinRecord.RecordType.DONATE);
        record.setDescription(message != null && !message.isEmpty() 
                ? "捐赠公益池: " + message 
                : "捐赠公益池");
        warmCoinRecordRepository.save(record);

        // 6. 获取公益池当前余额
        Integer poolBalance = charityPoolRepository.getCurrentBalance();

        logger.info("用户 {} 向公益池捐赠 {} 暖龄币，留言: {}", userId, amount, message);

        // 7. 返回结果
        return new DonateResponseDTO(
                amount,
                user.getWarmCoin(),
                poolBalance,
                "感谢您的爱心捐赠！您已成功捐赠 " + amount + " 个暖龄币到公益池"
        );
    }

    @Override
    @Transactional
    public void distribute(Long targetUserId, Integer amount, Long operatorId, String description) {
        // 1. 参数校验
        if (amount == null || amount <= 0) {
            throw new BusinessException("INVALID_AMOUNT", "发放金额必须大于0");
        }

        // 2. 查询目标用户
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "目标用户不存在"));

        // 3. 扣减公益池余额
        int updated = charityPoolRepository.distribute(amount);
        if (updated == 0) {
            Integer currentBalance = charityPoolRepository.getCurrentBalance();
            if (currentBalance == null) {
                throw new BusinessException("POOL_NOT_FOUND", "公益池未初始化，请联系管理员");
            }
            throw new BusinessException("INSUFFICIENT_BALANCE", 
                    "公益池余额不足，当前余额: " + currentBalance + "，需要: " + amount);
        }

        // 4. 增加目标用户的暖龄币
        Integer currentUserBalance = targetUser.getWarmCoin();
        if (currentUserBalance == null) {
            currentUserBalance = 0;
        }
        targetUser.setWarmCoin(currentUserBalance + amount);
        userRepository.save(targetUser);

        // 5. 创建暖龄币流水记录
        WarmCoinRecord record = new WarmCoinRecord();
        record.setUserId(targetUserId);
        record.setAmount(amount);  // 正数表示收入
        record.setType(WarmCoinRecord.RecordType.CHARITY_DISTRIBUTION);
        record.setDescription("公益池发放: " + description);
        warmCoinRecordRepository.save(record);

        logger.info("管理员 {} 从公益池向用户 {} 发放 {} 暖龄币: {}", 
                operatorId, targetUserId, amount, description);
    }

    @Override
    public Integer getCurrentBalance() {
        Integer balance = charityPoolRepository.getCurrentBalance();
        return balance != null ? balance : 0;
    }
    
    @Override
    public CharityPoolStatsDTO getAdminStats() {
        // 获取公益池基本信息
        CharityPool pool = charityPoolRepository.getPool()
                .orElseThrow(() -> new BusinessException("POOL_NOT_FOUND", "公益池未初始化"));
        
        // 计算本季度时间范围
        LocalDate now = LocalDate.now();
        int quarter = now.get(IsoFields.QUARTER_OF_YEAR);
        int year = now.getYear();
        LocalDate quarterStart = LocalDate.of(year, (quarter - 1) * 3 + 1, 1);
        LocalDate quarterEnd = quarterStart.plusMonths(3).minusDays(1);
        
        LocalDateTime startTime = quarterStart.atStartOfDay();
        LocalDateTime endTime = quarterEnd.atTime(LocalTime.MAX);
        
        // 查询本季度捐赠总额
        int quarterlyDonation = warmCoinRecordRepository.sumDonationByTimeRange(startTime, endTime);
        
        // 查询最近捐赠时间
        LocalDateTime lastDonationTime = warmCoinRecordRepository.findLastDonationTime().orElse(null);
        
        return new CharityPoolStatsDTO(
                pool.getBalance(),
                quarterlyDonation,
                pool.getTotalDistributed(),
                lastDonationTime
        );
    }
    
    @Override
    public PageResponse<DonationRecordDTO> queryDonationRecords(DonationQueryDTO query) {
        Pageable pageable = PageRequest.of(query.getPage(), query.getSize());
        Page<WarmCoinRecord> recordPage;
        
        String keyword = query.getKeyword();
        LocalDateTime startTime = query.getStartTime();
        LocalDateTime endTime = query.getEndTime();
        
        // 根据条件查询
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 有关键词，先查用户
            List<Long> userIds = findUserIdsByKeyword(keyword.trim());
            
            if (userIds.isEmpty()) {
                // 没找到匹配用户，返回空结果
                return new PageResponse<>(List.of(), 0, 0, query.getPage(), query.getSize());
            }
            
            if (startTime != null && endTime != null) {
                recordPage = warmCoinRecordRepository.findDonationsByUserIdsAndTimeRange(
                        userIds, startTime, endTime, pageable);
            } else {
                recordPage = warmCoinRecordRepository.findDonationsByUserIds(userIds, pageable);
            }
        } else {
            // 无关键词
            if (startTime != null && endTime != null) {
                recordPage = warmCoinRecordRepository.findDonationsByTimeRange(startTime, endTime, pageable);
            } else {
                recordPage = warmCoinRecordRepository.findAllDonations(pageable);
            }
        }
        
        // 获取所有相关用户信息
        List<Long> userIds = recordPage.getContent().stream()
                .map(WarmCoinRecord::getUserId)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        
        // 转换为DTO
        List<DonationRecordDTO> dtoList = recordPage.getContent().stream()
                .map(record -> {
                    User user = userMap.get(record.getUserId());
                    String userName = user != null 
                            ? (user.getRealName() != null ? user.getRealName() : user.getUsername())
                            : "未知用户";
                    String address = user != null ? user.getAddress() : null;
                    
                    return new DonationRecordDTO(
                            record.getId(),
                            record.getUserId(),
                            userName,
                            Math.abs(record.getAmount()),  // 转为正数
                            address,
                            record.getCreateTime()
                    );
                })
                .collect(Collectors.toList());
        
        return new PageResponse<>(
                dtoList,
                recordPage.getTotalElements(),
                recordPage.getTotalPages(),
                recordPage.getNumber(),
                recordPage.getSize()
        );
    }
    
    /**
     * 根据关键词查找用户ID列表（支持姓名模糊匹配和用户ID精确匹配）
     */
    private List<Long> findUserIdsByKeyword(String keyword) {
        // 尝试解析为用户ID
        try {
            Long userId = Long.parseLong(keyword);
            if (userRepository.existsById(userId)) {
                return List.of(userId);
            }
        } catch (NumberFormatException ignored) {
            // 不是数字，按姓名搜索
        }
        
        // 按姓名模糊搜索
        return userRepository.findByRealNameContainingOrUsernameContaining(keyword, keyword)
                .stream()
                .map(User::getId)
                .collect(Collectors.toList());
    }
}
