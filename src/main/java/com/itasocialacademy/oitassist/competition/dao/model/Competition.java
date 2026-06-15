package com.itasocialacademy.oitassist.competition.dao.model;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.modulith.NamedInterface;

@Entity
@Table(name = "competitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString()
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NamedInterface("CompetitionEntity")
public class Competition extends CompetitionEvent {
    @Enumerated(EnumType.STRING)
    @Column(name = "competition_status", nullable = false)
    private CompetitionStatus competitionStatus;
}
