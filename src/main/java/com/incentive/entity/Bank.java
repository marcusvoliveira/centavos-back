package com.incentive.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "banks")
public class Bank extends PanacheEntity {

    @Column(nullable = false, unique = true, length = 10)
    public String code;

    @Column(nullable = false, length = 100)
    public String name;

    public static List<Bank> findAllOrderedByName() {
        return list("ORDER BY name");
    }
}
