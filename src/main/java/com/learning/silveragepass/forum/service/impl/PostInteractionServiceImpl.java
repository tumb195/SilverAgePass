package com.learning.silveragepass.forum.service.impl;

import com.learning.silveragepass.entity.Message;
import com.learning.silveragepass.entity.MessageType;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.forum.dto.LikeResultDTO;
import com.learning.silveragepass.forum.entity.Post;
import com.learning.silveragepass.forum.entity.PostFavorite;
import com.learning.silveragepass.forum.entity.PostLike;
import com.learning.silveragepass.forum.repository.PostFavoriteRepository;
import com.learning.silveragepass.forum.repository.PostLikeRepository;
import com.learning.silveragepass.forum.repository.PostRepository;
import com.learning.silveragepass.forum.service.PostInteractionService;
import com.learning.silveragepass.repository.MessageRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.service.NotificationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 帖子互动服务实现类
 * Requirements: 7.3
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostInteractionServiceImpl implements PostInteractionService {
    
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostFavoriteRepository postFavoriteRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    
    @Autowired(required = false)
    private NotificationEventPublisher notificationEventPublisher;
    
    @Override
    @Transactional
    public LikeResultDTO toggleLike(Long postId, Long userId) {
        // 验证帖子存在
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));
        
        // 查询是否已点赞
        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndUserId(postId, userId);
        
        boolean isLiked;
        if (existingLike.isPresent()) {
            // 取消点赞
            postLikeRepository.delete(existingLike.get());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            isLiked = false;
            log.info("用户 {} 取消点赞帖子 {}", userId, postId);
        } else {
            // 点赞
            PostLike postLike = new PostLike(postId, userId);
            postLikeRepository.save(postLike);
            post.setLikeCount(post.getLikeCount() + 1);
            isLiked = true;
            log.info("用户 {} 点赞帖子 {}", userId, postId);
            
            // 发送点赞通知给帖子作者（如果不是自己点赞自己的帖子）
            if (!post.getUserId().equals(userId)) {
                sendLikeNotification(post, userId);
            }
        }
        
        postRepository.save(post);
        
        return new LikeResultDTO(isLiked, post.getLikeCount());
    }
    
    @Override
    @Transactional
    public LikeResultDTO toggleFavorite(Long postId, Long userId) {
        // 验证帖子存在
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("帖子不存在"));
        
        // 查询是否已收藏
        Optional<PostFavorite> existingFavorite = postFavoriteRepository.findByPostIdAndUserId(postId, userId);
        
        boolean isFavorited;
        if (existingFavorite.isPresent()) {
            // 取消收藏
            postFavoriteRepository.delete(existingFavorite.get());
            post.setFavoriteCount(Math.max(0, post.getFavoriteCount() - 1));
            isFavorited = false;
            log.info("用户 {} 取消收藏帖子 {}", userId, postId);
        } else {
            // 收藏
            PostFavorite postFavorite = new PostFavorite(postId, userId);
            postFavoriteRepository.save(postFavorite);
            post.setFavoriteCount(post.getFavoriteCount() + 1);
            isFavorited = true;
            log.info("用户 {} 收藏帖子 {}", userId, postId);
        }
        
        postRepository.save(post);
        
        return new LikeResultDTO(isFavorited, post.getFavoriteCount());
    }
    
    /**
     * 发送点赞通知
     * Requirements: 7.3
     */
    private void sendLikeNotification(Post post, Long likeUserId) {
        // 使用新的通知事件发布器 - Requirements: 7.3
        if (notificationEventPublisher != null) {
            notificationEventPublisher.publishForumLike(
                    post.getId(),
                    post.getUserId(),
                    likeUserId,
                    "post",
                    post.getTitle()
            );
            return;
        }
        
        // 降级到旧的消息系统
        User likeUser = userRepository.findById(likeUserId).orElse(null);
        if (likeUser == null) {
            return;
        }
        
        Message message = new Message();
        message.setTitle("您的帖子收到新点赞");
        message.setContent(String.format("用户\"%s\"点赞了您的帖子\"%s\"", 
            likeUser.getUsername(), post.getTitle()));
        message.setType(MessageType.FORUM_LIKE);
        message.setSenderId(likeUserId);
        message.setReceiverId(post.getUserId());
        message.setRelatedDemandId(post.getId()); // 复用这个字段存储帖子ID
        
        messageRepository.save(message);
        log.info("发送点赞通知给用户 {}", post.getUserId());
    }
}

