package com.werkpilot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.werkpilot.support.PostgreSqlTestContainerSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class MasterDataUniquenessIT extends PostgreSqlTestContainerSupport {

    private static final String ADMIN_EMAIL = "admin@werkpilot.local";
    private static final String ADMIN_PASSWORD = "WerkPilot-Admin-Change-Me-2026";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void lineAndMachineCodesAreUniqueWithinFactoryOnly() throws Exception {
        String adminToken = loginAccessToken();
        String suffix = suffix();
        String factoryA = createFactory(adminToken, "FA-" + suffix);
        String factoryB = createFactory(adminToken, "FB-" + suffix);
        String lineA1 = createLine(adminToken, factoryA, "L-SHARED-" + suffix);
        String lineA2 = createLine(adminToken, factoryA, "L-OTHER-" + suffix);
        String lineB1 = createLine(adminToken, factoryB, "L-SHARED-" + suffix);

        mockMvc.perform(post("/production-lines")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"factoryId":"%s","code":"L-SHARED-%s","name":"Duplicate Line"}
                                """.formatted(factoryA, suffix)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));

        createMachine(adminToken, lineA1, "M-SHARED-" + suffix);
        mockMvc.perform(post("/machines")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lineId":"%s","code":"M-SHARED-%s","name":"Duplicate Machine"}
                                """.formatted(lineA2, suffix)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));

        mockMvc.perform(post("/machines")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lineId":"%s","code":"M-SHARED-%s","name":"Other Factory Machine"}
                                """.formatted(lineB1, suffix)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.factoryId").value(factoryB));
    }

    @Test
    void productShiftReasonAndCategoryCodesAreGloballyUnique() throws Exception {
        String adminToken = loginAccessToken();
        String suffix = suffix();

        createProduct(adminToken, "P-GLOBAL-" + suffix);
        assertDuplicatePost(adminToken, "/products", """
                {"code":"P-GLOBAL-%s","name":"Duplicate Product"}
                """.formatted(suffix));

        createShift(adminToken, "S-GLOBAL-" + suffix);
        assertDuplicatePost(adminToken, "/shifts", """
                {"code":"S-GLOBAL-%s","name":"Duplicate Shift","startTime":"14:00:00","endTime":"22:00:00","plannedMinutes":480}
                """.formatted(suffix));

        createSimple(adminToken, "/downtime-reasons", "DR-GLOBAL-" + suffix);
        assertDuplicatePost(adminToken, "/downtime-reasons", """
                {"code":"DR-GLOBAL-%s","name":"Duplicate Reason"}
                """.formatted(suffix));

        createSimple(adminToken, "/scrap-categories", "SC-GLOBAL-" + suffix);
        assertDuplicatePost(adminToken, "/scrap-categories", """
                {"code":"SC-GLOBAL-%s","name":"Duplicate Category"}
                """.formatted(suffix));
    }

    @Test
    void missingOrInactiveParentsAreRejected() throws Exception {
        String adminToken = loginAccessToken();
        String suffix = suffix();

        mockMvc.perform(post("/production-lines")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"factoryId":"%s","code":"L-MISSING-%s","name":"Missing Factory Line"}
                                """.formatted(UUID.randomUUID(), suffix)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));

        mockMvc.perform(post("/machines")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lineId":"%s","code":"M-MISSING-%s","name":"Missing Line Machine"}
                                """.formatted(UUID.randomUUID(), suffix)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));

        String inactiveFactory = createFactory(adminToken, "F-INACTIVE-" + suffix);
        mockMvc.perform(delete("/factories/{id}", inactiveFactory)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/production-lines")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"factoryId":"%s","code":"L-INACTIVE-%s","name":"Inactive Factory Line"}
                                """.formatted(inactiveFactory, suffix)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }

    private void assertDuplicatePost(String adminToken, String path, String body) throws Exception {
        mockMvc.perform(post(path)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BUSINESS_RULE_VIOLATION"));
    }

    private String createFactory(String adminToken, String code) throws Exception {
        MvcResult result = mockMvc.perform(post("/factories")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"Pilot Factory"}
                                """.formatted(code)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private String createLine(String adminToken, String factoryId, String code) throws Exception {
        MvcResult result = mockMvc.perform(post("/production-lines")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"factoryId":"%s","code":"%s","name":"Pilot Line"}
                                """.formatted(factoryId, code)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }

    private void createMachine(String adminToken, String lineId, String code) throws Exception {
        mockMvc.perform(post("/machines")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lineId":"%s","code":"%s","name":"Pilot Machine"}
                                """.formatted(lineId, code)))
                .andExpect(status().isCreated());
    }

    private void createProduct(String adminToken, String code) throws Exception {
        mockMvc.perform(post("/products")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"Pilot Product"}
                                """.formatted(code)))
                .andExpect(status().isCreated());
    }

    private void createShift(String adminToken, String code) throws Exception {
        mockMvc.perform(post("/shifts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"Early Shift","startTime":"06:00:00","endTime":"14:00:00","plannedMinutes":480}
                                """.formatted(code)))
                .andExpect(status().isCreated());
    }

    private void createSimple(String adminToken, String path, String code) throws Exception {
        mockMvc.perform(post(path)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"Pilot Name"}
                                """.formatted(code)))
                .andExpect(status().isCreated());
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

    private static String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
