package com.itasocialacademy.oitassist;

import com.itasocialacademy.oitassist.core.web.AppExceptionHttpStatusMapper;
import com.itasocialacademy.oitassist.core.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public abstract class ControllerUnitTest<T> {
    public MockMvc mockMvc;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    private final AppExceptionHttpStatusMapper mapper = new AppExceptionHttpStatusMapper();

    protected abstract T getController();

    @BeforeEach
    public void setup() {
        this.mockMvc = MockMvcBuilders
            .standaloneSetup(getController())
            .setControllerAdvice(new GlobalExceptionHandler(mapper))
            .build();
    }
}
