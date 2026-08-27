package com.lab.resource;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "asset_category", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class AssetCategory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false) public String name;
    public boolean serialized = true;
    public boolean highValue = false;
    public boolean enabled = true;
    public String description;
    public LocalDateTime createdAt = LocalDateTime.now();
    public LocalDateTime updatedAt = LocalDateTime.now();
    @Version public int version;
}
