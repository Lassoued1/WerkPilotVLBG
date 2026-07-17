package com.werkpilot;

import static org.assertj.core.api.Assertions.assertThat;

import com.werkpilot.analytics.application.KpiQuery;
import com.werkpilot.analytics.application.KpiQueryService;
import com.werkpilot.analytics.application.ProductionKpiResponse;
import com.werkpilot.support.PostgreSqlTestContainerSupport;
import com.werkpilot.support.S3MeasurementFixtureSupport;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class TimeWindowAggregationIT extends PostgreSqlTestContainerSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KpiQueryService kpiQueryService;

    @Test
    void usesFullyContainedHalfOpenWindowAndCommittedJobsOnly() {
        S3MeasurementFixtureSupport support = new S3MeasurementFixtureSupport(jdbcTemplate);
        S3MeasurementFixtureSupport.Fixture fixture = support.createFixture();
        var committedJob = support.importJob("PRODUCTION_RECORDS", "COMMITTED");
        var supersededJob = support.importJob("PRODUCTION_RECORDS", "SUPERSEDED");

        support.production(committedJob, fixture, instant("2026-07-01T08:00:00Z"), instant("2026-07-01T09:00:00Z"), 100);
        support.production(committedJob, fixture, instant("2026-07-01T09:00:00Z"), instant("2026-07-01T10:00:00Z"), 50);
        support.production(committedJob, fixture, instant("2026-07-01T07:30:00Z"), instant("2026-07-01T08:30:00Z"), 999);
        support.production(supersededJob, fixture, instant("2026-07-01T08:00:00Z"), instant("2026-07-01T09:00:00Z"), 999);

        ProductionKpiResponse response = kpiQueryService.productionKpis(new KpiQuery(
                instant("2026-07-01T08:00:00Z"),
                instant("2026-07-01T10:00:00Z"),
                fixture.factoryId(),
                null,
                null,
                null,
                null));

        assertThat(response.totalUnitsProduced()).isEqualTo(150);
        assertThat(response.outputPerHour().value()).isEqualByComparingTo("75.000");
        assertThat(response.appliedFilters().from()).isEqualTo(instant("2026-07-01T08:00:00Z"));
        assertThat(response.appliedFilters().to()).isEqualTo(instant("2026-07-01T10:00:00Z"));
    }

    private static Instant instant(String value) {
        return Instant.parse(value);
    }
}
