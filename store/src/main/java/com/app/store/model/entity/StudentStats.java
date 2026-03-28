package com.app.store.model.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class StudentStats extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String studentId;
    
    private Integer totalCoinsEarned;
}
