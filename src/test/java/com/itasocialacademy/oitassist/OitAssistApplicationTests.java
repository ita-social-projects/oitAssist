package com.itasocialacademy.oitassist;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OitAssistApplicationTests {

    @Test
    void test () {
        boolean a = true;
        boolean b = true;

        Assertions.assertEquals(a, b);
    }
}
