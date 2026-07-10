package com.itasocialacademy.oitassist.participation.dao.model;

import com.itasocialacademy.oitassist.participation.dao.enums.RequestStatus;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class ParticipationRequestEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "competition_id", nullable = false)
    private Long competitionId;

    @Column(name = "stage_id", nullable = false)
    private Long stageId;

    @CreatedBy
    @Column(name = "issued_by", nullable = false)
    private Long issuedBy;

    @CreatedDate
    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RequestStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;
}
