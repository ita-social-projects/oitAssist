package com.itasocialacademy.oitassist.core.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
    @GetMapping(value = {
        "/",
        "/{path:^(?!api|actuator|docs|swagger-ui|v3|oauth2|uploads|assets|i18n|oauth-test|static)[^\\.]*}",
        "/{path:^(?!api|actuator|docs|swagger-ui|v3|oauth2|uploads|assets|i18n|oauth-test|static)[^\\.]*}/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
