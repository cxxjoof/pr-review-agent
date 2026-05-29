package com.example.prreview.repository;

import com.example.prreview.entity.ReviewResult;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewResultRepository extends JpaRepository<ReviewResult, Long> {

    @Query("select r from ReviewResult r where r.task.id = :taskId")
    Optional<ReviewResult> findByTaskId(@Param("taskId") Long taskId);
}
