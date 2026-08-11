package com.itasocialacademy.oitassist.participation.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class Application extends ParticipationRequestEvent {
    @Column(name = "processed_by")
    private Long processedBy;

    @Override
    public Long getUserId() {
        return getIssuedBy();
    }
}
