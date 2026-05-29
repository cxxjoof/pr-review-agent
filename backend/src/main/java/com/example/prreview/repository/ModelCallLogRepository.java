package com.example.prreview.repository;

import com.example.prreview.entity.ModelCallLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ModelCallLogRepository extends JpaRepository<ModelCallLog, Long> {

    @Query("select m from ModelCallLog m where m.task.id = :taskId")
    List<ModelCallLog> findByTaskId(@Param("taskId") Long taskId);
}
