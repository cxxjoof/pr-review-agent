package com.example.prreview.repository;

import com.example.prreview.entity.PullRequestInfo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PullRequestInfoRepository extends JpaRepository<PullRequestInfo, Long> {

    @Query("select p from PullRequestInfo p where p.task.id = :taskId")
    Optional<PullRequestInfo> findByTaskId(@Param("taskId") Long taskId);
}
