package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.SearchRequestDTO;
import com.learning.silveragepass.dto.SearchResultDTO;
import com.learning.silveragepass.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 搜索控制器
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "搜索", description = "用户和帖子搜索相关接口")
public class SearchController {
    
    private final SearchService searchService;
    
    /**
     * 统一搜索接口
     */
    @GetMapping
    @Operation(summary = "统一搜索", description = "搜索用户或帖子（通过关键词模糊匹配）")
    public ResponseEntity<ApiResponse<SearchResultDTO>> search(
            @Parameter(description = "搜索关键词", required = true)
            @RequestParam String keyword,
            
            @Parameter(description = "搜索类型：user-用户, post-帖子, all-全部（默认）")
            @RequestParam(required = false, defaultValue = "all") String type,
            
            @Parameter(description = "页码，从0开始")
            @RequestParam(required = false, defaultValue = "0") Integer page,
            
            @Parameter(description = "每页大小")
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        
        SearchRequestDTO request = new SearchRequestDTO();
        request.setKeyword(keyword);
        request.setType(type);
        request.setPage(page);
        request.setSize(size);
        
        SearchResultDTO result = searchService.search(request);
        
        return ResponseEntity.ok(ApiResponse.success("搜索成功", result));
    }
}

