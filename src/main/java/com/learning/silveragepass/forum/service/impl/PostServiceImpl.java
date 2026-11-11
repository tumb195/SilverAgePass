package com.learning.silveragepass.forum.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.exception.BadRequestException;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.forum.dto.PostCreateDTO;
import com.learning.silveragepass.forum.dto.PostDTO;
import com.learning.silveragepass.forum.dto.PostQueryDTO;
import com.learning.silveragepass.forum.entity.*;
import com.learning.silveragepass.forum.repository.*;
import com.learning.silveragepass.forum.service.PostService;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 帖子服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostFavoriteRepository postFavoriteRepository;
    private final PostViewRepository postViewRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional
    public PostDTO createPost(PostCreateDTO postCreateDTO, MultipartFile[] images, MultipartFile video, Long userId) {
        log.info("用户 {} 开始发布帖子", userId);
        
        // 验证图片数量
        if (images != null && images.length > 9) {
            throw new BadRequestException("图片数量不能超过9张");
        }
        
        // 创建帖子实体
        Post post = new Post();
        post.setUserId(userId);
        post.setType(postCreateDTO.getType());
        post.setTitle(postCreateDTO.getTitle());
        post.setContent(postCreateDTO.getContent());
        
        // 处理图片上传
        if (images != null && images.length > 0) {
            List<String> imageUrls = new ArrayList<>();
            for (MultipartFile image : images) {
                String imageUrl = fileStorageService.storeFile(image, "forum/images");
                imageUrls.add(imageUrl);
            }
            try {
                post.setImages(objectMapper.writeValueAsString(imageUrls));
            } catch (JsonProcessingException e) {
                throw new BadRequestException("图片URL序列化失败");
            }
        }
        
        // 处理视频上传
        if (video != null) {
            String videoUrl = fileStorageService.storeFile(video, "forum/videos");
            post.setVideoUrl(videoUrl);
        }
        
        // 保存帖子
        post = postRepository.save(post);
        log.info("帖子发布成功，ID: {}", post.getId());
        
        // 转换为DTO
        return convertToDTO(post, userId);
    }
    
    @Override
    public PageResponse<PostDTO> getPosts(PostQueryDTO queryDTO, Long userId) {
        Pageable pageable = PageRequest.of(queryDTO.getPage(), queryDTO.getSize());
        Page<Post> postPage;
        
        // 根据查询条件查询帖子
        if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().trim().isEmpty()) {
            // 关键词搜索
            if (queryDTO.getType() != null) {
                postPage = postRepository.searchByTypeAndKeyword(
                    queryDTO.getType(), queryDTO.getKeyword(), PostStatus.NORMAL, pageable);
            } else {
                postPage = postRepository.searchByKeyword(
                    queryDTO.getKeyword(), PostStatus.NORMAL, pageable);
            }
        } else {
            // 普通查询
            if (queryDTO.getType() != null) {
                postPage = postRepository.findByTypeAndStatusOrderByCreateTimeDesc(
                    queryDTO.getType(), PostStatus.NORMAL, pageable);
            } else {
                postPage = postRepository.findByStatusOrderByCreateTimeDesc(
                    PostStatus.NORMAL, pageable);
            }
        }
        
        // 转换为DTO
        List<PostDTO> postDTOs = convertToDTOList(postPage.getContent(), userId);
        
        return new PageResponse<>(
            postDTOs,
            postPage.getTotalElements(),
            postPage.getTotalPages(),
            postPage.getNumber(),
            postPage.getSize()
        );
    }
    
    @Override
    @Transactional
    public PostDTO getPostById(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));
        
        if (post.getStatus() == PostStatus.DELETED) {
            throw new ResourceNotFoundException("帖子已被删除");
        }
        
        // 异步记录浏览（实际应该用异步方式）
        recordView(postId, userId);
        
        return convertToDTO(post, userId);
    }
    
    @Override
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));
        
        // 验证是否是帖子作者
        if (!post.getUserId().equals(userId)) {
            throw new BadRequestException("无权删除他人的帖子");
        }
        
        // 软删除
        post.setStatus(PostStatus.DELETED);
        postRepository.save(post);
        
        log.info("用户 {} 删除了帖子 {}", userId, postId);
    }
    
    @Override
    public PageResponse<PostDTO> getMyPosts(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findByUserIdAndStatusOrderByCreateTimeDesc(
            userId, PostStatus.NORMAL, pageable);
        
        List<PostDTO> postDTOs = convertToDTOList(postPage.getContent(), userId);
        
        return new PageResponse<>(
            postDTOs,
            postPage.getTotalElements(),
            postPage.getTotalPages(),
            postPage.getNumber(),
            postPage.getSize()
        );
    }
    
    @Override
    public PageResponse<PostDTO> getUserPosts(Long userId, Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Post> postPage = postRepository.findByUserIdAndStatusOrderByCreateTimeDesc(
            userId, PostStatus.NORMAL, pageable);
        
        // 使用当前用户ID来判断点赞和收藏状态
        List<PostDTO> postDTOs = convertToDTOList(postPage.getContent(), currentUserId);
        
        return new PageResponse<>(
            postDTOs,
            postPage.getTotalElements(),
            postPage.getTotalPages(),
            postPage.getNumber(),
            postPage.getSize()
        );
    }
    
    @Override
    public PageResponse<PostDTO> getMyFavorites(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PostFavorite> favoritePage = postFavoriteRepository.findByUserIdOrderByCreateTimeDesc(
            userId, pageable);
        
        // 获取收藏的帖子ID列表
        List<Long> postIds = favoritePage.getContent().stream()
            .map(PostFavorite::getPostId)
            .collect(Collectors.toList());
        
        if (postIds.isEmpty()) {
            return new PageResponse<>(new ArrayList<>(), 0L, 0, page, size);
        }
        
        // 查询帖子详情
        List<Post> posts = postRepository.findAllById(postIds);
        
        // 按收藏时间排序
        Map<Long, Post> postMap = posts.stream()
            .collect(Collectors.toMap(Post::getId, p -> p));
        
        List<Post> sortedPosts = favoritePage.getContent().stream()
            .map(f -> postMap.get(f.getPostId()))
            .filter(Objects::nonNull)
            .filter(p -> p.getStatus() == PostStatus.NORMAL)
            .collect(Collectors.toList());
        
        List<PostDTO> postDTOs = convertToDTOList(sortedPosts, userId);
        
        return new PageResponse<>(
            postDTOs,
            favoritePage.getTotalElements(),
            favoritePage.getTotalPages(),
            favoritePage.getNumber(),
            favoritePage.getSize()
        );
    }
    
    @Override
    @Transactional
    public void recordView(Long postId, Long userId) {
        // 创建浏览记录
        PostView postView = new PostView(postId, userId);
        postViewRepository.save(postView);
        
        // 更新帖子浏览数（实际应该用定时任务批量更新）
        Post post = postRepository.findById(postId).orElse(null);
        if (post != null) {
            post.setViewCount(post.getViewCount() + 1);
            postRepository.save(post);
        }
    }
    
    /**
     * 转换Post为PostDTO
     */
    private PostDTO convertToDTO(Post post, Long currentUserId) {
        PostDTO dto = new PostDTO();
        dto.setId(post.getId());
        dto.setUserId(post.getUserId());
        dto.setType(post.getType());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setVideoUrl(post.getVideoUrl());
        dto.setLikeCount(post.getLikeCount());
        dto.setCommentCount(post.getCommentCount());
        dto.setFavoriteCount(post.getFavoriteCount());
        dto.setViewCount(post.getViewCount());
        dto.setCreateTime(post.getCreateTime());
        
        // 解析图片URL列表
        if (post.getImages() != null && !post.getImages().isEmpty()) {
            try {
                List<String> images = objectMapper.readValue(post.getImages(), new TypeReference<List<String>>() {});
                dto.setImages(images);
            } catch (JsonProcessingException e) {
                dto.setImages(new ArrayList<>());
            }
        } else {
            dto.setImages(new ArrayList<>());
        }
        
        // 获取用户信息
        User user = userRepository.findById(post.getUserId()).orElse(null);
        if (user != null) {
            dto.setUsername(user.getUsername());
            dto.setAvatar(user.getAvatar());
        }
        
        // 判断当前用户是否已点赞和收藏
        if (currentUserId != null) {
            dto.setIsLiked(postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUserId));
            dto.setIsFavorited(postFavoriteRepository.existsByPostIdAndUserId(post.getId(), currentUserId));
        } else {
            dto.setIsLiked(false);
            dto.setIsFavorited(false);
        }
        
        return dto;
    }
    
    /**
     * 批量转换Post为PostDTO
     */
    private List<PostDTO> convertToDTOList(List<Post> posts, Long currentUserId) {
        if (posts.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 批量获取用户信息
        Set<Long> userIds = posts.stream().map(Post::getUserId).collect(Collectors.toSet());
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        
        // 批量查询点赞和收藏状态
        List<Long> postIds = posts.stream().map(Post::getId).collect(Collectors.toList());
        Set<Long> likedPostIds = new HashSet<>();
        Set<Long> favoritedPostIds = new HashSet<>();
        
        if (currentUserId != null) {
            List<PostLike> likes = postLikeRepository.findByUserIdAndPostIdIn(currentUserId, postIds);
            likedPostIds = likes.stream().map(PostLike::getPostId).collect(Collectors.toSet());
            
            List<PostFavorite> favorites = postFavoriteRepository.findByUserIdAndPostIdIn(currentUserId, postIds);
            favoritedPostIds = favorites.stream().map(PostFavorite::getPostId).collect(Collectors.toSet());
        }
        
        // 转换为DTO
        List<PostDTO> dtos = new ArrayList<>();
        for (Post post : posts) {
            PostDTO dto = new PostDTO();
            dto.setId(post.getId());
            dto.setUserId(post.getUserId());
            dto.setType(post.getType());
            dto.setTitle(post.getTitle());
            dto.setContent(post.getContent());
            dto.setVideoUrl(post.getVideoUrl());
            dto.setLikeCount(post.getLikeCount());
            dto.setCommentCount(post.getCommentCount());
            dto.setFavoriteCount(post.getFavoriteCount());
            dto.setViewCount(post.getViewCount());
            dto.setCreateTime(post.getCreateTime());
            
            // 解析图片
            if (post.getImages() != null && !post.getImages().isEmpty()) {
                try {
                    List<String> images = objectMapper.readValue(post.getImages(), new TypeReference<List<String>>() {});
                    dto.setImages(images);
                } catch (JsonProcessingException e) {
                    dto.setImages(new ArrayList<>());
                }
            } else {
                dto.setImages(new ArrayList<>());
            }
            
            // 设置用户信息
            User user = userMap.get(post.getUserId());
            if (user != null) {
                dto.setUsername(user.getUsername());
                dto.setAvatar(user.getAvatar());
            }
            
            // 设置点赞和收藏状态
            dto.setIsLiked(likedPostIds.contains(post.getId()));
            dto.setIsFavorited(favoritedPostIds.contains(post.getId()));
            
            dtos.add(dto);
        }
        
        return dtos;
    }
}

