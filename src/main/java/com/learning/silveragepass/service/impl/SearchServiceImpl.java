package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.*;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.forum.entity.Post;
import com.learning.silveragepass.forum.entity.PostStatus;
import com.learning.silveragepass.forum.repository.PostRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 搜索服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {
    
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    
    @Override
    public SearchResultDTO search(SearchRequestDTO request) {
        String keyword = request.getKeyword();
        String type = request.getType();
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return new SearchResultDTO(new ArrayList<>(), 0L, new ArrayList<>(), 0L);
        }
        
        keyword = keyword.trim();
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        
        SearchResultDTO result = new SearchResultDTO();
        
        // 根据类型搜索
        if ("user".equalsIgnoreCase(type)) {
            // 仅搜索用户
            searchUsers(keyword, pageable, result);
        } else if ("post".equalsIgnoreCase(type)) {
            // 仅搜索帖子
            searchPosts(keyword, pageable, result);
        } else {
            // 搜索全部（用户和帖子）
            searchUsers(keyword, pageable, result);
            searchPosts(keyword, pageable, result);
        }
        
        log.info("搜索关键词: {}, 类型: {}, 用户结果数: {}, 帖子结果数: {}", 
            keyword, type, result.getUserTotal(), result.getPostTotal());
        
        return result;
    }
    
    /**
     * 搜索用户
     */
    private void searchUsers(String keyword, Pageable pageable, SearchResultDTO result) {
        Page<User> userPage = userRepository.searchByKeyword(keyword, pageable);
        
        List<UserSearchResultDTO> users = userPage.getContent().stream()
            .map(this::convertToUserSearchResult)
            .collect(Collectors.toList());
        
        result.setUsers(users);
        result.setUserTotal(userPage.getTotalElements());
    }
    
    /**
     * 搜索帖子
     */
    private void searchPosts(String keyword, Pageable pageable, SearchResultDTO result) {
        Page<Post> postPage = postRepository.searchByKeyword(keyword, PostStatus.NORMAL, pageable);
        
        // 获取所有涉及的用户ID
        List<Long> userIds = postPage.getContent().stream()
            .map(Post::getUserId)
            .distinct()
            .collect(Collectors.toList());
        
        // 批量查询用户信息
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userRepository.findAllById(userIds);
            userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        }
        
        // 转换为DTO
        List<PostSearchResultDTO> posts = new ArrayList<>();
        for (Post post : postPage.getContent()) {
            User user = userMap.get(post.getUserId());
            posts.add(convertToPostSearchResult(post, user));
        }
        
        result.setPosts(posts);
        result.setPostTotal(postPage.getTotalElements());
    }
    
    /**
     * 转换用户实体为搜索结果DTO
     */
    private UserSearchResultDTO convertToUserSearchResult(User user) {
        UserSearchResultDTO dto = new UserSearchResultDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRealName(user.getRealName());
        dto.setAvatar(user.getAvatar());
        dto.setPhone(maskPhone(user.getPhone())); // 脱敏处理
        dto.setAge(user.getAge());
        dto.setAddress(user.getAddress());
        return dto;
    }
    
    /**
     * 转换帖子实体为搜索结果DTO
     */
    private PostSearchResultDTO convertToPostSearchResult(Post post, User user) {
        PostSearchResultDTO dto = new PostSearchResultDTO();
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        
        // 内容摘要，最多200字符
        String content = post.getContent();
        if (content != null && content.length() > 200) {
            content = content.substring(0, 200) + "...";
        }
        dto.setContent(content);
        
        dto.setType(post.getType());
        dto.setUserId(post.getUserId());
        
        if (user != null) {
            dto.setUsername(user.getUsername());
            dto.setUserAvatar(user.getAvatar());
        }
        
        dto.setLikeCount(post.getLikeCount());
        dto.setCommentCount(post.getCommentCount());
        dto.setFavoriteCount(post.getFavoriteCount());
        dto.setViewCount(post.getViewCount());
        dto.setCreateTime(post.getCreateTime());
        
        return dto;
    }
    
    /**
     * 手机号脱敏处理
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}

