package com.learning.silveragepass.forum.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.entity.Message;
import com.learning.silveragepass.entity.MessageType;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.exception.BadRequestException;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.forum.dto.*;
import com.learning.silveragepass.forum.entity.*;
import com.learning.silveragepass.forum.repository.*;
import com.learning.silveragepass.forum.service.CommentService;
import com.learning.silveragepass.repository.MessageRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.service.FileStorageService;
import com.learning.silveragepass.service.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论服务实现类
 * Requirements: 7.1, 7.2, 7.3
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {
    
    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentReplyRepository commentReplyRepository;
    private final ReplyLikeRepository replyLikeRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    
    @Autowired(required = false)
    private NotificationEventPublisher notificationEventPublisher;
    
    @Override
    @Transactional
    public CommentDTO createComment(Long postId, CommentCreateDTO commentCreateDTO, 
                                   MultipartFile[] images, Long userId) {
        log.info("用户 {} 开始评论帖子 {}", userId, postId);
        
        // 验证帖子存在
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));
        
        if (post.getStatus() == PostStatus.DELETED) {
            throw new BadRequestException("帖子已被删除，无法评论");
        }
        
        // 验证图片数量
        if (images != null && images.length > 3) {
            throw new BadRequestException("评论图片数量不能超过3张");
        }
        
        // 创建评论实体
        PostComment comment = new PostComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(commentCreateDTO.getContent());
        
        // 处理图片上传
        if (images != null && images.length > 0) {
            List<String> imageUrls = new ArrayList<>();
            for (MultipartFile image : images) {
                String imageUrl = fileStorageService.storeFile(image, "forum/comments");
                imageUrls.add(imageUrl);
            }
            try {
                comment.setImages(objectMapper.writeValueAsString(imageUrls));
            } catch (JsonProcessingException e) {
                throw new BadRequestException("图片URL序列化失败");
            }
        }
        
        // 保存评论
        comment = postCommentRepository.save(comment);
        
        // 更新帖子评论数
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);
        
        // 发送消息通知给帖子作者（如果不是自己评论自己的帖子）
        if (!post.getUserId().equals(userId)) {
            sendCommentNotification(post, comment, userId);
        }
        
        log.info("评论发表成功，ID: {}", comment.getId());
        
        // 转换为DTO
        return convertToDTO(comment, userId);
    }
    
    @Override
    public PageResponse<CommentDTO> getComments(Long postId, Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PostComment> commentPage = postCommentRepository.findByPostIdAndStatusOrderByCreateTimeDesc(
            postId, PostStatus.NORMAL, pageable);
        
        List<CommentDTO> commentDTOs = convertToDTOList(commentPage.getContent(), userId);
        
        return new PageResponse<>(
            commentDTOs,
            commentPage.getTotalElements(),
            commentPage.getTotalPages(),
            commentPage.getNumber(),
            commentPage.getSize()
        );
    }
    
    @Override
    @Transactional
    public ReplyDTO replyComment(Long commentId, ReplyCreateDTO replyCreateDTO, Long userId) {
        log.info("用户 {} 开始回复评论 {}", userId, commentId);
        
        // 验证评论存在
        PostComment comment = postCommentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("评论不存在"));
        
        if (comment.getStatus() == PostStatus.DELETED) {
            throw new BadRequestException("评论已被删除，无法回复");
        }
        
        // 验证被回复用户存在
        User replyToUser = userRepository.findById(replyCreateDTO.getReplyToUserId())
            .orElseThrow(() -> new ResourceNotFoundException("被回复用户不存在"));
        
        // 创建回复实体
        CommentReply reply = new CommentReply();
        reply.setCommentId(commentId);
        reply.setUserId(userId);
        reply.setReplyToUserId(replyCreateDTO.getReplyToUserId());
        reply.setContent(replyCreateDTO.getContent());
        
        // 保存回复
        reply = commentReplyRepository.save(reply);
        
        // 更新评论回复数
        comment.setReplyCount(comment.getReplyCount() + 1);
        postCommentRepository.save(comment);
        
        // 发送消息通知给被回复用户（如果不是自己回复自己）
        if (!reply.getReplyToUserId().equals(userId)) {
            sendReplyNotification(comment, reply, userId);
        }
        
        log.info("回复发表成功，ID: {}", reply.getId());
        
        // 转换为DTO
        return convertReplyToDTO(reply);
    }
    
    @Override
    @Transactional
    public LikeResultDTO toggleCommentLike(Long commentId, Long userId) {
        // 验证评论存在
        PostComment comment = postCommentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("评论不存在"));
        
        // 查询是否已点赞
        Optional<CommentLike> existingLike = commentLikeRepository.findByCommentIdAndUserId(commentId, userId);
        
        boolean isLiked;
        if (existingLike.isPresent()) {
            // 取消点赞
            commentLikeRepository.delete(existingLike.get());
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
            isLiked = false;
            log.info("用户 {} 取消点赞评论 {}", userId, commentId);
        } else {
            // 点赞
            CommentLike commentLike = new CommentLike(commentId, userId);
            commentLikeRepository.save(commentLike);
            comment.setLikeCount(comment.getLikeCount() + 1);
            isLiked = true;
            log.info("用户 {} 点赞评论 {}", userId, commentId);
        }
        
        postCommentRepository.save(comment);
        
        return new LikeResultDTO(isLiked, comment.getLikeCount());
    }
    
    @Override
    public PageResponse<MyCommentDTO> getMyComments(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PostComment> commentPage = postCommentRepository.findByUserIdAndStatusOrderByCreateTimeDesc(
            userId, PostStatus.NORMAL, pageable);
        
        // 获取帖子信息
        List<Long> postIds = commentPage.getContent().stream()
            .map(PostComment::getPostId)
            .distinct()
            .collect(Collectors.toList());
        
        Map<Long, Post> postMap = new HashMap<>();
        if (!postIds.isEmpty()) {
            List<Post> posts = postRepository.findAllById(postIds);
            postMap = posts.stream().collect(Collectors.toMap(Post::getId, p -> p));
        }
        
        // 转换为DTO
        List<MyCommentDTO> commentDTOs = new ArrayList<>();
        for (PostComment comment : commentPage.getContent()) {
            MyCommentDTO dto = new MyCommentDTO();
            dto.setId(comment.getId());
            dto.setPostId(comment.getPostId());
            dto.setContent(comment.getContent());
            dto.setLikeCount(comment.getLikeCount());
            dto.setReplyCount(comment.getReplyCount());
            dto.setCreateTime(comment.getCreateTime());
            
            // 解析图片
            if (comment.getImages() != null && !comment.getImages().isEmpty()) {
                try {
                    List<String> images = objectMapper.readValue(comment.getImages(), new TypeReference<List<String>>() {});
                    dto.setImages(images);
                } catch (JsonProcessingException e) {
                    dto.setImages(new ArrayList<>());
                }
            } else {
                dto.setImages(new ArrayList<>());
            }
            
            // 设置帖子标题
            Post post = postMap.get(comment.getPostId());
            if (post != null) {
                dto.setPostTitle(post.getTitle());
            }
            
            commentDTOs.add(dto);
        }
        
        return new PageResponse<>(
            commentDTOs,
            commentPage.getTotalElements(),
            commentPage.getTotalPages(),
            commentPage.getNumber(),
            commentPage.getSize()
        );
    }
    
    @Override
    public PageResponse<ReplyDTO> getCommentReplies(Long commentId, Long userId, int page, int size) {
        log.info("获取评论 {} 的回复列表，页码: {}, 每页数量: {}", commentId, page, size);
        
        // 验证评论存在
        PostComment comment = postCommentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException("评论不存在"));
        
        // 分页查询回复
        Pageable pageable = PageRequest.of(page, size);
        Page<CommentReply> replyPage = commentReplyRepository.findByCommentIdAndStatusOrderByCreateTimeAsc(
            commentId, PostStatus.NORMAL, pageable);
        
        // 转换为DTO
        List<ReplyDTO> replyDTOs = convertReplyToDTOList(replyPage.getContent());
        
        return new PageResponse<>(
            replyDTOs,
            replyPage.getTotalElements(),
            replyPage.getTotalPages(),
            replyPage.getNumber(),
            replyPage.getSize()
        );
    }
    
    @Override
    @Transactional
    public LikeResultDTO toggleReplyLike(Long replyId, Long userId) {
        // 验证回复存在
        CommentReply reply = commentReplyRepository.findById(replyId)
            .orElseThrow(() -> new ResourceNotFoundException("回复不存在"));
        
        // 查询是否已点赞
        Optional<ReplyLike> existingLike = replyLikeRepository.findByReplyIdAndUserId(replyId, userId);
        
        boolean isLiked;
        if (existingLike.isPresent()) {
            // 取消点赞
            replyLikeRepository.delete(existingLike.get());
            reply.setLikeCount(Math.max(0, reply.getLikeCount() - 1));
            isLiked = false;
            log.info("用户 {} 取消点赞回复 {}", userId, replyId);
        } else {
            // 点赞
            ReplyLike replyLike = new ReplyLike(replyId, userId);
            replyLikeRepository.save(replyLike);
            reply.setLikeCount(reply.getLikeCount() + 1);
            isLiked = true;
            log.info("用户 {} 点赞回复 {}", userId, replyId);
        }
        
        commentReplyRepository.save(reply);
        
        return new LikeResultDTO(isLiked, reply.getLikeCount());
    }
    
    /**
     * 发送评论通知
     * Requirements: 7.1
     */
    private void sendCommentNotification(Post post, PostComment comment, Long commentUserId) {
        // 使用新的通知事件发布器 - Requirements: 7.1
        if (notificationEventPublisher != null) {
            notificationEventPublisher.publishForumComment(
                    post.getId(),
                    post.getUserId(),
                    commentUserId,
                    post.getTitle(),
                    comment.getContent()
            );
            return;
        }
        
        // 降级到旧的消息系统
        User commentUser = userRepository.findById(commentUserId).orElse(null);
        if (commentUser == null) {
            return;
        }
        
        Message message = new Message();
        message.setTitle("您的帖子收到新评论");
        message.setContent(String.format("用户\"%s\"评论了您的帖子\"%s\"：%s", 
            commentUser.getUsername(), post.getTitle(), comment.getContent()));
        message.setType(MessageType.FORUM_COMMENT);
        message.setSenderId(commentUserId);
        message.setReceiverId(post.getUserId());
        message.setRelatedDemandId(post.getId()); // 复用这个字段存储帖子ID
        
        messageRepository.save(message);
        log.info("发送评论通知给用户 {}", post.getUserId());
    }
    
    /**
     * 发送回复通知
     * Requirements: 7.2
     */
    private void sendReplyNotification(PostComment comment, CommentReply reply, Long replyUserId) {
        Post post = postRepository.findById(comment.getPostId()).orElse(null);
        
        // 使用新的通知事件发布器 - Requirements: 7.2
        if (notificationEventPublisher != null && post != null) {
            notificationEventPublisher.publishForumReply(
                    comment.getId(),
                    reply.getReplyToUserId(),
                    replyUserId,
                    post.getTitle(),
                    reply.getContent()
            );
            return;
        }
        
        // 降级到旧的消息系统
        User replyUser = userRepository.findById(replyUserId).orElse(null);
        
        if (replyUser == null || post == null) {
            return;
        }
        
        Message message = new Message();
        message.setTitle("您的评论收到新回复");
        message.setContent(String.format("用户\"%s\"回复了您在帖子\"%s\"下的评论：%s", 
            replyUser.getUsername(), post.getTitle(), reply.getContent()));
        message.setType(MessageType.FORUM_REPLY);
        message.setSenderId(replyUserId);
        message.setReceiverId(reply.getReplyToUserId());
        message.setRelatedDemandId(comment.getId()); // 复用这个字段存储评论ID
        
        messageRepository.save(message);
        log.info("发送回复通知给用户 {}", reply.getReplyToUserId());
    }
    
    /**
     * 转换Comment为CommentDTO
     */
    private CommentDTO convertToDTO(PostComment comment, Long currentUserId) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setPostId(comment.getPostId());
        dto.setUserId(comment.getUserId());
        dto.setContent(comment.getContent());
        dto.setLikeCount(comment.getLikeCount());
        dto.setReplyCount(comment.getReplyCount());
        dto.setCreateTime(comment.getCreateTime());
        
        // 解析图片
        if (comment.getImages() != null && !comment.getImages().isEmpty()) {
            try {
                List<String> images = objectMapper.readValue(comment.getImages(), new TypeReference<List<String>>() {});
                dto.setImages(images);
            } catch (JsonProcessingException e) {
                dto.setImages(new ArrayList<>());
            }
        } else {
            dto.setImages(new ArrayList<>());
        }
        
        // 获取用户信息
        User user = userRepository.findById(comment.getUserId()).orElse(null);
        if (user != null) {
            dto.setUsername(user.getUsername());
            dto.setAvatar(user.getAvatar());
        }
        
        // 判断当前用户是否已点赞
        if (currentUserId != null) {
            dto.setIsLiked(commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), currentUserId));
        } else {
            dto.setIsLiked(false);
        }
        
        // 获取回复列表
        List<CommentReply> replies = commentReplyRepository.findByCommentIdAndStatusOrderByCreateTimeAsc(
            comment.getId(), PostStatus.NORMAL);
        dto.setReplies(convertReplyToDTOList(replies));
        
        return dto;
    }
    
    /**
     * 批量转换Comment为CommentDTO
     */
    private List<CommentDTO> convertToDTOList(List<PostComment> comments, Long currentUserId) {
        if (comments.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 批量获取用户信息
        Set<Long> userIds = comments.stream().map(PostComment::getUserId).collect(Collectors.toSet());
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        
        // 批量查询点赞状态
        List<Long> commentIds = comments.stream().map(PostComment::getId).collect(Collectors.toList());
        Set<Long> likedCommentIds = new HashSet<>();
        
        if (currentUserId != null) {
            List<CommentLike> likes = commentLikeRepository.findByUserIdAndCommentIdIn(currentUserId, commentIds);
            likedCommentIds = likes.stream().map(CommentLike::getCommentId).collect(Collectors.toSet());
        }
        
        // 批量获取回复
        List<CommentReply> allReplies = commentReplyRepository.findByCommentIdInAndStatusOrderByCreateTimeAsc(
            commentIds, PostStatus.NORMAL);
        Map<Long, List<CommentReply>> replyMap = allReplies.stream()
            .collect(Collectors.groupingBy(CommentReply::getCommentId));
        
        // 获取回复用户信息
        Set<Long> replyUserIds = allReplies.stream()
            .flatMap(r -> Arrays.asList(r.getUserId(), r.getReplyToUserId()).stream())
            .collect(Collectors.toSet());
        List<User> replyUsers = userRepository.findAllById(replyUserIds);
        Map<Long, User> replyUserMap = replyUsers.stream().collect(Collectors.toMap(User::getId, u -> u));
        
        // 转换为DTO
        List<CommentDTO> dtos = new ArrayList<>();
        for (PostComment comment : comments) {
            CommentDTO dto = new CommentDTO();
            dto.setId(comment.getId());
            dto.setPostId(comment.getPostId());
            dto.setUserId(comment.getUserId());
            dto.setContent(comment.getContent());
            dto.setLikeCount(comment.getLikeCount());
            dto.setReplyCount(comment.getReplyCount());
            dto.setCreateTime(comment.getCreateTime());
            
            // 解析图片
            if (comment.getImages() != null && !comment.getImages().isEmpty()) {
                try {
                    List<String> images = objectMapper.readValue(comment.getImages(), new TypeReference<List<String>>() {});
                    dto.setImages(images);
                } catch (JsonProcessingException e) {
                    dto.setImages(new ArrayList<>());
                }
            } else {
                dto.setImages(new ArrayList<>());
            }
            
            // 设置用户信息
            User user = userMap.get(comment.getUserId());
            if (user != null) {
                dto.setUsername(user.getUsername());
                dto.setAvatar(user.getAvatar());
            }
            
            // 设置点赞状态
            dto.setIsLiked(likedCommentIds.contains(comment.getId()));
            
            // 设置回复列表（只返回前3条）
            List<CommentReply> replies = replyMap.getOrDefault(comment.getId(), new ArrayList<>());
            List<ReplyDTO> replyDTOs = new ArrayList<>();
            int maxReplies = Math.min(3, replies.size()); // 只取前3条回复
            for (int i = 0; i < maxReplies; i++) {
                CommentReply reply = replies.get(i);
                ReplyDTO replyDTO = new ReplyDTO();
                replyDTO.setId(reply.getId());
                replyDTO.setCommentId(reply.getCommentId());
                replyDTO.setUserId(reply.getUserId());
                replyDTO.setReplyToUserId(reply.getReplyToUserId());
                replyDTO.setContent(reply.getContent());
                replyDTO.setLikeCount(reply.getLikeCount());
                replyDTO.setCreateTime(reply.getCreateTime());
                
                User replyUser = replyUserMap.get(reply.getUserId());
                if (replyUser != null) {
                    replyDTO.setUsername(replyUser.getUsername());
                    replyDTO.setAvatar(replyUser.getAvatar());
                }
                
                User replyToUser = replyUserMap.get(reply.getReplyToUserId());
                if (replyToUser != null) {
                    replyDTO.setReplyToUsername(replyToUser.getUsername());
                }
                
                replyDTOs.add(replyDTO);
            }
            dto.setReplies(replyDTOs);
            
            dtos.add(dto);
        }
        
        return dtos;
    }
    
    /**
     * 转换Reply为ReplyDTO
     */
    private ReplyDTO convertReplyToDTO(CommentReply reply) {
        ReplyDTO dto = new ReplyDTO();
        dto.setId(reply.getId());
        dto.setCommentId(reply.getCommentId());
        dto.setUserId(reply.getUserId());
        dto.setReplyToUserId(reply.getReplyToUserId());
        dto.setContent(reply.getContent());
        dto.setLikeCount(reply.getLikeCount());
        dto.setCreateTime(reply.getCreateTime());
        
        // 获取用户信息
        User user = userRepository.findById(reply.getUserId()).orElse(null);
        if (user != null) {
            dto.setUsername(user.getUsername());
            dto.setAvatar(user.getAvatar());
        }
        
        User replyToUser = userRepository.findById(reply.getReplyToUserId()).orElse(null);
        if (replyToUser != null) {
            dto.setReplyToUsername(replyToUser.getUsername());
        }
        
        return dto;
    }
    
    /**
     * 批量转换Reply为ReplyDTO
     */
    private List<ReplyDTO> convertReplyToDTOList(List<CommentReply> replies) {
        if (replies.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取所有用户ID
        Set<Long> userIds = replies.stream()
            .flatMap(r -> Arrays.asList(r.getUserId(), r.getReplyToUserId()).stream())
            .collect(Collectors.toSet());
        
        List<User> users = userRepository.findAllById(userIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        
        // 转换为DTO
        List<ReplyDTO> dtos = new ArrayList<>();
        for (CommentReply reply : replies) {
            ReplyDTO dto = new ReplyDTO();
            dto.setId(reply.getId());
            dto.setCommentId(reply.getCommentId());
            dto.setUserId(reply.getUserId());
            dto.setReplyToUserId(reply.getReplyToUserId());
            dto.setContent(reply.getContent());
            dto.setLikeCount(reply.getLikeCount());
            dto.setCreateTime(reply.getCreateTime());
            
            User user = userMap.get(reply.getUserId());
            if (user != null) {
                dto.setUsername(user.getUsername());
                dto.setAvatar(user.getAvatar());
            }
            
            User replyToUser = userMap.get(reply.getReplyToUserId());
            if (replyToUser != null) {
                dto.setReplyToUsername(replyToUser.getUsername());
            }
            
            dtos.add(dto);
        }
        
        return dtos;
    }
}

