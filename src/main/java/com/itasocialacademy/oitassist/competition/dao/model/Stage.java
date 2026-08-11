package com.itasocialacademy.oitassist.competition.dao.model;

import com.itasocialacademy.oitassist.competition.dao.enums.StageScope;
import com.itasocialacademy.oitassist.competition.dao.enums.StageStatus;
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
    name = "stages",
    uniqueConstraints = @UniqueConstraint(columnNames = {"competition_id", "title"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class Stage extends CompetitionEvent {
    @Column(name = "competition_id", nullable = false)
    private Long competitionId;

    @Column(name = "sort_position", nullable = false)
    private Short sortPosition;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    private StageScope scope;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StageStatus status = StageStatus.SCHEDULED;
}