package com.werkpilot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.werkpilot.support.PostgreSqlTestContainerSupport;
import jakarta.servlet.http.Cookie;
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
class CsrfProtectionIT extends PostgreSqlTestContainerSupport {

    private static final String REFRESH_COOKIE_NAME = "werkpilot_refresh";
    private static final String CSRF_HEADER_NAME = "X-WerkPilot-CSRF";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void refreshRequiresMatchingCsrfHeader() throws Exception {
        SessionParts session = login();

        mockMvc.perform(post("/auth/refresh").cookie(session.refreshCookie()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_EXPIRED"));

        mockMvc.perform(post("/auth/refresh")
                        .cookie(session.refreshCookie())
                        .header(CSRF_HEADER_NAME, "wrong-csrf-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_EXPIRED"));
    }

    @Test
    void logoutRequiresMatchingCsrfHeader() throws Exception {
        SessionParts session = login();

        mockMvc.perform(post("/auth/logout")
                        .cookie(session.refreshCookie())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_EXPIRED"));

        mockMvc.perform(post("/auth/logout")
                        .cookie(session.refreshCookie())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                        .header(CSRF_HEADER_NAME, "wrong-csrf-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_EXPIRED"));
    }

    @Test
    void corsAllowsConfiguredFrontendOriginWithCredentials() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:8081")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:8081"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    private SessionParts login() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@werkpilot.local","password":"WerkPilot-Admin-Change-Me-2026"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return new SessionParts(
                refreshCookieFrom(result),
                JsonPath.read(body, "$.accessToken"),
                JsonPath.read(body, "$.csrfToken"));
    }

    private Cookie refreshCookieFrom(MvcResult result) {
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String cookiePair = setCookie.split(";", 2)[0];
        String refreshToken = cookiePair.substring((REFRESH_COOKIE_NAME + "=").length());
        return new Cookie(REFRESH_COOKIE_NAME, refreshToken);
    }

    private record SessionParts(Cookie refreshCookie, String accessToken, String csrfToken) {
    }
}
