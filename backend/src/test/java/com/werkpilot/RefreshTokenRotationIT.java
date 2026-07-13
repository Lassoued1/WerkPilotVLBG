package com.werkpilot;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
class RefreshTokenRotationIT extends PostgreSqlTestContainerSupport {

    private static final String REFRESH_COOKIE_NAME = "werkpilot_refresh";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void refreshRotatesRefreshCookieAndRejectsThePreviousToken() throws Exception {
        MvcResult loginResult = login();
        String loginBody = loginResult.getResponse().getContentAsString();
        String firstAccessToken = JsonPath.read(loginBody, "$.accessToken");
        String firstCsrfToken = JsonPath.read(loginBody, "$.csrfToken");
        Cookie firstRefreshCookie = refreshCookieFrom(loginResult);

        MvcResult refreshResult = mockMvc.perform(post("/auth/refresh")
                        .cookie(firstRefreshCookie)
                        .header("X-WerkPilot-CSRF", firstCsrfToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(REFRESH_COOKIE_NAME + "=")))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.csrfToken").isNotEmpty())
                .andReturn();

        String refreshBody = refreshResult.getResponse().getContentAsString();
        String secondAccessToken = JsonPath.read(refreshBody, "$.accessToken");
        String secondCsrfToken = JsonPath.read(refreshBody, "$.csrfToken");
        Cookie secondRefreshCookie = refreshCookieFrom(refreshResult);

        org.assertj.core.api.Assertions.assertThat(secondAccessToken).isNotEqualTo(firstAccessToken);
        org.assertj.core.api.Assertions.assertThat(secondCsrfToken).isNotEqualTo(firstCsrfToken);
        org.assertj.core.api.Assertions.assertThat(secondRefreshCookie.getValue()).isNotEqualTo(firstRefreshCookie.getValue());

        mockMvc.perform(post("/auth/refresh")
                        .cookie(firstRefreshCookie)
                        .header("X-WerkPilot-CSRF", firstCsrfToken))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, not(containsString(REFRESH_COOKIE_NAME + "="))))
                .andExpect(jsonPath("$.errorCode").value("AUTH_TOKEN_EXPIRED"));
    }

    private MvcResult login() throws Exception {
        return mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@werkpilot.local","password":"WerkPilot-Admin-Change-Me-2026"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
    }

    private Cookie refreshCookieFrom(MvcResult result) {
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String cookiePair = setCookie.split(";", 2)[0];
        String refreshToken = cookiePair.substring((REFRESH_COOKIE_NAME + "=").length());
        return new Cookie(REFRESH_COOKIE_NAME, refreshToken);
    }
}
