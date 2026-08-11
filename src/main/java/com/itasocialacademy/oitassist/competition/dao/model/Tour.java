package com.itasocialacademy.oitassist.competition.dao.model;

import com.itasocialacademy.oitassist.competition.dao.enums.ExecutionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "tours",
    uniqueConstraints = @UniqueConstraint(columnNames = {"stage_id", "title"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class Tour extends CompetitionEvent {
    @Column(name = "stage_id", nullable = false)
    private Long stageId;

    @Column(name = "sort_position", nullable = false)
    private Short sortPosition;

    @Column(name = "location", nullable = false)
    private String location;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false)
    private ExecutionStatus executionStatus = ExecutionStatus.SCHEDULED;
}