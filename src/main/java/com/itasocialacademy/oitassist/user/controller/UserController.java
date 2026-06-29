package com.itasocialacademy.oitassist.user.controller;

import com.itasocialacademy.oitassist.core.rest.controller.AbstractRestControllerImpl;
import com.itasocialacademy.oitassist.user.dao.dto.request.ChangeUserRoleRequest;
import com.itasocialacademy.oitassist.user.dao.dto.request.CreateUserDTO;
import com.itasocialacademy.oitassist.user.dao.dto.request.UpdateUserDTO;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import com.itasocialacademy.oitassist.user.service.interfaces.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Users v1", description = "Operations related to users")
@RequestMapping("/api/v1/users")
public class UserController
    extends AbstractRestControllerImpl<Long, CreateUserDTO, UpdateUserDTO, ResponseUserDTO, UserService> {
    protected UserController(UserService service) {
        super(service);
    }

    @GetMapping("/profile")
    @Operation(
        summary = "Get current user profile",
        description = "Returns the current authenticated user's profile")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User profile retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ResponseUserDTO.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - token is missing or invalid",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                        {
                            "message": "Full authentication is required to access this resource"
                        }
                    """)))
    })
    public ResponseEntity<ResponseUserDTO> getProfile() {
        return ResponseEntity.ok(service.getCurrentUserProfile());
    }

    @PatchMapping("/{id}/role")
    @Operation(
        summary = "Change user role",
        description = "Allows admin to change role of an existing user")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User role updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ResponseUserDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid role or request payload",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                        {
                            "message": "Invalid role provided"
                        }
                    """))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - token is missing or invalid",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                        {
                            "message": "Full authentication is required to access this resource"
                        }
                    """))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient permissions or attempt to modify restricted user",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                        {
                            "message": "Access denied"
                        }
                    """))),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                        {
                            "message": "User not found"
                        }
                    """)))
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseUserDTO> changeUserRole(
        @PathVariable Long id,
        @RequestBody @Valid ChangeUserRoleRequest request) {
        return ResponseEntity.ok(service.changeUserRole(id, request.getRole()));
    }
}
