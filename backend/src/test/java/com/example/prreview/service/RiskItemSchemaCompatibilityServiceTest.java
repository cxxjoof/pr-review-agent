package com.example.prreview.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.prreview.entity.ReviewTask;
import com.example.prreview.enums.TaskStatus;
import com.example.prreview.repository.ReviewTaskRepository;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class RiskItemSchemaCompatibilityServiceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private ReviewTaskRepository reviewTaskRepository;

    @Test
    void shouldBackfillFindingColumnsFromLegacyRiskColumns() {
        ReviewTask task = new ReviewTask();
        task.setRepoUrl("https://github.com/example/repo");
        task.setRepoOwner("example");
        task.setRepoName("repo");
        task.setPrNumber(7);
        task.setStatus(TaskStatus.SUCCESS);
        ReviewTask savedTask = reviewTaskRepository.save(task);

        jdbcTemplate.execute("alter table risk_item add column risk_level varchar(30)");
        jdbcTemplate.execute("alter table risk_item add column risk_type varchar(100)");
        jdbcTemplate.update(
                """
                insert into risk_item (
                    task_id,
                    file_path,
                    line_number,
                    finding_level,
                    finding_kind,
                    finding_category,
                    description,
                    suggestion,
                    created_at,
                    risk_level,
                    risk_type
                ) values (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?)
                """,
                savedTask.getId(),
                "README.md",
                12,
                null,
                null,
                null,
                "Legacy finding row",
                "Backfill new fields",
                "MEDIUM",
                "CONFIG_CHANGE"
        );

        RiskItemSchemaCompatibilityService compatibilityService =
                new RiskItemSchemaCompatibilityService(jdbcTemplate, dataSource);

        compatibilityService.migrateLegacyColumnsIfNeeded();

        var row = jdbcTemplate.queryForMap(
                """
                select finding_level, finding_kind, finding_category
                from risk_item
                where task_id = ?
                """,
                savedTask.getId()
        );

        assertThat(row.get("finding_level")).isEqualTo("MEDIUM");
        assertThat(row.get("finding_kind")).isEqualTo("RISK");
        assertThat(row.get("finding_category")).isEqualTo("CONFIG_COMPATIBILITY");
    }
}
