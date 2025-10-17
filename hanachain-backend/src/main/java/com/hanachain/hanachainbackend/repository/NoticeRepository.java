package com.hanachain.hanachainbackend.repository;

import com.hanachain.hanachainbackend.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    
    // 삭제되지 않은 공지사항만 조회 (최신순)
    @Query("SELECT n FROM Notice n WHERE n.deletedAt IS NULL ORDER BY n.createdAt DESC")
    Page<Notice> findAllActive(Pageable pageable);
    
    // 삭제되지 않은 공지사항 중 ID로 조회
    @Query("SELECT n FROM Notice n WHERE n.id = :id AND n.deletedAt IS NULL")
    Optional<Notice> findByIdActive(Long id);
    
    // 중요 공지사항만 조회
    @Query("SELECT n FROM Notice n WHERE n.isImportant = true AND n.deletedAt IS NULL ORDER BY n.createdAt DESC")
    List<Notice> findImportantNotices(Pageable pageable);
    
    // 최근 공지사항 조회 (삭제되지 않은 것만)
    @Query("SELECT n FROM Notice n WHERE n.deletedAt IS NULL ORDER BY n.createdAt DESC")
    List<Notice> findRecentNotices(Pageable pageable);
}

