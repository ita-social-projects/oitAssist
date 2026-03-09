package com.itasocialacademy.oitassist.competition.dao.model;

import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionLevel;
import com.itasocialacademy.oitassist.competition.dao.enums.CompetitionStatus;
import com.itasocialacademy.oitassist.core.rest.entity.LongEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.modulith.NamedInterface;

@Entity
@Table(name = "competitions")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString()
@EqualsAndHashCode()
public class Competition implements LongEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private CompetitionLevel level;

    @Enumerated(EnumType.STRING)
    @Column(name = "competition_status", nullable = false)
    private CompetitionStatus competitionStatus;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "start_at", nullable = false)
    private ZonedDateTime startAt;

    @Column(name = "end_at")
    private ZonedDateTime endAt;
}
