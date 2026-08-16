package com.itasocialacademy.oitassist.core.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itasocialacademy.oitassist.core.web.AppExceptionHttpStatusMapper;
import com.itasocialacademy.oitassist.security.config.SecurityConfig;
import com.itasocialacademy.oitassist.security.jwt.JwtFilter;
import com.itasocialacademy.oitassist.security.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.itasocialacademy.oitassist.security.oauth2.OAuth2FailureHandler;
import com.itasocialacademy.oitassist.security.oauth2.OAuth2SuccessHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SpaController.class)
@Import(SecurityConfig.class)
class SpaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppExceptionHttpStatusMapper appExceptionHttpStatusMapper;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private AuthenticationEntryPoint entryPoint;

    @MockitoBean
    private OAuth2SuccessHandler successHandler;

    @MockitoBean
    private OAuth2FailureHandler failureHandler;

    @MockitoBean
    private HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    void shouldForwardUnauthenticatedUiRequestToindexHtml() throws Exception {
        mockMvc.perform(get("/ui"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void shouldForwardUnauthenticatedNestedUiRequestToindexHtml() throws Exception {
        mockMvc.perform(get("/ui/nested/dashboard"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void shouldForwardUnauthenticatedTrailingSlashUiRequestToindexHtml() throws Exception {
        mockMvc.perform(get("/ui/"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));
    }
}
