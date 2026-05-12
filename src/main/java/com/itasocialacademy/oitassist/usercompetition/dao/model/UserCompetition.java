package com.itasocialacademy.oitassist.usercompetition.dao.model;

import com.itasocialacademy.oitassist.competition.dao.model.Competition;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_competition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCompetition {
    @EmbeddedId
    private UserCompetitionId id;

    @Column(name = "author_id", nullable = false, insertable = false, updatable = false)
    private Long authorId;

    @ManyToOne(optional = false)
    @MapsId("competitionId")
    @JoinColumn(name = "competition_id", nullable = false)
    private Competition competitionId;
}
