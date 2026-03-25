package com.itasocialacademy.oitassist.filemanager.dao.model;

import com.itasocialacademy.oitassist.core.rest.entity.LongEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "file_assets")
@NoArgsConstructor
@Getter
@Setter
public class FileAsset implements LongEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
