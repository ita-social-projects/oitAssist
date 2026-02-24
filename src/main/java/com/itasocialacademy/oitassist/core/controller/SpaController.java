package com.itasocialacademy.oitassist.core.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
    @GetMapping({"/"})
    public String forward() {
        return "forward:/index.html";
    }
}
