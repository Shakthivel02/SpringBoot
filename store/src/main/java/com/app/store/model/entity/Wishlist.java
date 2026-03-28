package com.app.store.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Wishlist extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String studentId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "wishlist_items", joinColumns = @JoinColumn(name = "wishlist_id"))
    private List<WishlistItem> items = new ArrayList<>();
}
