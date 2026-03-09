package com.itasocialacademy.oitassist.task.dao.model;

import com.itasocialacademy.oitassist.core.rest.entity.LongEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Task implements LongEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "competition_id", nullable = false)
    private Long competitionId;
}
