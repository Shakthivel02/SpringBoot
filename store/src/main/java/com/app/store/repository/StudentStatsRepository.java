package com.app.store.repository;

import com.app.store.model.entity.StudentStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentStatsRepository extends JpaRepository<StudentStats, Long> {
    Optional<StudentStats> findByStudentId(String studentId);
}
