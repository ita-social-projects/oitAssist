package com.itasocialacademy.oitassist.envvar.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.itasocialacademy.oitassist.core.web.AppExceptionHttpStatusMapper;
import com.itasocialacademy.oitassist.envvar.service.interfaces.EnvVariableService;
import com.itasocialacademy.oitassist.security.api.interfaces.SecurityFacade;
import com.itasocialacademy.oitassist.security.jwt.JwtFilter;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(
    controllers = EnvVariableController.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JwtFilter.class))
@Import(EnvVariableControllerTest.SecurityTestConfiguration.class)
class EnvVariableControllerTest {

    private static final String ENDPOINT = "/api/v1/admin/environment-variables";
    private static final String PUBLIC_KEY = "APP_NAME";
    private static final String PUBLIC_VALUE = "oit-assist";
    private static final Long ADMIN_ID = 42L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnvVariableService envVariableService;

    @MockitoBean
    private SecurityFacade securityFacade;

    @MockitoBean
    private AppExceptionHttpStatusMapper appExceptionHttpStatusMapper;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getMap_ShouldReturnVariables_WhenCallerIsAdmin() throws Exception {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(ADMIN_ID));
        when(envVariableService.getenv()).thenReturn(Map.of(PUBLIC_KEY, PUBLIC_VALUE));

        mockMvc.perform(get(ENDPOINT))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$." + PUBLIC_KEY).value(PUBLIC_VALUE));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getMap_ShouldReturnEmptyObject_WhenNoVariableIsAllowed() throws Exception {
        when(securityFacade.getCurrentUserId()).thenReturn(Optional.of(ADMIN_ID));
        when(envVariableService.getenv()).thenReturn(Map.of());

        mockMvc.perform(get(ENDPOINT))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isEmpty());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("callersWithoutAdminRole")
    void getMap_ShouldReturnForbidden_WhenCallerIsNotAdmin(RequestPostProcessor caller) throws Exception {
        mockMvc.perform(get(ENDPOINT).with(caller))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
            .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(envVariableService);
    }

    @Test
    void controller_ShouldBeExcludedFromApiDocumentation() {
        assertThat(EnvVariableController.class.getAnnotation(Hidden.class))
            .as("the endpoint must stay out of the generated OpenAPI document")
            .isNotNull();
    }

    private static Stream<Arguments> callersWithoutAdminRole() {
        return Stream.of(
            Arguments.of(named("caller with the USER role", user("someone").roles("USER"))),
            Arguments.of(named("anonymous caller", anonymous())));
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableWebSecurity
    @EnableMethodSecurity
    static class SecurityTestConfiguration {
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) {
            return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorization -> authorization
                    .anyRequest()
                    .permitAll())
                .build();
        }
    }
}
