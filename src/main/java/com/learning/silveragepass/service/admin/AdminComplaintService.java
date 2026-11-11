package com.learning.silveragepass.service.admin;

import com.learning.silveragepass.dto.PageResponse;
import com.learning.silveragepass.dto.admin.ComplaintDTO;
import com.learning.silveragepass.entity.AdminAccount;
import com.learning.silveragepass.entity.Complaint;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.exception.ResourceNotFoundException;
import com.learning.silveragepass.repository.AdminAccountRepository;
import com.learning.silveragepass.repository.ComplaintRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminComplaintService {
    
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final AdminAccountRepository adminAccountRepository;
    
    /**
     * 获取投诉列表
     */
    public PageResponse<ComplaintDTO> getComplaintList(int page, int pageSize, String search) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Complaint> complaintPage = complaintRepository.findBySearch(search, pageable);
        
        List<ComplaintDTO> content = complaintPage.getContent().stream()
                .map(this::convertToComplaintDTO)
                .collect(Collectors.toList());
        
        return new PageResponse<>(
                content,
                complaintPage.getTotalElements(),
                complaintPage.getTotalPages(),
                complaintPage.getNumber(),
                complaintPage.getSize(),
                complaintPage.isFirst(),
                complaintPage.isLast(),
                complaintPage.isEmpty()
        );
    }
    
    /**
     * 获取投诉详情
     */
    public ComplaintDTO getComplaintDetail(Long id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("投诉不存在"));
        return convertToComplaintDTO(complaint);
    }
    
    /**
     * 处理投诉
     */
    @Transactional
    public String handleComplaint(Long id, String result) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("投诉不存在"));
        
        // 获取当前管理员
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        AdminAccount admin = adminAccountRepository.findByUsername(username)
                .orElse(null);
        
        complaint.setStatus("resolved");
        complaint.setResult(result);
        complaint.setHandleTime(LocalDateTime.now());
        if (admin != null) {
            complaint.setHandlerId(admin.getId());
        }
        
        complaintRepository.save(complaint);
        return "投诉已处理";
    }
    
    private ComplaintDTO convertToComplaintDTO(Complaint complaint) {
        ComplaintDTO dto = new ComplaintDTO();
        dto.setId(complaint.getId());
        dto.setTitle(complaint.getTitle());
        dto.setDescription(complaint.getDescription());
        dto.setTime(complaint.getCreateTime());
        dto.setStatus(complaint.getStatus());
        dto.setHandleTime(complaint.getHandleTime());
        dto.setResult(complaint.getResult());
        
        // 获取投诉人信息
        if (complaint.getAuthorId() != null) {
            userRepository.findById(complaint.getAuthorId()).ifPresent(user -> {
                dto.setAuthor(user.getUsername());
                dto.setPhone(user.getPhone());
            });
        }
        
        // 获取处理人信息
        if (complaint.getHandlerId() != null) {
            adminAccountRepository.findById(complaint.getHandlerId()).ifPresent(admin -> {
                dto.setHandler(admin.getUsername());
            });
        }
        
        // 处理图片
        if (complaint.getImages() != null && !complaint.getImages().isEmpty()) {
            List<String> imageList = Arrays.asList(complaint.getImages().split(","));
            dto.setImages(imageList);
            dto.setImage(imageList.isEmpty() ? null : imageList.get(0));
        } else {
            dto.setImages(Collections.emptyList());
        }
        
        return dto;
    }
}
