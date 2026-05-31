package com.example.prreview.repository;

import com.example.prreview.entity.RiskItem;
import com.example.prreview.enums.FindingLevel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RiskItemRepository extends JpaRepository<RiskItem, Long> {

    @Query("select r from RiskItem r where r.task.id = :taskId")
    List<RiskItem> findByTaskId(@Param("taskId") Long taskId);

    @Query("select r from RiskItem r where r.task.id = :taskId and r.findingLevel = :findingLevel")
    List<RiskItem> findByTaskIdAndFindingLevel(@Param("taskId") Long taskId, @Param("findingLevel") FindingLevel findingLevel);

    @Query("select r from RiskItem r where r.id = :findingId and r.task.id = :taskId")
    Optional<RiskItem> findByIdAndTaskId(@Param("findingId") Long findingId, @Param("taskId") Long taskId);

    void deleteByTask_Id(Long taskId);
}
