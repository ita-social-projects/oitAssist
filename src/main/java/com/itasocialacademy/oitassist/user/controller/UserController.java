package com.itasocialacademy.oitassist.user.controller;

import com.itasocialacademy.oitassist.core.rest.controller.AbstractRestControllerImpl;
import com.itasocialacademy.oitassist.user.dao.dto.request.CreateUserDTO;
import com.itasocialacademy.oitassist.user.dao.dto.request.UpdateUserDTO;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import com.itasocialacademy.oitassist.user.service.interfaces.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Users v1", description = "Operations related to users")
@RequestMapping("/api/v1/users")
public class UserController
    extends AbstractRestControllerImpl<Long, CreateUserDTO, UpdateUserDTO, ResponseUserDTO, UserService> {
    protected UserController(UserService service) {
        super(service);
    }
}
