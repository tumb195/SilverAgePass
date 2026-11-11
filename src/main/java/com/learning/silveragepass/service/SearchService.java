package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.SearchRequestDTO;
import com.learning.silveragepass.dto.SearchResultDTO;

/**
 * 搜索服务接口
 */
public interface SearchService {
    
    /**
     * 统一搜索接口
     * @param request 搜索请求
     * @return 搜索结果
     */
    SearchResultDTO search(SearchRequestDTO request);
}

