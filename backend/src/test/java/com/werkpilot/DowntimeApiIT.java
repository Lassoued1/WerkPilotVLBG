package com.werkpilot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.werkpilot.support.PostgreSqlTestContainerSupport;
import com.werkpilot.support.S3MeasurementFixtureSupport;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class DowntimeApiIT extends PostgreSqlTestContainerSupport {

    private static final String ADMIN_EMAIL = "admin@werkpilot.local";
    private static final String ADMIN_PASSWORD = "WerkPilot-Admin-Change-Me-2026";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void downtimeParetoReturnsCommittedTotalsAvailabilityAndCumulativePercentages() throws Exception {
        String token = loginAccessToken();
        S3MeasurementFixtureSupport support = new S3MeasurementFixtureSupport(jdbcTemplate);
        S3MeasurementFixtureSupport.Fixture fixture = support.createFixture();
        var committedJob = support.importJob("DOWNTIME_RECORDS", "COMMITTED");
        var supersededJob = support.importJob("DOWNTIME_RECORDS", "SUPERSEDED");
        support.downtime(committedJob, fixture, instant("2026-07-01T08:00:00Z"), instant("2026-07-01T08:30:00Z"), 30);
        support.downtime(committedJob, fixture, instant("2026-07-01T09:00:00Z"), instant("2026-07-01T09:20:00Z"), 20);
        support.downtime(supersededJob, fixture, instant("2026-07-01T10:00:00Z"), instant("2026-07-01T10:15:00Z"), 15);

        mockMvc.perform(get("/downtime/pareto")
                        .param("from", "2026-07-01T08:00:00Z")
                        .param("to", "2026-07-01T11:00:00Z")
                        .param("machineId", fixture.machineId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDowntimeMinutes").value(50))
                .andExpect(jsonPath("$.availability.available").value(true))
                .andExpect(jsonPath("$.availability.value").value(89.583))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].downtimeMinutes").value(30))
                .andExpect(jsonPath("$.items[0].cumulativePercentage").value(60.000))
                .andExpect(jsonPath("$.items[1].cumulativePercentage").value(100.000));
    }

    @Test
    void downtimeBoundaryRowsMustBeFullyContained() throws Exception {
        String token = loginAccessToken();
        S3MeasurementFixtureSupport support = new S3MeasurementFixtureSupport(jdbcTemplate);
        S3MeasurementFixtureSupport.Fixture fixture = support.createFixture();
        var committedJob = support.importJob("DOWNTIME_RECORDS", "COMMITTED");
        support.downtime(committedJob, fixture, instant("2026-07-01T07:30:00Z"), instant("2026-07-01T08:15:00Z"), 45);
        support.downtime(committedJob, fixture, instant("2026-07-01T08:00:00Z"), instant("2026-07-01T08:15:00Z"), 15);

        mockMvc.perform(get("/downtime/pareto")
                        .param("from", "2026-07-01T08:00:00Z")
                        .param("to", "2026-07-01T09:00:00Z")
                        .param("machineId", fixture.machineId().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDowntimeMinutes").value(15))
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    private String loginAccessToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static Instant instant(String value) {
        return Instant.parse(value);
    }
}
