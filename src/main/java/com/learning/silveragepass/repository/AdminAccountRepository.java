package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.AdminAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminAccountRepository extends JpaRepository<AdminAccount, Long> {
    
    Optional<AdminAccount> findByUsername(String username);
    
    Optional<AdminAccount> findByPhone(String phone);
    
    Boolean existsByUsername(String username);
    
    Boolean existsByPhone(String phone);
    
    List<AdminAccount> findByApprovalStatus(AdminAccount.ApprovalStatus approvalStatus);
}
