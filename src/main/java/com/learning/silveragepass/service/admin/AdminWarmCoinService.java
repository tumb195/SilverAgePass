package com.learning.silveragepass.service.admin;

import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.admin.WarmCoinRecordDTO;
import com.learning.silveragepass.entity.Demand;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.entity.WarmCoinRecord;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.repository.DemandRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.repository.WarmCoinRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminWarmCoinService {
    
    private final WarmCoinRecordRepository warmCoinRecordRepository;
    private final UserRepository userRepository;
    private final DemandRepository demandRepository;
    
    /**
     * 获取暖龄币记录列表
     */
    public PageResponse<WarmCoinRecordDTO> getRecordList(int page, int pageSize, String search, String type, Long userId) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<WarmCoinRecord> recordPage;
        
        if (userId != null) {
            recordPage = warmCoinRecordRepository.findByUserId(userId, pageable);
        } else {
            recordPage = warmCoinRecordRepository.findAll(pageable);
        }
        
        List<WarmCoinRecordDTO> content = recordPage.getContent().stream()
                .map(this::convertToWarmCoinRecordDTO)
                .collect(Collectors.toList());
        
        return new PageResponse<>(
                content,
                recordPage.getTotalElements(),
                recordPage.getTotalPages(),
                recordPage.getNumber(),
                recordPage.getSize(),
                recordPage.isFirst(),
                recordPage.isLast(),
                recordPage.isEmpty()
        );
    }
    
    /**
     * 获取记录详情
     */
    public WarmCoinRecordDTO getRecordDetail(Long id) {
        WarmCoinRecord record = warmCoinRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("记录不存在"));
        return convertToWarmCoinRecordDTO(record);
    }
    
    /**
     * 获取用户暖龄币记录
     */
    public PageResponse<WarmCoinRecordDTO> getUserRecords(Long userId, int page, int pageSize) {
        return getRecordList(page, pageSize, null, null, userId);
    }
    
    private WarmCoinRecordDTO convertToWarmCoinRecordDTO(WarmCoinRecord record) {
        WarmCoinRecordDTO dto = new WarmCoinRecordDTO();
        dto.setId(record.getId());
        dto.setUserId(record.getUserId());
        dto.setAmount(record.getAmount());
        dto.setCreateTime(record.getCreateTime());
        dto.setType(record.getType() != null ? record.getType().name() : null);
        dto.setTypeDesc(record.getType() != null ? record.getType().getDescription() : "未知");
        dto.setDescription(record.getDescription());
        dto.setDemandId(record.getDemandId());
        
        // 获取用户信息
        if (record.getUserId() != null) {
            userRepository.findById(record.getUserId()).ifPresent(user -> {
                dto.setUsername(user.getUsername());
                dto.setPhone(user.getPhone());
                dto.setBalanceAfter(user.getWarmCoin());
            });
        }
        
        // 获取任务信息
        if (record.getDemandId() != null) {
            demandRepository.findById(record.getDemandId()).ifPresent(demand -> {
                dto.setDemandTitle(demand.getTitle());
            });
        }
        
        return dto;
    }
    
    /**
     * 违规扣分
     * 对用户进行违规扣分，扣除暖心币并创建记录
     * 
     * @param userId 用户ID
     * @param amount 扣除数量（正数，方法内会转为负数）
     * @param reason 违规原因
     */
    @Transactional
    public void violationDeduct(Long userId, Integer amount, String reason) {
        // 1. 查询用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        
        // 2. 检查余额并扣除
        int deductAmount = Math.abs(amount); // 确保是正数
        if (user.getWarmCoin() < deductAmount) {
            // 余额不足，扣除全部余额
            deductAmount = user.getWarmCoin();
        }
        
        user.setWarmCoin(user.getWarmCoin() - deductAmount);
        userRepository.save(user);
        
        // 3. 创建违规扣分记录
        WarmCoinRecord record = new WarmCoinRecord();
        record.setUserId(userId);
        record.setAmount(-deductAmount); // 负数表示扣除
        record.setType(WarmCoinRecord.RecordType.VIOLATION_POINTS);
        record.setDescription("违规扣分：" + reason);
        warmCoinRecordRepository.save(record);
    }
    
    /**
     * 管理员调整
     * 管理员手动调整用户的暖心币余额
     * 
     * @param userId 用户ID
     * @param amount 调整数量（正数=增加，负数=减少）
     * @param reason 调整原因
     */
    @Transactional
    public void adjustCoin(Long userId, Integer amount, String reason) {
        // 1. 查询用户
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));
        
        // 2. 调整暖心币
        int newAmount = user.getWarmCoin() + amount;
        if (newAmount < 0) {
            throw new IllegalArgumentException("调整后余额不能为负数，当前余额：" + user.getWarmCoin() + "，调整金额：" + amount);
        }
        
        user.setWarmCoin(newAmount);
        userRepository.save(user);
        
        // 3. 创建管理员调整记录
        WarmCoinRecord record = new WarmCoinRecord();
        record.setUserId(userId);
        record.setAmount(amount);
        record.setType(WarmCoinRecord.RecordType.ADMINISTRATOR_ADJUSTMENT);
        record.setDescription("管理员调整：" + reason);
        warmCoinRecordRepository.save(record);
    }

}
