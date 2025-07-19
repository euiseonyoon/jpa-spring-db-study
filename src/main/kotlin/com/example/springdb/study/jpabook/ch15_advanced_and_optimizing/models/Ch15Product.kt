package com.example.springdb.study.jpabook.ch15_advanced_and_optimizing.models

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Ch15Product {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    val id: Long? = null

    @Column(nullable = false)
    var name: String? = null

    @Column(nullable = false)
    var price: Int? = null

    constructor()
    constructor(name: String, price: Int) {
        this.name = name
        this.price = price
    }
}
