package com.example.springdb.study.jpabook.ch15_advanced_and_optimizing.models

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["order", "item"])
    ]
)
class Ch15OrderItem {
    @Id
    @GeneratedValue
    val id: Long? = null

    @Column(nullable = false)
    var price: Int? = null

    @Column(nullable = false)
    var count: Int? = null

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    var order: Ch15Order? = null

    // item proxy를 사용하기 위해 의도적으로 lazy 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    var item: Ch15Item? = null
}
