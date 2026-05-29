package com.example.prreview.repository;

import com.example.prreview.entity.ReviewTask;
import com.example.prreview.enums.TaskStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewTaskRepository extends JpaRepository<ReviewTask, Long> {

    List<ReviewTask> findByRepoOwnerAndRepoNameAndPrNumber(String repoOwner, String repoName, Integer prNumber);

    List<ReviewTask> findByStatus(TaskStatus status);
}
