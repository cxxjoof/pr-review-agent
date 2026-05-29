package com.example.prreview.repository;

import com.example.prreview.entity.RiskItem;
import com.example.prreview.enums.RiskLevel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RiskItemRepository extends JpaRepository<RiskItem, Long> {

    @Query("select r from RiskItem r where r.task.id = :taskId")
    List<RiskItem> findByTaskId(@Param("taskId") Long taskId);

    @Query("select r from RiskItem r where r.task.id = :taskId and r.riskLevel = :riskLevel")
    List<RiskItem> findByTaskIdAndRiskLevel(@Param("taskId") Long taskId, @Param("riskLevel") RiskLevel riskLevel);

    void deleteByTask_Id(Long taskId);
}
