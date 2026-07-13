package com.werkpilot;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.werkpilot.shared.api.AggregateValue;
import com.werkpilot.shared.api.FilterCriteria;
import com.werkpilot.shared.api.JobResponse;
import com.werkpilot.shared.api.JobStatus;
import com.werkpilot.shared.api.PageResponse;
import com.werkpilot.shared.error.ApiException;
import com.werkpilot.shared.error.ErrorCode;
import com.werkpilot.shared.error.ErrorDetail;
import com.werkpilot.support.PostgreSqlTestContainerSupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
class SharedApiContractIT extends PostgreSqlTestContainerSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiDocumentExposesSharedSchemasAsOpenApi31() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").value("3.1.0"))
                .andExpect(jsonPath("$.servers[0].url").value("/api/v1"))
                .andExpect(jsonPath("$.components.schemas.ApiErrorResponse.type").value("object"))
                .andExpect(jsonPath("$.components.schemas.PageResponse.type").value("object"))
                .andExpect(jsonPath("$.components.schemas.AggregateValue.type").value("object"))
                .andExpect(jsonPath("$.components.schemas.FilterCriteria.type").value("object"))
                .andExpect(jsonPath("$.components.schemas.JobResponse.properties.status.enum[0]").value("PROCESSING"))
                .andExpect(jsonPath("$.components.schemas.JobResponse.properties.status.enum[3]").value("SUPERSEDED"));
    }

    @Test
    void sharedResponseShapesSerializeWithContractFieldNames() throws Exception {
        mockMvc.perform(get("/api/v1/__contract/shared-shapes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.items.length()").value(0))
                .andExpect(jsonPath("$.page.page").value(0))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.aggregate.value").value(4.82))
                .andExpect(jsonPath("$.aggregate.unit").value("kWh_per_unit"))
                .andExpect(jsonPath("$.aggregate.available").value(true))
                .andExpect(jsonPath("$.job.status").value("PROCESSING"))
                .andExpect(jsonPath("$.job.completedAt").value(nullValue()))
                .andExpect(jsonPath("$.appliedFilters.factoryId").value("11111111-1111-1111-1111-111111111111"));
    }

    @Test
    void apiExceptionsReturnSanitizedEnglishErrorEnvelopeWithCsvGermanDetailsAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/__contract/business-rule"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.errorCode").value("CSV_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("The uploaded file contains invalid rows."))
                .andExpect(jsonPath("$.path").value("/api/v1/__contract/business-rule"))
                .andExpect(jsonPath("$.details[0].row").value(14))
                .andExpect(jsonPath("$.details[0].column").value("units_produced"))
                .andExpect(jsonPath("$.details[0].value").value("-5"))
                .andExpect(jsonPath("$.details[0].message").value("Der Wert muss groesser oder gleich null sein."))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    void validationErrorsUseStableCodeWithoutStackTraceOrInternalClassNames() throws Exception {
        mockMvc.perform(post("/api/v1/__contract/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Request validation failed."))
                .andExpect(jsonPath("$.details[0].column").value("name"))
                .andExpect(content().string(not(containsString("MethodArgumentNotValidException"))))
                .andExpect(content().string(not(containsString("com.werkpilot"))));
    }

    @TestConfiguration
    static class ContractProbeConfiguration {

        @Bean
        ContractProbeController contractProbeController() {
            return new ContractProbeController();
        }
    }

    @RestController
    @RequestMapping("/api/v1/__contract")
    static class ContractProbeController {

        @GetMapping("/shared-shapes")
        ContractShapes sharedShapes() {
            return new ContractShapes(
                    new PageResponse<>(List.of(), 0, 20, 0, 0),
                    AggregateValue.available(new BigDecimal("4.82"), "kWh_per_unit"),
                    new FilterCriteria(null, null, UUID.fromString("11111111-1111-1111-1111-111111111111"),
                            null, null, null, null, null, null, null, null, null, null, null, null),
                    new JobResponse(UUID.fromString("22222222-2222-2222-2222-222222222222"),
                            JobStatus.PROCESSING,
                            Instant.parse("2026-07-05T12:00:00Z"),
                            null));
        }

        @GetMapping("/business-rule")
        void businessRule() {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    ErrorCode.CSV_VALIDATION_FAILED,
                    "The uploaded file contains invalid rows.",
                    List.of(new ErrorDetail(14, "units_produced", "-5", "Der Wert muss groesser oder gleich null sein.")));
        }

        @PostMapping("/validate")
        void validate(@Valid @RequestBody ValidationProbe request) {
        }
    }

    record ContractShapes(
            PageResponse<String> page,
            AggregateValue aggregate,
            FilterCriteria appliedFilters,
            JobResponse job) {
    }

    record ValidationProbe(@NotBlank String name) {
    }
}
