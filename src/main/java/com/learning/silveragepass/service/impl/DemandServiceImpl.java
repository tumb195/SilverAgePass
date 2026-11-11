package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.*;
import com.learning.silveragepass.entity.Demand;
import com.learning.silveragepass.entity.DemandStatus;
import com.learning.silveragepass.entity.DemandType;
import com.learning.silveragepass.entity.Relative;
import com.learning.silveragepass.entity.RelativeStatus;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.entity.WarmCoinRecord;
import com.learning.silveragepass.exception.BusinessException;
import com.learning.silveragepass.exception.CertificationException;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.repository.CharityPoolRepository;
import com.learning.silveragepass.repository.DemandRepository;
import com.learning.silveragepass.repository.RelativeRepository;
import com.learning.silveragepass.service.DemandService;
import com.learning.silveragepass.service.NotificationEventPublisher;
import com.learning.silveragepass.service.OrganizationCertificationService;
import com.learning.silveragepass.service.RelativeService;
import com.learning.silveragepass.service.UserAddressService;
import com.learning.silveragepass.service.UserService;
import com.learning.silveragepass.service.WarmCoinRecordService;
import com.learning.silveragepass.util.GridAggregator;
import com.learning.silveragepass.util.WeightCalculator;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DemandServiceImpl implements DemandService {

    private static final Logger logger = LoggerFactory.getLogger(DemandServiceImpl.class);

    private final DemandRepository demandRepository;
    private final UserService userService;
    private final WarmCoinRecordService warmCoinRecordService;
    private final RelativeService relativeService;
    private final RelativeRepository relativeRepository;
    private final UserAddressService userAddressService;
    private final CharityPoolRepository charityPoolRepository;
    private final OrganizationCertificationService organizationCertificationService;
    
    @Autowired(required = false)
    private NotificationEventPublisher notificationEventPublisher;

    @Autowired
    public DemandServiceImpl(DemandRepository demandRepository, UserService userService,
            WarmCoinRecordService warmCoinRecordService, RelativeService relativeService,
            RelativeRepository relativeRepository, UserAddressService userAddressService,
            CharityPoolRepository charityPoolRepository,
            OrganizationCertificationService organizationCertificationService) {
        this.demandRepository = demandRepository;
        this.userService = userService;
        this.warmCoinRecordService = warmCoinRecordService;
        this.relativeService = relativeService;
        this.charityPoolRepository = charityPoolRepository;
        this.relativeRepository = relativeRepository;
        this.userAddressService = userAddressService;
        this.organizationCertificationService = organizationCertificationService;
    }

    @Override
    @Transactional
    public Demand createDemand(DemandCreateDTO dto, Long publisherId) {
        // 检查发布者是否存在
        User publisher = userService.getUserById(publisherId);

        // 检查暖龄币是否足够
        if (publisher.getWarmCoin() < dto.getWarmCoinReward()) {
            throw new IllegalArgumentException("暖龄币余额不足");
        }

        // 创建需求
        Demand demand = new Demand();
        demand.setTitle(dto.getTitle());
        demand.setType(dto.getType());
        demand.setDescription(dto.getDescription());
        demand.setServiceAddress(dto.getServiceAddress());
        demand.setServiceDate(dto.getServiceDate());
        demand.setServiceTime(dto.getServiceTime());
        demand.setEstimatedDuration(dto.getEstimatedDuration());
        demand.setWarmCoinReward(dto.getWarmCoinReward());
        demand.setStatus(DemandStatus.WAITING);
        demand.setPublisherId(publisherId);
        demand.setLatitude(dto.getLatitude());
        demand.setLongitude(dto.getLongitude());

        Demand savedDemand = demandRepository.save(demand);

        // 扣除发布者的暖龄币
        userService.updateWarmCoin(publisherId, -dto.getWarmCoinReward());

        // 创建暖龄币记录
        WarmCoinRecord record = new WarmCoinRecord();
        record.setUserId(publisherId);
        record.setDemandId(savedDemand.getId());
        record.setAmount(-dto.getWarmCoinReward());
        record.setType(WarmCoinRecord.RecordType.DEMAND_SPEND);
        record.setDescription("发布需求：" + dto.getTitle());
        warmCoinRecordService.createRecord(record);

        // 保存地址到用户的地址列表（使用新的地址管理服务）
        try {
            UserAddressCreateDTO addressDto = new UserAddressCreateDTO();
            addressDto.setAddress(dto.getServiceAddress());
            addressDto.setLatitude(dto.getLatitude());
            addressDto.setLongitude(dto.getLongitude());

            // 先检查地址是否已存在
            if (userAddressService.existsByUserIdAndAddress(publisherId, dto.getServiceAddress())) {
                // 如果地址已存在，查找地址并更新最后使用时间
                userAddressService.updateLastUsedTimeByAddress(publisherId, dto.getServiceAddress());
            } else {
                // 如果地址不存在，创建新地址
                userAddressService.createAddress(publisherId, addressDto);
            }
        } catch (Exception e) {
            // 地址操作失败时忽略，不影响需求发布
            logger.warn("保存地址失败，发布者ID: {}, 地址: {}, 错误: {}",
                    publisherId, dto.getServiceAddress(), e.getMessage());
        }

        // 通知亲人（如果有配置分享需求的亲人关系）
        try {
            relativeService.notifyRelativesAboutDemand(publisherId, savedDemand.getId());
        } catch (Exception e) {
            // 通知失败时忽略，不影响需求发布
            // 记录日志但不抛出异常，避免事务回滚
            logger.warn("通知亲人需求失败，需求ID: {}, 发布者ID: {}, 错误: {}",
                    savedDemand.getId(), publisherId, e.getMessage());
        }

        return savedDemand;
    }

    @Override
    public DemandDTO getDemandById(Long id) {
        Demand demand = demandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("需求不存在，ID: " + id));

        return convertToDTO(demand);
    }

    @Override
    @Transactional
    public Demand updateDemand(Long id, DemandCreateDTO dto, Long userId) {
        Demand demand = demandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("需求不存在，ID: " + id));

        // 检查是否是发布者
        if (!demand.getPublisherId().equals(userId)) {
            throw new IllegalArgumentException("只有发布者才能更新需求");
        }

        // 检查需求状态，只有待接单状态才能更新
        if (demand.getStatus() != DemandStatus.WAITING) {
            throw new IllegalArgumentException("只有待接单状态的需求才能更新");
        }

        // 更新需求信息
        demand.setTitle(dto.getTitle());
        demand.setType(dto.getType());
        demand.setDescription(dto.getDescription());
        demand.setServiceAddress(dto.getServiceAddress());
        demand.setServiceDate(dto.getServiceDate());
        demand.setServiceTime(dto.getServiceTime());
        demand.setEstimatedDuration(dto.getEstimatedDuration());

        // 如果暖龄币奖励有变化，需要更新用户的暖龄币
        if (!demand.getWarmCoinReward().equals(dto.getWarmCoinReward())) {
            int diff = dto.getWarmCoinReward() - demand.getWarmCoinReward();

            // 检查暖龄币是否足够
            User user = userService.getUserById(userId);
            if (diff > 0 && user.getWarmCoin() < diff) {
                throw new IllegalArgumentException("暖龄币余额不足");
            }

            // 更新用户暖龄币
            userService.updateWarmCoin(userId, -diff);

            // 创建暖龄币记录
            if (diff != 0) {
                WarmCoinRecord record = new WarmCoinRecord();
                record.setUserId(userId);
                record.setDemandId(id);
                record.setAmount(-diff);
                record.setType(WarmCoinRecord.RecordType.DEMAND_SPEND);
                record.setDescription("更新需求暖龄币奖励：" + dto.getTitle());
                warmCoinRecordService.createRecord(record);
            }

            // 更新需求的暖龄币奖励
            demand.setWarmCoinReward(dto.getWarmCoinReward());
        }

        // 更新地理位置坐标
        if (dto.getLatitude() != null && dto.getLongitude() != null) {
            demand.setLatitude(dto.getLatitude());
            demand.setLongitude(dto.getLongitude());
        }

        return demandRepository.save(demand);
    }

    @Override
    @Transactional
    public Demand cancelDemand(Long id, Long userId) {
        Demand demand = demandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("需求不存在，ID: " + id));

        // 检查是否是发布者
        if (!demand.getPublisherId().equals(userId)) {
            throw new IllegalArgumentException("只有发布者才能取消需求");
        }

        // 检查需求状态，只有待接单状态才能取消
        if (demand.getStatus() != DemandStatus.WAITING) {
            throw new IllegalArgumentException("只有待接单状态的需求才能取消");
        }

        // 更新需求状态
        demand.setStatus(DemandStatus.CANCELLED);

        // 退还暖龄币
        userService.updateWarmCoin(userId, demand.getWarmCoinReward());

        // 创建暖龄币记录
        WarmCoinRecord record = new WarmCoinRecord();
        record.setUserId(userId);
        record.setDemandId(id);
        record.setAmount(demand.getWarmCoinReward());
        record.setType(WarmCoinRecord.RecordType.DEMAND_SPEND);
        record.setDescription("取消需求退还暖龄币：" + demand.getTitle());
        warmCoinRecordService.createRecord(record);

        Demand savedDemand = demandRepository.save(demand);
        
        // 发送通知 - Requirements: 6.3
        if (notificationEventPublisher != null) {
            notificationEventPublisher.publishDemandCancelled(
                    savedDemand.getId(),
                    savedDemand.getPublisherId(),
                    savedDemand.getVolunteerId(),
                    savedDemand.getTitle(),
                    userId
            );
        }
        
        return savedDemand;
    }

    @Override
    @Transactional
    public Demand acceptDemand(Long id, Long volunteerId) {
        // 使用悲观锁防止并发问题
        Demand demand = demandRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("需求不存在，ID: " + id));

        // 检查需求状态，只有待接单状态才能接单
        if (demand.getStatus() != DemandStatus.WAITING) {
            throw new IllegalArgumentException("只有待接单状态的需求才能接单");
        }

        // 检查是否是自己发布的需求
        if (demand.getPublisherId().equals(volunteerId)) {
            throw new IllegalArgumentException("不能接自己发布的需求");
        }

        // 检查用户是否已通过组织认证 - Requirements: 5.1, 5.2, 5.3
        if (!organizationCertificationService.isUserCertified(volunteerId)) {
            throw new CertificationException(CertificationException.ErrorCode.NOT_CERTIFIED);
        }

        // 更新需求状态为已接单
        demand.setStatus(DemandStatus.ACCEPTED);
        demand.setVolunteerId(volunteerId);
        demand.setAcceptTime(LocalDateTime.now());

        Demand savedDemand = demandRepository.save(demand);
        
        // 发送通知 - Requirements: 6.1
        if (notificationEventPublisher != null) {
            notificationEventPublisher.publishDemandAccepted(
                    savedDemand.getId(),
                    savedDemand.getPublisherId(),
                    volunteerId,
                    savedDemand.getTitle()
            );
        }
        
        return savedDemand;
    }

    @Override
    @Transactional
    public Demand startService(ServiceProofDTO proofDTO, Long volunteerId) {
        Demand demand = demandRepository.findById(proofDTO.getDemandId())
                .orElseThrow(() -> new ResourceNotFoundException("需求不存在，ID: " + proofDTO.getDemandId()));

        // 检查是否是接单的志愿者
        if (!demand.getVolunteerId().equals(volunteerId)) {
            throw new IllegalArgumentException("只有接单的志愿者才能开始服务");
        }

        // 检查需求状态，只有已接单状态才能开始服务
        if (demand.getStatus() != DemandStatus.ACCEPTED) {
            throw new IllegalArgumentException("只有已接单状态的需求才能开始服务");
        }

        // 检查是否是开始服务凭证
        if (proofDTO.getType() != ServiceProofDTO.ProofType.START) {
            throw new IllegalArgumentException("凭证类型不正确");
        }

        // 更新需求信息，状态改为进行中
        demand.setStatus(DemandStatus.IN_PROGRESS);
        demand.setStartTime(LocalDateTime.now());
        demand.setStartProof(proofDTO.getImageUrl() + "," + proofDTO.getDescription());

        return demandRepository.save(demand);
    }

    @Override
    @Transactional
    public Demand endService(ServiceProofDTO proofDTO, Long volunteerId) {
        Demand demand = demandRepository.findById(proofDTO.getDemandId())
                .orElseThrow(() -> new ResourceNotFoundException("需求不存在，ID: " + proofDTO.getDemandId()));

        // 检查是否是接单的志愿者
        if (!demand.getVolunteerId().equals(volunteerId)) {
            throw new IllegalArgumentException("只有接单的志愿者才能结束服务");
        }

        // 检查需求状态，只有进行中状态才能结束服务
        if (demand.getStatus() != DemandStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("只有进行中状态的需求才能结束服务");
        }

        // 检查是否已经开始服务
        if (demand.getStartTime() == null) {
            throw new IllegalArgumentException("需要先开始服务才能结束服务");
        }

        // 检查是否是结束服务凭证
        if (proofDTO.getType() != ServiceProofDTO.ProofType.END) {
            throw new IllegalArgumentException("凭证类型不正确");
        }

        // 更新需求信息
        demand.setEndTime(LocalDateTime.now());
        demand.setEndProof(proofDTO.getImageUrl() + "," + proofDTO.getDescription());
        demand.setStatus(DemandStatus.COMPLETED);

        Demand savedDemand = demandRepository.save(demand);
        
        // 发送通知 - Requirements: 6.2
        if (notificationEventPublisher != null) {
            notificationEventPublisher.publishDemandCompleted(
                    savedDemand.getId(),
                    savedDemand.getPublisherId(),
                    volunteerId,
                    savedDemand.getTitle()
            );
        }
        
        return savedDemand;
    }

    @Override
    @Transactional
    public boolean distributeWarmCoin(WarmCoinDistributionDTO dto, Long volunteerId) {
        Demand demand = demandRepository.findById(dto.getDemandId())
                .orElseThrow(() -> new ResourceNotFoundException("需求不存在，ID: " + dto.getDemandId()));

        // 检查是否是接单的志愿者
        if (!demand.getVolunteerId().equals(volunteerId)) {
            throw new IllegalArgumentException("只有接单的志愿者才能分配暖龄币");
        }

        // 检查需求状态，只有已完成状态才能分配暖龄币
        if (demand.getStatus() != DemandStatus.COMPLETED) {
            throw new IllegalArgumentException("只有已完成状态的需求才能分配暖龄币");
        }

        // 检查比例总和是否为100
        int totalPercentage = dto.getSelfPercentage() + dto.getParentPercentage() + dto.getDonatePercentage();
        if (totalPercentage != 100) {
            throw new IllegalArgumentException("分配比例总和必须为100%");
        }

        // 计算各部分的暖龄币数量
        int totalReward = demand.getWarmCoinReward();
        int selfAmount = totalReward * dto.getSelfPercentage() / 100;
        int parentAmount = totalReward * dto.getParentPercentage() / 100;
        int donateAmount = totalReward * dto.getDonatePercentage() / 100;

        // 检查转赠父母的用户ID
        if (dto.getParentPercentage() > 0 && dto.getParentUserId() == null) {
            throw new IllegalArgumentException("转赠父母时需要指定父母用户ID");
        }

        // 验证父母联系人关系（如果选择转赠父母）
        if (parentAmount > 0) {
            // 验证父母用户是否存在（如果不存在会抛出 ResourceNotFoundException）
            userService.getUserById(dto.getParentUserId());

            // 验证父母关系是否存在且已接受
            // 检查双向关系：志愿者可能是关系的发起方或接收方
            Optional<Relative> relationship1 = relativeRepository.findByUserIdAndRelativeId(volunteerId,
                    dto.getParentUserId());
            Optional<Relative> relationship2 = relativeRepository.findByUserIdAndRelativeId(dto.getParentUserId(),
                    volunteerId);

            boolean hasValidRelationship = false;
            if (relationship1.isPresent() && relationship1.get().getStatus() == RelativeStatus.ACCEPTED) {
                hasValidRelationship = true;
            } else if (relationship2.isPresent() && relationship2.get().getStatus() == RelativeStatus.ACCEPTED) {
                hasValidRelationship = true;
            }

            if (!hasValidRelationship) {
                throw new IllegalArgumentException("您与指定的父母用户之间不存在已接受的父母关系，无法转赠暖龄币");
            }
        }

        // 使用 try-catch 包装分配逻辑，如果分配失败则回滚任务状态
        try {
            // 分配暖龄币
            // 1. 自己保留部分
            if (selfAmount > 0) {
                userService.updateWarmCoin(volunteerId, selfAmount);

                WarmCoinRecord record = new WarmCoinRecord();
                record.setUserId(volunteerId);
                record.setDemandId(dto.getDemandId());
                record.setAmount(selfAmount);
                record.setType(WarmCoinRecord.RecordType.SERVICE_EARN);
                record.setDescription("完成服务获得暖龄币：" + demand.getTitle());
                warmCoinRecordService.createRecord(record);
            }

            // 2. 转赠父母部分
            if (parentAmount > 0) {
                userService.updateWarmCoin(dto.getParentUserId(), parentAmount);

                WarmCoinRecord record = new WarmCoinRecord();
                record.setUserId(volunteerId);
                record.setDemandId(dto.getDemandId());
                record.setAmount(-parentAmount);
                record.setType(WarmCoinRecord.RecordType.TRANSFER);
                record.setTargetUserId(dto.getParentUserId());
                record.setDescription("转赠暖龄币给父母");
                warmCoinRecordService.createRecord(record);

                WarmCoinRecord parentRecord = new WarmCoinRecord();
                parentRecord.setUserId(dto.getParentUserId());
                parentRecord.setDemandId(dto.getDemandId());
                parentRecord.setAmount(parentAmount);
                parentRecord.setType(WarmCoinRecord.RecordType.TRANSFER);
                parentRecord.setDescription("收到子女转赠的暖龄币");
                warmCoinRecordService.createRecord(parentRecord);
            }

            // 3. 捐赠公益池部分
            if (donateAmount > 0) {
                WarmCoinRecord record = new WarmCoinRecord();
                record.setUserId(volunteerId);
                record.setDemandId(dto.getDemandId());
                record.setAmount(-donateAmount);
                record.setType(WarmCoinRecord.RecordType.DONATE);
                record.setDescription("完成任务捐赠暖龄币到公益池：" + demand.getTitle());
                warmCoinRecordService.createRecord(record);

                // 增加公益池余额
                charityPoolRepository.addDonation(donateAmount);
            }

            return true;
        } catch (Exception e) {
            // 如果分配过程中出现任何异常，将任务状态回滚到 IN_PROGRESS
            demand.setStatus(DemandStatus.IN_PROGRESS);
            demandRepository.save(demand);
            // 重新抛出异常，让调用方知道分配失败
            throw e;
        }
    }

    @Override
    public Page<DemandDTO> queryDemands(DemandQueryDTO queryDTO) {
        // 创建排序
        Sort sort = Sort.by(
                queryDTO.getSortDirection().equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC,
                queryDTO.getSortBy());

        // 创建分页
        Pageable pageable = PageRequest.of(queryDTO.getPage(), queryDTO.getSize(), sort);

        // 创建查询条件
        Specification<Demand> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 关键词搜索
            if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
                Predicate titlePredicate = criteriaBuilder.like(root.get("title"), "%" + queryDTO.getKeyword() + "%");
                Predicate descPredicate = criteriaBuilder.like(root.get("description"),
                        "%" + queryDTO.getKeyword() + "%");
                predicates.add(criteriaBuilder.or(titlePredicate, descPredicate));
            }

            // 类型过滤
            if (queryDTO.getTypes() != null && !queryDTO.getTypes().isEmpty()) {
                predicates.add(root.get("type").in(queryDTO.getTypes()));
            }

            // 状态过滤
            if (queryDTO.getStatuses() != null && !queryDTO.getStatuses().isEmpty()) {
                predicates.add(root.get("status").in(queryDTO.getStatuses()));
            }

            // 服务日期范围过滤
            if (queryDTO.getStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("serviceDate"), queryDTO.getStartDate()));
            }
            if (queryDTO.getEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("serviceDate"), queryDTO.getEndDate()));
            }

            // 发布者过滤
            if (queryDTO.getPublisherId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("publisherId"), queryDTO.getPublisherId()));
            }

            // 志愿者过滤
            if (queryDTO.getVolunteerId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("volunteerId"), queryDTO.getVolunteerId()));
            }

            // 地理位置过滤 - 暂时跳过，将在查询后单独处理
            // 因为JPA Criteria API对MySQL空间函数支持有限，我们将在查询后过滤

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        // 执行查询
        Page<Demand> demands = demandRepository.findAll(spec, pageable);

        // 如果启用了地理位置过滤，需要进一步处理
        if (queryDTO.getNearbyOnly() && queryDTO.getLatitude() != null && queryDTO.getLongitude() != null) {
            // 使用Repository的地理位置查询方法
            List<Demand> nearbyDemands = demandRepository.findNearbyDemandsWithoutStatus(
                    queryDTO.getLatitude(),
                    queryDTO.getLongitude(),
                    queryDTO.getRadius());

            // 应用其他过滤条件到地理位置查询结果
            List<Demand> filteredDemands = nearbyDemands.stream()
                    .filter(demand -> {
                        // 应用其他过滤条件
                        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().isEmpty()) {
                            if (!demand.getTitle().contains(queryDTO.getKeyword()) &&
                                    !demand.getDescription().contains(queryDTO.getKeyword())) {
                                return false;
                            }
                        }

                        if (queryDTO.getTypes() != null && !queryDTO.getTypes().isEmpty()) {
                            if (!queryDTO.getTypes().contains(demand.getType())) {
                                return false;
                            }
                        }

                        if (queryDTO.getStatuses() != null && !queryDTO.getStatuses().isEmpty()) {
                            if (!queryDTO.getStatuses().contains(demand.getStatus())) {
                                return false;
                            }
                        }

                        if (queryDTO.getStartDate() != null) {
                            if (demand.getServiceDate().isBefore(queryDTO.getStartDate())) {
                                return false;
                            }
                        }

                        if (queryDTO.getEndDate() != null) {
                            if (demand.getServiceDate().isAfter(queryDTO.getEndDate())) {
                                return false;
                            }
                        }

                        if (queryDTO.getPublisherId() != null) {
                            if (!demand.getPublisherId().equals(queryDTO.getPublisherId())) {
                                return false;
                            }
                        }

                        if (queryDTO.getVolunteerId() != null) {
                            if (demand.getVolunteerId() == null
                                    || !demand.getVolunteerId().equals(queryDTO.getVolunteerId())) {
                                return false;
                            }
                        }

                        return true;
                    })
                    .collect(Collectors.toList());

            // 手动分页
            int start = queryDTO.getPage() * queryDTO.getSize();
            int end = Math.min(start + queryDTO.getSize(), filteredDemands.size());
            List<Demand> pagedDemands = filteredDemands.subList(start, end);

            // 转换为DTO
            List<DemandDTO> demandDTOs = pagedDemands.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            // 创建分页结果
            return new PageImpl<>(demandDTOs, pageable, filteredDemands.size());
        }

        // 转换为DTO
        return demands.map(this::convertToDTO);
    }

    @Override
    public DemandStatisticsDTO getDemandStatistics() {
        DemandStatisticsDTO statisticsDTO = new DemandStatisticsDTO();

        // 统计需求总数
        long totalCount = demandRepository.count();
        statisticsDTO.setTotalCount(totalCount);

        // 统计各状态的需求数量
        long waitingCount = demandRepository.countByStatus(DemandStatus.WAITING);
        long acceptedCount = demandRepository.countByStatus(DemandStatus.ACCEPTED);
        long inProgressCount = demandRepository.countByStatus(DemandStatus.IN_PROGRESS);
        long completedCount = demandRepository.countByStatus(DemandStatus.COMPLETED);
        long cancelledCount = demandRepository.countByStatus(DemandStatus.CANCELLED);

        statisticsDTO.setWaitingCount(waitingCount);
        statisticsDTO.setAcceptedCount(acceptedCount);
        statisticsDTO.setInProgressCount(inProgressCount);
        statisticsDTO.setCompletedCount(completedCount);
        statisticsDTO.setCancelledCount(cancelledCount);

        // 计算平均响应时间
        Double avgResponseTime = demandRepository.calculateAvgResponseTime();
        statisticsDTO.setAvgResponseTime(avgResponseTime != null ? avgResponseTime : 0.0);

        // 统计需求类型分布
        List<DemandStatisticsDTO.TypeDistribution> typeDistributions = new ArrayList<>();
        for (DemandType type : DemandType.values()) {
            long count = demandRepository.countByType(type);
            double percentage = totalCount > 0 ? (double) count / totalCount * 100 : 0;

            DemandStatisticsDTO.TypeDistribution typeDistribution = new DemandStatisticsDTO.TypeDistribution(
                    type.name(),
                    type.getDescription(),
                    count,
                    percentage);
            typeDistributions.add(typeDistribution);
        }
        statisticsDTO.setTypeDistributions(typeDistributions);

        // 统计需求状态分布
        List<DemandStatisticsDTO.StatusDistribution> statusDistributions = new ArrayList<>();
        for (DemandStatus status : DemandStatus.values()) {
            long count;
            switch (status) {
                case WAITING:
                    count = waitingCount;
                    break;
                case ACCEPTED:
                    count = acceptedCount;
                    break;
                case IN_PROGRESS:
                    count = inProgressCount;
                    break;
                case COMPLETED:
                    count = completedCount;
                    break;
                case CANCELLED:
                    count = cancelledCount;
                    break;
                default:
                    count = 0;
            }

            double percentage = totalCount > 0 ? (double) count / totalCount * 100 : 0;

            DemandStatisticsDTO.StatusDistribution statusDistribution = new DemandStatisticsDTO.StatusDistribution(
                    status.name(),
                    status.getDescription(),
                    count,
                    percentage);
            statusDistributions.add(statusDistribution);
        }
        statisticsDTO.setStatusDistributions(statusDistributions);

        // 统计需求时间分布
        List<Object[]> monthlyData = demandRepository.countByMonth();
        List<DemandStatisticsDTO.TimeDistribution> timeDistributions = new ArrayList<>();

        for (Object[] data : monthlyData) {
            String month = (String) data[0];
            Long count = ((Number) data[1]).longValue();

            DemandStatisticsDTO.TimeDistribution timeDistribution = new DemandStatisticsDTO.TimeDistribution(
                    month,
                    count);
            timeDistributions.add(timeDistribution);
        }
        statisticsDTO.setTimeDistributions(timeDistributions);

        return statisticsDTO;
    }

    @Override
    public List<HeatMapDataDTO> getHeatMapData(Double latitude, Double longitude, Integer radius) {
        // 查询附近的待接单需求
        List<Demand> nearbyDemands = demandRepository.findNearbyDemands(
                DemandStatus.WAITING,
                latitude,
                longitude,
                radius);

        // 转换为热力图数据
        return nearbyDemands.stream().map(demand -> {
            HeatMapDataDTO dto = new HeatMapDataDTO();
            dto.setId(demand.getId());
            dto.setLatitude(demand.getLatitude());
            dto.setLongitude(demand.getLongitude());
            dto.setWeight(1); // 可以根据需求紧急程度或其他因素设置权重
            dto.setType(demand.getType());
            dto.setTitle(demand.getTitle());

            // 计算与当前位置的距离
            if (latitude != null && longitude != null && demand.getLatitude() != null
                    && demand.getLongitude() != null) {
                // 使用简化的距离计算公式（平面距离）
                double dx = (longitude - demand.getLongitude())
                        * Math.cos((latitude + demand.getLatitude()) * Math.PI / 360);
                double dy = latitude - demand.getLatitude();
                double distance = 111.2 * Math.sqrt(dx * dx + dy * dy) * 1000; // 转换为米
                dto.setDistance(distance);
            }

            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public Page<DemandDTO> getVolunteerAcceptedDemands(Long volunteerId, DemandQueryDTO queryDTO) {
        // 设置志愿者ID到查询条件中
        queryDTO.setVolunteerId(volunteerId);

        // 复用现有的queryDemands方法
        return queryDemands(queryDTO);
    }

    @Override
    @Transactional
    public Demand cancelAcceptedDemand(Long id, Long volunteerId) {
        Demand demand = demandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("需求不存在，ID: " + id));

        // 检查是否是接单的志愿者
        if (demand.getVolunteerId() == null || !demand.getVolunteerId().equals(volunteerId)) {
            throw new IllegalArgumentException("只有接单的志愿者才能取消已接的订单");
        }

        // 检查需求状态，只有已接单或进行中状态才能取消接单
        if (demand.getStatus() != DemandStatus.ACCEPTED && demand.getStatus() != DemandStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("只有已接单或进行中状态的订单才能取消接单");
        }

        // 检查是否已经开始服务
        if (demand.getStartTime() != null) {
            throw new IllegalArgumentException("服务已开始，无法取消订单。请联系需求发布者处理");
        }

        // 将订单状态恢复为待接单
        demand.setStatus(DemandStatus.WAITING);
        // 保存志愿者ID用于通知
        Long originalVolunteerId = demand.getVolunteerId();
        // 清除志愿者相关信息
        demand.setVolunteerId(null);
        demand.setAcceptTime(null);
        // 清除服务凭证（如果有的话）
        demand.setStartProof(null);

        Demand savedDemand = demandRepository.save(demand);
        
        // 发送通知 - Requirements: 6.3 (志愿者取消接单也属于取消场景)
        if (notificationEventPublisher != null) {
            notificationEventPublisher.publishDemandCancelled(
                    savedDemand.getId(),
                    savedDemand.getPublisherId(),
                    originalVolunteerId,
                    savedDemand.getTitle(),
                    volunteerId
            );
        }
        
        return savedDemand;
    }

    /**
     * 获取每日任务状态统计
     */
    @Override
    public List<DailyStatusStatsDTO> getDailyStatusStats(Integer days) {
        if (days == null || days <= 0) {
            days = 7; // 默认7天
        }

        List<DailyStatusStatsDTO> statsList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 获取最近N天的统计数据
        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime date = now.minusDays(i);
            LocalDateTime dayStart = date.withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime dayEnd = date.withHour(23).withMinute(59).withSecond(59).withNano(999999999);

            // 统计当天各状态的任务数量
            Long waiting = demandRepository.countByStatusAndCreateTimeBetween(
                    DemandStatus.WAITING, dayStart, dayEnd);
            Long accepted = demandRepository.countByStatusAndCreateTimeBetween(
                    DemandStatus.ACCEPTED, dayStart, dayEnd);
            Long inProgress = demandRepository.countByStatusAndCreateTimeBetween(
                    DemandStatus.IN_PROGRESS, dayStart, dayEnd);
            Long completed = demandRepository.countByStatusAndCreateTimeBetween(
                    DemandStatus.COMPLETED, dayStart, dayEnd);
            Long cancelled = demandRepository.countByStatusAndCreateTimeBetween(
                    DemandStatus.CANCELLED, dayStart, dayEnd);

            DailyStatusStatsDTO stats = DailyStatusStatsDTO.builder()
                    .date(date.format(formatter))
                    .pending(waiting != null ? waiting : 0L)
                    .accepted(accepted != null ? accepted : 0L)
                    .inProgress(inProgress != null ? inProgress : 0L)
                    .completed(completed != null ? completed : 0L)
                    .cancelled(cancelled != null ? cancelled : 0L)
                    .build();

            statsList.add(stats);
        }

        return statsList;
    }

    /**
     * 将Demand实体转换为DemandDTO
     */
    private DemandDTO convertToDTO(Demand demand) {
        DemandDTO dto = new DemandDTO();
        BeanUtils.copyProperties(demand, dto);

        // 获取发布者信息
        try {
            User publisher = userService.getUserById(demand.getPublisherId());
            dto.setPublisherName(publisher.getRealName());
            dto.setPublisherPhone(publisher.getPhone());
        } catch (Exception e) {
            // 忽略异常，发布者可能已被删除
        }

        // 获取志愿者信息
        if (demand.getVolunteerId() != null) {
            try {
                User volunteer = userService.getUserById(demand.getVolunteerId());
                dto.setVolunteerName(volunteer.getRealName());
                dto.setVolunteerPhone(volunteer.getPhone());
            } catch (Exception e) {
                // 忽略异常，志愿者可能已被删除
            }
        }

        // 获取代发创建者信息
        if (demand.getProxyCreatorId() != null) {
            try {
                User proxyCreator = userService.getUserById(demand.getProxyCreatorId());
                dto.setProxyCreatorName(proxyCreator.getRealName());
            } catch (Exception e) {
                // 忽略异常，代发创建者可能已被删除
            }
        }

        return dto;
    }

    /**
     * 获取增强版热力图数据
     * 支持类型过滤、权重计算和距离计算
     * 
     * Requirements: 1.1, 1.2, 1.3, 3.1, 4.1
     */
    @Override
    public HeatMapResponseDTO getEnhancedHeatMapData(HeatMapRequestDTO request) {
        // 准备类型过滤参数
        List<String> typeStrings = null;
        boolean hasTypeFilter = false;
        if (request.getTypes() != null && !request.getTypes().isEmpty()) {
            typeStrings = request.getTypes().stream()
                    .map(DemandType::name)
                    .collect(Collectors.toList());
            hasTypeFilter = true;
        }

        // 查询附近的待接单需求（只查询WAITING状态）
        List<Demand> nearbyDemands = demandRepository.findNearbyDemandsWithTypes(
                DemandStatus.WAITING.name(),
                request.getLatitude(),
                request.getLongitude(),
                request.getRadius(),
                typeStrings,
                hasTypeFilter,
                request.getMaxResults());

        // 转换为热力图数据点
        List<HeatMapDataDTO> points = nearbyDemands.stream()
                .map(demand -> convertToHeatMapDataDTO(demand, request.getLatitude(), request.getLongitude()))
                .collect(Collectors.toList());

        // 计算类型分布统计
        Map<DemandType, Long> typeDistribution = nearbyDemands.stream()
                .filter(d -> d.getType() != null)
                .collect(Collectors.groupingBy(Demand::getType, Collectors.counting()));

        // 构建响应
        HeatMapResponseDTO response = new HeatMapResponseDTO();
        response.setCenterLatitude(request.getLatitude());
        response.setCenterLongitude(request.getLongitude());
        response.setRadius(request.getRadius());
        response.setTotalCount(points.size());
        response.setPoints(points);
        response.setGrids(null); // 非聚合模式不返回网格数据
        response.setTypeDistribution(typeDistribution);

        return response;
    }

    /**
     * 获取聚合热力图数据
     * 将需求按网格聚合，返回每个网格的统计信息
     * 
     * Requirements: 2.1, 2.2, 2.3, 2.4
     */
    @Override
    public List<HeatMapGridDTO> getAggregatedHeatMapData(HeatMapRequestDTO request) {
        // 准备类型过滤参数
        List<String> typeStrings = null;
        boolean hasTypeFilter = false;
        if (request.getTypes() != null && !request.getTypes().isEmpty()) {
            typeStrings = request.getTypes().stream()
                    .map(DemandType::name)
                    .collect(Collectors.toList());
            hasTypeFilter = true;
        }

        // 查询附近的待接单需求
        List<Demand> nearbyDemands = demandRepository.findNearbyDemandsWithTypes(
                DemandStatus.WAITING.name(),
                request.getLatitude(),
                request.getLongitude(),
                request.getRadius(),
                typeStrings,
                hasTypeFilter,
                request.getMaxResults());

        // 使用GridAggregator进行网格聚合
        List<HeatMapGridDTO> grids = GridAggregator.aggregate(
                nearbyDemands,
                request.getLatitude(),
                request.getLongitude(),
                request.getGridSize());

        // 为每个网格计算平均距离
        for (HeatMapGridDTO grid : grids) {
            // 计算网格中心到用户位置的距离作为平均距离的近似值
            Double avgDistance = GridAggregator.calculateDistance(
                    request.getLatitude(),
                    request.getLongitude(),
                    grid.getCenterLatitude(),
                    grid.getCenterLongitude());
            grid.setAvgDistance(avgDistance);
        }

        return grids;
    }

    /**
     * 将Demand实体转换为HeatMapDataDTO
     * 包含权重计算和距离计算
     */
    private HeatMapDataDTO convertToHeatMapDataDTO(Demand demand, Double userLat, Double userLng) {
        HeatMapDataDTO dto = new HeatMapDataDTO();
        dto.setId(demand.getId());
        dto.setLatitude(demand.getLatitude());
        dto.setLongitude(demand.getLongitude());
        dto.setType(demand.getType());
        dto.setTitle(demand.getTitle());
        dto.setServiceDate(demand.getServiceDate());

        // 计算权重
        int weight = WeightCalculator.calculateWeight(demand);
        dto.setWeight(weight);

        // 计算紧急程度 (1-5)
        dto.setUrgencyLevel(calculateUrgencyLevel(demand.getServiceDate()));

        // 计算与用户的距离
        if (userLat != null && userLng != null && demand.getLatitude() != null && demand.getLongitude() != null) {
            double distance = GridAggregator.calculateDistance(
                    userLat, userLng,
                    demand.getLatitude(), demand.getLongitude());
            dto.setDistance(distance);
        }

        return dto;
    }

    /**
     * 根据服务日期计算紧急程度
     * 1 = 最不紧急（超过7天）
     * 5 = 最紧急（24小时内）
     */
    private Integer calculateUrgencyLevel(LocalDate serviceDate) {
        if (serviceDate == null) {
            return 1;
        }

        long daysUntilService = ChronoUnit.DAYS.between(LocalDate.now(), serviceDate);

        if (daysUntilService < 0) {
            return 1; // 已过期
        } else if (daysUntilService <= 1) {
            return 5; // 24小时内
        } else if (daysUntilService <= 3) {
            return 4; // 3天内
        } else if (daysUntilService <= 7) {
            return 3; // 一周内
        } else if (daysUntilService <= 14) {
            return 2; // 两周内
        } else {
            return 1; // 超过两周
        }
    }
}