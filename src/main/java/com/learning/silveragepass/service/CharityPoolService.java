package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.CharityPoolDTO;
import com.learning.silveragepass.dto.DonateResponseDTO;
import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.admin.CharityPoolStatsDTO;
import com.learning.silveragepass.dto.admin.DonationQueryDTO;
import com.learning.silveragepass.dto.admin.DonationRecordDTO;

/**
 * 公益池服务接口
 */
public interface CharityPoolService {

    /**
     * 获取公益池信息
     */
    CharityPoolDTO getPoolInfo();

    /**
     * 用户捐赠暖龄币到公益池
     * @param userId 捐赠用户ID
     * @param amount 捐赠金额
     * @param message 捐赠留言
     * @return 捐赠结果
     */
    DonateResponseDTO donateToPool(Long userId, Integer amount, String message);

    /**
     * 从公益池发放（管理员操作）
     * @param targetUserId 接收用户ID
     * @param amount 发放金额
     * @param operatorId 操作管理员ID
     * @param description 发放说明
     */
    void distribute(Long targetUserId, Integer amount, Long operatorId, String description);

    /**
     * 获取当前余额
     */
    Integer getCurrentBalance();
    
    /**
     * 获取后台公益池统计信息
     * 包含：当前余额、本季度累计捐赠、累计发放、最近捐赠时间
     */
    CharityPoolStatsDTO getAdminStats();
    
    /**
     * 查询捐赠记录列表（支持按姓名/ID、时间范围筛选）
     */
    PageResponse<DonationRecordDTO> queryDonationRecords(DonationQueryDTO query);
}
