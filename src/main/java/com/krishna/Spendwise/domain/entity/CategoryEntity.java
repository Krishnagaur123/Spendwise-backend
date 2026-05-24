package com.krishna.Spendwise.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Expense/income category. Categories are either system defaults ({@code isDefault=true},
 * seeded on user registration) or user-created customs. Defaults cannot be deleted.
 */
@Entity
@Table(name = "tbl_categories")
@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class CategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /** {@code "INCOME"} or {@code "EXPENSE"} — determines which table a transaction is saved to. */
    private String type;

    private String icon;

    /** {@code true} = system default (Salary, Food, Rent, etc.) — protected from user deletion. */
    @Builder.Default
    private Boolean isDefault = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private ProfileEntity profile;
}
