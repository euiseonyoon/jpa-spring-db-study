package com.example.springdb.study.jpabook.ch16_transaction_and_locks.models

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.springframework.cache.annotation.Cacheable

@Cacheable
@Cache(
    usage = CacheConcurrencyStrategy.READ_WRITE,
    // region = "com.example.springdb.study.jpabook.ch16_transaction_and_locks.models.Ch16AttachedFile"
)
@Entity
class Ch16AttachedFile {
    @Id
    @GeneratedValue
    val id: Long? = null

    var fileName: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    var board: Ch16Board? = null

    constructor()
    constructor(fileName: String) {
        this.fileName = fileName
    }
}
