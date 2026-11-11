package com.learning.silveragepass.entity;

/**
 * 定位授权状态枚举
 * Requirements: 5.1, 5.3, 5.4, 5.5
 */
public enum LocationAuthStatus {
    /**
     * 待审批 - 亲属已申请，等待老人审批
     */
    PENDING,
    
    /**
     * 已批准 - 老人已批准定位授权
     */
    APPROVED,
    
    /**
     * 已拒绝 - 老人已拒绝定位授权
     */
    REJECTED,
    
    /**
     * 已撤销 - 老人已撤销之前批准的授权
     */
    REVOKED
}
