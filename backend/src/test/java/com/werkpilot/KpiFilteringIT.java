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
class KpiFilteringIT extends PostgreSqlTestContainerSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KpiQueryService kpiQueryService;

    @Test
    void appliesMachineProductAndShiftFiltersTogether() {
        S3MeasurementFixtureSupport support = new S3MeasurementFixtureSupport(jdbcTemplate);
        S3MeasurementFixtureSupport.Fixture fixture = support.createFixture();
        var job = support.importJob("PRODUCTION_RECORDS", "COMMITTED");

        support.production(job, fixture, fixture.machineId(), fixture.productId(), fixture.shiftId(),
                instant("2026-07-01T08:00:00Z"), instant("2026-07-01T09:00:00Z"), 40);
        support.production(job, fixture, fixture.secondMachineId(), fixture.productId(), fixture.shiftId(),
                instant("2026-07-01T08:00:00Z"), instant("2026-07-01T09:00:00Z"), 100);
        support.production(job, fixture, fixture.machineId(), fixture.secondProductId(), fixture.shiftId(),
                instant("2026-07-01T08:00:00Z"), instant("2026-07-01T09:00:00Z"), 200);
        support.production(job, fixture, fixture.machineId(), fixture.productId(), fixture.secondShiftId(),
                instant("2026-07-01T08:00:00Z"), instant("2026-07-01T09:00:00Z"), 300);

        ProductionKpiResponse response = kpiQueryService.productionKpis(new KpiQuery(
                instant("2026-07-01T08:00:00Z"),
                instant("2026-07-01T10:00:00Z"),
                fixture.factoryId(),
                fixture.lineId(),
                fixture.machineId(),
                fixture.productId(),
                fixture.shiftId()));

        assertThat(response.totalUnitsProduced()).isEqualTo(40);
        assertThat(response.outputPerHour().value()).isEqualByComparingTo("40.000");
    }

    private static Instant instant(String value) {
        return Instant.parse(value);
    }
}
