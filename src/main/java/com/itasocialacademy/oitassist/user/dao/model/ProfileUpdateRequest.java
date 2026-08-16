package com.itasocialacademy.oitassist.user.dao.model;

import com.itasocialacademy.oitassist.user.dao.enums.UpdateRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "profile_update_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UpdateRequestStatus status;

    @Column(name = "old_first_name", nullable = false)
    private String oldFirstName;

    @Column(name = "old_last_name", nullable = false)
    private String oldLastName;

    @Column(name = "old_middle_name")
    private String oldMiddleName;

    @Column(name = "old_phone_number")
    private String oldPhoneNumber;

    @Column(name = "new_first_name", nullable = false)
    private String newFirstName;

    @Column(name = "new_last_name", nullable = false)
    private String newLastName;

    @Column(name = "new_middle_name")
    private String newMiddleName;

    @Column(name = "new_phone_number")
    private String newPhoneNumber;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;
}
