package com.itasocialacademy.oitassist.task.controller;

import com.itasocialacademy.oitassist.core.enums.ErrorCode;
import com.itasocialacademy.oitassist.core.exceptions.ExternalServiceException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskController {
    @GetMapping("/tasks")
    ResponseEntity<String> getTasks() {
        if (true) throw new ExternalServiceException("Service unavailable", ErrorCode.COMMON_INTERNAL_ERROR);
        return ResponseEntity.ok().build();
    }
}
