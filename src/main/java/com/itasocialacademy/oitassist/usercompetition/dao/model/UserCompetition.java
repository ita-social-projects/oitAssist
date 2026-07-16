package com.itasocialacademy.oitassist.usercompetition.dao.model;

import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import com.itasocialacademy.oitassist.core.rest.entity.Entity;
import com.itasocialacademy.oitassist.usercompetition.dao.enums.UserCompetitionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@jakarta.persistence.Entity
@Table(name = "user_competition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCompetition implements Entity<UserCompetitionId> {
    @EmbeddedId
    private UserCompetitionId id;

    @Column(name = "author_id", nullable = false, insertable = false, updatable = false)
    private Long authorId;

    @ManyToOne(optional = false)
    @MapsId("competitionId")
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competitionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserCompetitionStatus status;

    @Column(name = "is_read", nullable = false, columnDefinition = "boolean default false")
    private boolean isRead;
}