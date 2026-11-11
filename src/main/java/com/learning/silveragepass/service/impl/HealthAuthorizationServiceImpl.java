package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.proxy.ProxyAuthorizationDTO;
import com.learning.silveragepass.entity.ProxyAuthorizationStatus;
import com.learning.silveragepass.entity.Relative;
import com.learning.silveragepass.entity.RelativeStatus;
import com.learning.silveragepass.repository.RelativeRepository;
import com.learning.silveragepass.service.HealthAuthorizationService;
import com.learning.silveragepass.service.ProxyAuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 健康数据授权服务实现
 * 负责验证用户对健康数据的访问权限
 * 
 * 统一代理授权：使用ProxyAuthorizationService检查授权状态
 * 亲属必须获得老人的明确授权后，才能访问健康数据
 * Requirements: 1.3, 4.1, 4.2
 */
@Service
public class HealthAuthorizationServiceImpl implements HealthAuthorizationService {

    @Autowired
    private RelativeRepository relativeRepository;

    @Autowired
    @Lazy
    private ProxyAuthorizationService proxyAuthorizationService;

    @Override
    public boolean canAccessUserHealthData(Long currentUserId, Long targetUserId) {
        // 用户可以访问自己的健康数据
        if (currentUserId.equals(targetUserId)) {
            return true;
        }
        
        // 检查是否有有效的代理授权（双向检查）
        // 情况1：currentUser是亲属，targetUser是老人
        // 情况2：currentUser是老人，targetUser是亲属
        // Requirements: 1.3, 4.1, 4.2
        return proxyAuthorizationService.hasValidAuthorization(currentUserId, targetUserId)
            || proxyAuthorizationService.hasValidAuthorization(targetUserId, currentUserId);
    }

    @Override
    public boolean isAcceptedRelative(Long userId, Long relativeId) {
        // 检查双向关系：用户可能是关系的发起方或接收方
        // 情况1：userId是发起方，relativeId是接收方
        Optional<Relative> relationship1 = relativeRepository.findByUserIdAndRelativeId(userId, relativeId);
        if (relationship1.isPresent() && relationship1.get().getStatus() == RelativeStatus.ACCEPTED) {
            return true;
        }
        
        // 情况2：relativeId是发起方，userId是接收方
        Optional<Relative> relationship2 = relativeRepository.findByUserIdAndRelativeId(relativeId, userId);
        if (relationship2.isPresent() && relationship2.get().getStatus() == RelativeStatus.ACCEPTED) {
            return true;
        }
        
        return false;
    }

    /**
     * 获取当前用户可访问健康数据的用户ID列表
     * 
     * 修改为返回有代理授权的用户列表，而非仅基于亲属关系
     * Requirements: 3.1, 3.2, 3.3, 3.4
     */
    @Override
    public List<Long> getAccessibleUserIds(Long currentUserId) {
        List<Long> accessibleUserIds = new ArrayList<>();
        
        // 用户可以访问自己的数据
        accessibleUserIds.add(currentUserId);
        
        // 获取当前用户作为亲属获得的授权（可以访问老人数据）
        // Requirements: 3.1, 3.2, 3.3, 3.4
        List<ProxyAuthorizationDTO> asRelative = proxyAuthorizationService.getRelativeAuthorizations(currentUserId);
        for (ProxyAuthorizationDTO auth : asRelative) {
            if (auth.getStatus() == ProxyAuthorizationStatus.APPROVED) {
                accessibleUserIds.add(auth.getElderId());
            }
        }
        
        // 获取当前用户作为老人授予的授权（老人也可以查看亲属帮忙记录的数据）
        List<ProxyAuthorizationDTO> asElder = proxyAuthorizationService.getElderApprovedAuthorizations(currentUserId);
        for (ProxyAuthorizationDTO auth : asElder) {
            accessibleUserIds.add(auth.getRelativeId());
        }
        
        return accessibleUserIds;
    }
}
