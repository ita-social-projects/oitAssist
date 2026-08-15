package com.itasocialacademy.oitassist.security.config;

import com.itasocialacademy.oitassist.core.controller.SpaController;
import com.itasocialacademy.oitassist.core.web.AppExceptionHttpStatusMapper;
import com.itasocialacademy.oitassist.core.web.GlobalExceptionHandler;
import com.itasocialacademy.oitassist.security.jwt.CustomAuthenticationEntryPoint;
import com.itasocialacademy.oitassist.security.jwt.JwtFilter;
import com.itasocialacademy.oitassist.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.itasocialacademy.oitassist.security.oauth2.OAuth2FailureHandler;
import com.itasocialacademy.oitassist.security.oauth2.OAuth2SuccessHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SpaController.class)
@Import({
    SecurityConfig.class,
    GlobalExceptionHandler.class,
    AppExceptionHttpStatusMapper.class,
    CustomAuthenticationEntryPoint.class,
    ObjectMapper.class
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private OAuth2SuccessHandler successHandler;

    @MockitoBean
    private OAuth2FailureHandler failureHandler;

    @MockitoBean
    private HttpCookieOAuth2AuthorizationRequestRepository cookieRepository;

    @BeforeEach
    void setUp() throws IOException, ServletException {
        doAnswer(invocation -> {
            ServletRequest req = invocation.getArgument(0);
            ServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    void publicStaticAssets_shouldBePermitted() throws Exception {
        mockMvc.perform(get("/assets/index-DtHrNYtg.js"))
            .andExpect(status().isNotFound()); // not 401 Unauthorized

        mockMvc.perform(get("/assets/index-DA9piY5s.css"))
            .andExpect(status().isNotFound()); // not 401 Unauthorized

        mockMvc.perform(get("/vite.svg"))
            .andExpect(status().isNotFound()); // not 401 Unauthorized

        mockMvc.perform(get("/favicon.ico"))
            .andExpect(status().isNotFound()); // not 401 Unauthorized

        mockMvc.perform(get("/i18n/en/common.json"))
            .andExpect(status().isNotFound()); // not 401 Unauthorized
    }

    @Test
    void spaRoutes_shouldBePermittedAndForwardToIndexHtml() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/competitions"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/competitions/123"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/competitions/123/stages/456"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void apiEndpoints_withoutAuth_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/competitions"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void publicApiEndpoints_withoutAuth_shouldBePermitted() throws Exception {
        mockMvc.perform(post("/api/v1/security/signIn"))
            .andExpect(status().isNotFound()); // not 401 Unauthorized

        mockMvc.perform(get("/api/v1/news"))
            .andExpect(status().isNotFound()); // not 401 Unauthorized
    }
}
