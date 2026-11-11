package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.DemandDTO;
import com.learning.silveragepass.dto.UserAddressCreateDTO;
import com.learning.silveragepass.dto.UserAddressDTO;
import com.learning.silveragepass.dto.proxy.ProxyAddressListDTO;
import com.learning.silveragepass.dto.proxy.ProxyDemandCreateDTO;
import com.learning.silveragepass.entity.Demand;
import com.learning.silveragepass.entity.DemandStatus;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.entity.WarmCoinRecord;
import com.learning.silveragepass.exception.BadRequestException;
import com.learning.silveragepass.repository.DemandRepository;
import com.learning.silveragepass.service.MessageService;
import com.learning.silveragepass.service.ProxyAuthorizationService;
import com.learning.silveragepass.service.ProxyDemandService;
import com.learning.silveragepass.service.UserAddressService;
import com.learning.silveragepass.service.UserService;
import com.learning.silveragepass.service.WarmCoinRecordService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 代发需求服务实现类
 * Requirements: 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3, 4.4
 */
@Service
public class ProxyDemandServiceImpl implements ProxyDemandService {

    private final ProxyAuthorizationService proxyAuthorizationService;
    private final UserAddressService userAddressService;
    private final UserService userService;
    private final DemandRepository demandRepository;
    private final WarmCoinRecordService warmCoinRecordService;
    private final MessageService messageService;

    @Autowired
    public ProxyDemandServiceImpl(
            ProxyAuthorizationService proxyAuthorizationService,
            UserAddressService userAddressService,
            UserService userService,
            DemandRepository demandRepository,
            WarmCoinRecordService warmCoinRecordService,
            MessageService messageService) {
        this.proxyAuthorizationService = proxyAuthorizationService;
        this.userAddressService = userAddressService;
        this.userService = userService;
        this.demandRepository = demandRepository;
        this.warmCoinRecordService = warmCoinRecordService;
        this.messageService = messageService;
    }


    /**
     * 获取可用地址列表
     * 返回老人和联系人双方的地址簿
     * Requirements: 4.1, 4.2, 4.3
     */
    @Override
    public ProxyAddressListDTO getAvailableAddresses(Long relativeId, Long elderId) {
        // 验证授权关系
        if (!proxyAuthorizationService.hasValidAuthorization(relativeId, elderId)) {
            throw new BadRequestException("您没有代发需求的授权");
        }

        // 获取老人的地址列表
        List<UserAddressDTO> elderAddresses = userAddressService.getAddresses(elderId);

        // 获取联系人的地址列表
        List<UserAddressDTO> relativeAddresses = userAddressService.getAddresses(relativeId);

        return new ProxyAddressListDTO(elderAddresses, relativeAddresses);
    }

    /**
     * 代发需求
     * 已授权的联系人代替老人发布需求
     * Requirements: 3.1, 3.2, 3.3, 3.4, 4.2, 4.3, 4.4
     */
    @Override
    @Transactional
    public DemandDTO createProxyDemand(Long relativeId, Long elderId, ProxyDemandCreateDTO dto) {
        // Requirements 3.1, 3.2: 验证授权关系
        if (!proxyAuthorizationService.hasValidAuthorization(relativeId, elderId)) {
            throw new BadRequestException("您没有代发需求的授权");
        }

        // 获取老人用户信息
        User elder = userService.getUserById(elderId);

        // Requirements 3.3, 3.4: 检查老人暖龄币余额
        if (elder.getWarmCoin() < dto.getWarmCoinReward()) {
            throw new BadRequestException("老人暖龄币余额不足");
        }

        // 处理地址选择
        String serviceAddress = dto.getServiceAddress();
        Double latitude = dto.getLatitude();
        Double longitude = dto.getLongitude();

        // Requirements 4.2, 4.3: 如果从地址簿选择地址
        if (dto.getAddressId() != null && dto.getAddressSource() != null) {
            Long addressOwnerId = "elder".equals(dto.getAddressSource()) ? elderId : relativeId;
            UserAddressDTO selectedAddress = userAddressService.getAddressById(addressOwnerId, dto.getAddressId());
            
            serviceAddress = selectedAddress.getAddress();
            latitude = selectedAddress.getLatitude();
            longitude = selectedAddress.getLongitude();

            // 更新地址的最后使用时间
            userAddressService.updateLastUsedTime(addressOwnerId, dto.getAddressId());

            // Requirements 4.3: 如果使用联系人地址且选择保存到老人地址簿
            if ("relative".equals(dto.getAddressSource()) && Boolean.TRUE.equals(dto.getSaveToElderAddressBook())) {
                try {
                    UserAddressCreateDTO addressCreateDTO = new UserAddressCreateDTO();
                    addressCreateDTO.setAddress(selectedAddress.getAddress());
                    addressCreateDTO.setLabel(selectedAddress.getLabel());
                    addressCreateDTO.setLatitude(selectedAddress.getLatitude());
                    addressCreateDTO.setLongitude(selectedAddress.getLongitude());
                    userAddressService.createAddress(elderId, addressCreateDTO);
                } catch (Exception e) {
                    // 地址已存在或达到上限时忽略，不影响需求发布
                }
            }
        }

        // Requirements 3.1: 创建需求，老人为发布者
        Demand demand = new Demand();
        demand.setTitle(dto.getTitle());
        demand.setType(dto.getType());
        demand.setDescription(dto.getDescription());
        demand.setServiceAddress(serviceAddress);
        demand.setServiceDate(dto.getServiceDate());
        demand.setServiceTime(dto.getServiceTime());
        demand.setEstimatedDuration(dto.getEstimatedDuration());
        demand.setWarmCoinReward(dto.getWarmCoinReward());
        demand.setStatus(DemandStatus.WAITING);
        demand.setPublisherId(elderId);
        demand.setLatitude(latitude);
        demand.setLongitude(longitude);
        // Requirements 5.1, 5.2: 记录代发创建者
        demand.setProxyCreatorId(relativeId);

        Demand savedDemand = demandRepository.save(demand);

        // Requirements 3.3: 扣除老人的暖龄币
        userService.updateWarmCoin(elderId, -dto.getWarmCoinReward());

        // 创建暖龄币记录
        WarmCoinRecord record = new WarmCoinRecord();
        record.setUserId(elderId);
        record.setDemandId(savedDemand.getId());
        record.setAmount(-dto.getWarmCoinReward());
        record.setType(WarmCoinRecord.RecordType.DEMAND_SPEND);
        record.setDescription("代发需求：" + dto.getTitle());
        warmCoinRecordService.createRecord(record);

        // Requirements 4.4: 如果是手动输入的新地址，保存到老人地址簿
        if (dto.getAddressId() == null && serviceAddress != null && !serviceAddress.isEmpty()) {
            try {
                UserAddressCreateDTO addressCreateDTO = new UserAddressCreateDTO();
                addressCreateDTO.setAddress(serviceAddress);
                addressCreateDTO.setLatitude(latitude);
                addressCreateDTO.setLongitude(longitude);
                userAddressService.createAddress(elderId, addressCreateDTO);
            } catch (Exception e) {
                // 地址已存在或达到上限时忽略，不影响需求发布
            }
        }

        // 发送系统消息通知老人
        User relative = userService.getUserById(relativeId);
        String relativeName = relative.getRealName() != null ? relative.getRealName() : relative.getUsername();
        String title = "代发需求通知";
        String content = String.format("%s 已代您发布需求：%s", relativeName, dto.getTitle());
        messageService.sendSystemMessage(title, content, elderId);

        return convertToDTO(savedDemand);
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

        return dto;
    }
}
