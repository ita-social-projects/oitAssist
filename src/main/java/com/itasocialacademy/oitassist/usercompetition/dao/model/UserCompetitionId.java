package com.itasocialacademy.oitassist.usercompetition.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserCompetitionId implements Serializable {
    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "competition_id")
    private Long competitionId;
}
