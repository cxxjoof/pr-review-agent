package com.example.prreview.repository;

import com.example.prreview.entity.ReviewFeedback;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewFeedbackRepository extends JpaRepository<ReviewFeedback, Long> {

    @Query("select rf from ReviewFeedback rf where rf.task.id = :taskId order by rf.createdAt desc, rf.id desc")
    List<ReviewFeedback> findByTaskIdOrderByCreatedAtDesc(@Param("taskId") Long taskId);
}
