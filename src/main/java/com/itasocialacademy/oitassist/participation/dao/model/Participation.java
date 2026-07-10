package com.itasocialacademy.oitassist.participation.dao.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "participants")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Participation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "competition_id", nullable = false)
    private Long competitionId;

    @Column(name = "stage_id", nullable = false)
    private Long stageId;
}
