package com.itasocialacademy.oitassist.user.controller;

import com.itasocialacademy.oitassist.user.dao.dto.request.ProfileUpdateRequestDTO;
import com.itasocialacademy.oitassist.user.dao.dto.request.ReviewRequestDTO;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseProfileUpdateRequestDTO;
import static org.springframework.data.domain.Sort.Direction.DESC;
import com.itasocialacademy.oitassist.core.dao.dto.response.PageResponse;
import com.itasocialacademy.oitassist.core.web.ErrorResponse;
import com.itasocialacademy.oitassist.user.dao.dto.request.ChangeUserRoleRequest;
import com.itasocialacademy.oitassist.user.dao.dto.request.ChangeUserStatusRequest;
import com.itasocialacademy.oitassist.user.dao.dto.response.ResponseUserDTO;
import com.itasocialacademy.oitassist.user.dao.enums.UpdateRequestStatus;
import com.itasocialacademy.oitassist.user.dao.enums.Role;
import com.itasocialacademy.oitassist.user.service.interfaces.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Users v1", description = "Operations related to users")
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService service;

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
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ResponseUserDTO> getProfile() {
        return ResponseEntity.ok(service.getCurrentUserProfile());
    }

    @PostMapping("/profile/update-request")
    @Operation(
        summary = "Creates request for updating current user profile",
        description = "Returns Created(201) status")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "User profile update request successfully created"),
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
            responseCode = "409",
            description = "Conflict",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                        {
                            "message": "You already have a pending update request | You already have a request today"
                        }
                    """))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                        {
                            "message": "Validation failed"
                        }
                    """)))
    })
    public ResponseEntity<Void> createProfileUpdateRequest(@Valid @RequestBody ProfileUpdateRequestDTO request) {
        service.createProfileUpdateRequest(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/profile/update-request")
    @Operation(
        summary = "Get all/by status profile update requests",
        description = "Returns a paginated list of profile update requests filtered by status."
            + " Only accessible by ORG role.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile update requests retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class))),
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
            description = "Forbidden - insufficient permissions",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                        {
                            "message": "Access denied"
                        }
                    """))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                        {
                            "message": "Validation failed"
                        }
                    """)))
    })
    @PreAuthorize("hasRole('ORG')")
    public ResponseEntity<Page<ResponseProfileUpdateRequestDTO>> getProfileUpdateRequests(
        @RequestParam(defaultValue = "PENDING", required = false) UpdateRequestStatus status,
        @ParameterObject @PageableDefault(sort = "requestedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(service.getProfileUpdateRequests(status, pageable));
    }

    @PatchMapping("/profile/update-request/{id}/review")
    @Operation(
        summary = "Review a profile update request",
        description = "Approves or rejects a pending profile update request. Only accessible by ORG role.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Request reviewed successfully"),
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
            description = "Forbidden - insufficient permissions",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                        {
                            "message": "Access denied"
                        }
                    """))),
        @ApiResponse(
            responseCode = "404",
            description = "Request or user not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                        {
                            "message": "Request not found: 1"
                        }
                    """))),
        @ApiResponse(
            responseCode = "409",
            description = "Request already reviewed",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                        {
                            "message": "Request is already reviewed"
                        }
                    """))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(example = """
                        {
                            "message": "Rejection reason cannot be blank"
                        }
                    """)))
    })
    @PreAuthorize("hasRole('ORG')")
    public ResponseEntity<Void> reviewProfileUpdateRequests(
        @PathVariable Long id,
        @Valid @RequestBody ReviewRequestDTO body) {
        service.reviewProfileUpdateRequests(id, body);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
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
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - token is missing or invalid",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient permissions or attempt to modify restricted user",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseUserDTO> changeUserRole(
        @PathVariable Long id,
        @RequestBody @Valid ChangeUserRoleRequest request) {
        return ResponseEntity.ok(service.changeUserRole(id, request.getRole()));
    }

    @Operation(
        summary = "Get paginated list of users",
        description = "Returns a paginated list of users for the admin dashboard with optional search by name or email")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully"),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - token is missing or invalid",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient permissions",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ResponseUserDTO>> getUsers(
        @ParameterObject @PageableDefault(
            sort = "id",
            direction = DESC) Pageable pageable,

        @Parameter(
            description = "Optional search query for filtering users by name or email",
            example = "ivan") @RequestParam(required = false) String search,
        @Parameter(
            description = "Optional parameter for filtering users by roles",
            example = "[\"ADMIN\", \"ORG\"]") @RequestParam(required = false) List<Role> roles) {
        return ResponseEntity.ok(PageResponse.from(service.getUsers(pageable, search, roles)));
    }

    @Operation(
        summary = "Get users by ids",
        description = "Get paginated list of users with given ids")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "400",
            description = "Required user IDs are missing or invalid",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - token is missing or invalid",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient permissions",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/by-ids")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ResponseUserDTO>> getUsersByIds(
        @ParameterObject @PageableDefault(
            sort = "id",
            direction = DESC) Pageable pageable,

        @Parameter(
            description = "Ids of users to search for",
            example = "[1,2,3]") @RequestParam List<Long> ids) {
        return ResponseEntity.ok(PageResponse.from(service.getUsersByIds(pageable, ids)));
    }

    @PatchMapping("/{id}/status")
    @Operation(
        summary = "Change user status",
        description = "Allows admin to change status of an existing user")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User status updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ResponseUserDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid status or request payload",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - token is missing or invalid",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - insufficient permissions or attempt to modify restricted user",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(
            responseCode = "404",
            description = "User not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseUserDTO> changeUserStatus(
        @PathVariable Long id,
        @RequestBody @Valid ChangeUserStatusRequest request) {
        return ResponseEntity.ok(service.changeUserStatus(id, request.getStatus()));
    }
}
