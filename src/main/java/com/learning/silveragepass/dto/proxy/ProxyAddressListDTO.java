package com.learning.silveragepass.dto.proxy;

import com.learning.silveragepass.dto.UserAddressDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 代发需求可用地址列表响应DTO
 * Requirements: 4.1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyAddressListDTO {

    /**
     * 老人的地址列表
     */
    private List<UserAddressDTO> elderAddresses;

    /**
     * 联系人的地址列表
     */
    private List<UserAddressDTO> relativeAddresses;
}
