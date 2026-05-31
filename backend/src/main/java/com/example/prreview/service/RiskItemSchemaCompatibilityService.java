package com.example.prreview.service;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RiskItemSchemaCompatibilityService {

    private static final Logger log = LoggerFactory.getLogger(RiskItemSchemaCompatibilityService.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public RiskItemSchemaCompatibilityService(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void migrateLegacyColumnsIfNeeded() {
        if (!tableExists("risk_item")
                || !columnExists("risk_item", "risk_level")
                || !columnExists("risk_item", "risk_type")
                || !columnExists("risk_item", "finding_level")
                || !columnExists("risk_item", "finding_kind")
                || !columnExists("risk_item", "finding_category")) {
            return;
        }

        List<Map<String, Object>> legacyRows = jdbcTemplate.queryForList(
                """
                select id, risk_level, risk_type, finding_level, finding_kind, finding_category
                from risk_item
                where finding_level is null or finding_kind is null or finding_category is null
                """
        );

        for (Map<String, Object> row : legacyRows) {
            Long id = toLong(row.get("id"));
            if (id == null) {
                continue;
            }

            String findingLevel = preferredValue(row.get("finding_level"), mapLegacyFindingLevel(row.get("risk_level")));
            String findingKind = preferredValue(row.get("finding_kind"), mapLegacyFindingKind(row.get("risk_level")));
            String findingCategory = preferredValue(row.get("finding_category"), mapLegacyFindingCategory(row.get("risk_type")));

            jdbcTemplate.update(
                    """
                    update risk_item
                    set finding_level = ?, finding_kind = ?, finding_category = ?
                    where id = ?
                    """,
                    findingLevel,
                    findingKind,
                    findingCategory,
                    id
            );
        }

        if (!legacyRows.isEmpty()) {
            log.info("Backfilled {} legacy risk_item rows into finding_* columns.", legacyRows.size());
        }
    }

    private boolean tableExists(String tableName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet tables = metadata.getTables(connection.getCatalog(), null, tableName, new String[]{"TABLE"})) {
                if (tables.next()) {
                    return true;
                }
            }
            try (ResultSet tables = metadata.getTables(connection.getCatalog(), null, tableName.toUpperCase(Locale.ROOT), new String[]{"TABLE"})) {
                return tables.next();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to inspect risk_item table metadata.", exception);
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            return hasColumn(metadata, connection.getCatalog(), tableName, columnName)
                    || hasColumn(metadata, connection.getCatalog(), tableName.toUpperCase(Locale.ROOT), columnName.toUpperCase(Locale.ROOT));
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to inspect risk_item column metadata.", exception);
        }
    }

    private boolean hasColumn(DatabaseMetaData metadata, String catalog, String tableName, String columnName) throws SQLException {
        try (ResultSet columns = metadata.getColumns(catalog, null, tableName, columnName)) {
            return columns.next();
        }
    }

    private String preferredValue(Object currentValue, String fallbackValue) {
        String normalizedCurrent = normalizeEnumValue(currentValue);
        if (StringUtils.hasText(normalizedCurrent)) {
            return normalizedCurrent;
        }
        return fallbackValue;
    }

    private String mapLegacyFindingLevel(Object legacyRiskLevel) {
        String normalized = normalizeEnumValue(legacyRiskLevel);
        if (!StringUtils.hasText(normalized)) {
            return "ADVISORY";
        }

        return switch (normalized) {
            case "HIGH", "MEDIUM", "LOW" -> normalized;
            default -> "ADVISORY";
        };
    }

    private String mapLegacyFindingKind(Object legacyRiskLevel) {
        return StringUtils.hasText(normalizeEnumValue(legacyRiskLevel)) ? "RISK" : "ADVISORY";
    }

    private String mapLegacyFindingCategory(Object legacyRiskType) {
        String normalized = normalizeEnumValue(legacyRiskType);
        if (!StringUtils.hasText(normalized)) {
            return "OTHER";
        }

        return switch (normalized) {
            case "SECURITY" -> "SECURITY";
            case "PERFORMANCE" -> "PERFORMANCE";
            case "EXCEPTION_HANDLING" -> "EXCEPTION_HANDLING";
            case "TEST_MISSING" -> "TEST_GAP";
            case "CONFIG_CHANGE" -> "CONFIG_COMPATIBILITY";
            case "CODE_STYLE" -> "MAINTAINABILITY";
            case "DATABASE", "INPUT_VALIDATION", "NULL_POINTER", "OTHER" -> "OTHER";
            default -> "OTHER";
        };
    }

    private String normalizeEnumValue(Object value) {
        if (value == null) {
            return null;
        }

        String normalized = value.toString().trim();
        if (normalized.isBlank()) {
            return null;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
