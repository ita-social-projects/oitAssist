package com.itasocialacademy.oitassist.core.controller;

import com.itasocialacademy.oitassist.ControllerUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SpaControllerTest extends ControllerUnitTest<SpaController> {

    @Override
    protected SpaController getController() {
        return new SpaController();
    }

    @Test
    void forwardRoot_shouldForwardToIndexHtml() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void forwardSingleSegmentPath_shouldForwardToIndexHtml() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/competitions"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void forwardMultiSegmentPath_shouldForwardToIndexHtml() throws Exception {
        mockMvc.perform(get("/competitions/1"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/competitions/1/stages"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));

        mockMvc.perform(get("/competitions/1/stages/2/tasks"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void apiPaths_shouldNotBeForwarded() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isNotFound());
    }

    @Test
    void staticFilesWithExtensions_shouldNotBeForwarded() throws Exception {
        mockMvc.perform(get("/vite.svg"))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/assets/index-DtHrNYtg.js"))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/assets/index-DA9piY5s.css"))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/favicon.ico"))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/i18n/en/common.json"))
            .andExpect(status().isNotFound());
    }
}
