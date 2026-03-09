package com.itasocialacademy.oitassist.usercompetition.dao.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "user_competition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString()
@EqualsAndHashCode()
@Builder
public class UserCompetition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "competition_id", nullable = false)
    private Long competitionId;
}
