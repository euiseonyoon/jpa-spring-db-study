package com.example.springdb.study.jpabook.ch15_advanced_and_optimizing.models

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import java.time.LocalDateTime

@Entity
class Ch15Order {
    @Id
    @GeneratedValue
    val id: Long? = null

    @Column(nullable = false)
    var orderDate: LocalDateTime = LocalDateTime.now()

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: Ch15OrderStatus = Ch15OrderStatus.READY

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    var orderItems: MutableSet<Ch15OrderItem> = mutableSetOf()

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    var member: Ch15Member? = null

    fun assignItem(
        item: Ch15Item,
        count: Int
    ) {
        val orderItem = Ch15OrderItem()
        orderItem.order = this
        orderItem.item = item
        orderItem.price = item.price
        orderItem.count = count

        if (!this.orderItems.contains(orderItem)) {
            this.orderItems.add(orderItem)
        }
        if (!item.orderItems.contains(orderItem)) {
            item.orderItems.add(orderItem)
        }
    }

    fun assignMember(member: Ch15Member) {
        this.member = member
        if (!member.orders.contains(this)) {
            member.orders.add(this)
        }
    }
}
