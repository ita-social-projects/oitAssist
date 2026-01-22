package com.itasocialacademy.oitassist.clean_architecture_example.presentation.controller;

import com.itasocialacademy.oitassist.clean_architecture_example.application.service.UserService;
import com.itasocialacademy.oitassist.clean_architecture_example.domain.dto.request.CreateUserRequest;
import com.itasocialacademy.oitassist.clean_architecture_example.domain.dto.response.CreateUserResponse;
import com.itasocialacademy.oitassist.clean_architecture_example.presentation.mapper.UserDtoMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
@Validated
public class ClearUserController {
    private final UserService userService;
    private final UserDtoMapper userDtoMapper;

    @PostMapping
    public ResponseEntity<CreateUserResponse> create(@RequestBody @Valid CreateUserRequest request) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(userService.create(userDtoMapper.toDomain(request)));
    }
}
