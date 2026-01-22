package com.itasocialacademy.oitassist.clean_architecture_example.infrastructure.persistance.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table
public class UserEntity {
    @Id
    private Long id;
    private String username;
    private String password;
    private String email;
}
