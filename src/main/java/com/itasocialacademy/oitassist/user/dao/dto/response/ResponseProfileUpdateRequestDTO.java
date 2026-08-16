package com.itasocialacademy.oitassist.user.dao.dto.response;

import com.itasocialacademy.oitassist.user.dao.enums.UpdateRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.Instant;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Schema(description = "Profile change request DTO for response")
public class ResponseProfileUpdateRequestDTO {
    @Schema(description = "Request ID", example = "1")
    private Long id;

    @Schema(description = "Request status", example = "PENDING")
    private UpdateRequestStatus status;

    @Schema(description = "Old first name", example = "Bob")
    private String oldFirstName;

    @Schema(description = "Old last name", example = "Smith")
    private String oldLastName;

    @Schema(description = "Old middle name", example = "John")
    private String oldMiddleName;

    @Schema(description = "Old phone number", example = "380931111111")
    private String oldPhoneNumber;

    @Schema(description = "New first name", example = "Alice")
    private String newFirstName;

    @Schema(description = "New last name", example = "Johnson")
    private String newLastName;

    @Schema(description = "New middle name", example = "Marie")
    private String newMiddleName;

    @Schema(description = "New phone number", example = "380932222222")
    private String newPhoneNumber;

    @Schema(description = "Requested at", example = "2024-01-01T00:00:00Z")
    private Instant requestedAt;

    @Schema(description = "Reviewed at", example = "2024-01-02T00:00:00Z")
    private Instant reviewedAt;

    @Schema(description = "Reject reason", example = "Invalid data")
    private String rejectReason;
}
