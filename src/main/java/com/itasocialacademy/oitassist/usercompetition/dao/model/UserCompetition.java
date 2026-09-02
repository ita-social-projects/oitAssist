package com.itasocialacademy.oitassist.usercompetition.dao.model;

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

    @Column(name = "competition_id", nullable = false, insertable = false, updatable = false)
    private Long competitionId;
}
