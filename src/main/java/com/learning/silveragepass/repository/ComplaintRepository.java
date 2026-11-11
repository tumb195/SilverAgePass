package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    
    @Query("SELECT c FROM Complaint c WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "c.title LIKE %:search% OR c.description LIKE %:search%)")
    Page<Complaint> findBySearch(@Param("search") String search, Pageable pageable);
    
    long countByStatus(String status);
}
