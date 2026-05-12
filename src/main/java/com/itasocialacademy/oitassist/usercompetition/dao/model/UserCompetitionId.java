package com.itasocialacademy.oitassist.usercompetition.dao.model;

import jakarta.persistence.Column;

import java.io.Serializable;

public class UserCompetitionId implements Serializable {
    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "competition_id")
    private Long competitionId;
}
