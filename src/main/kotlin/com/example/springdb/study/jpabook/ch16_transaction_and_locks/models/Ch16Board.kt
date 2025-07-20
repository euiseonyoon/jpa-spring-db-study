package com.example.springdb.study.jpabook.ch16_transaction_and_locks.models

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Version

@Entity
class Ch16Board {
    @Id
    @GeneratedValue
    val id: Long? = null

    var title: String? = null
    var context: String? = null

    /**
     * Version은 아래의 타입에만 적용 가능
     *     Long, Int, Short, Timestamp
     *
     * 어떠한 쿼리를 할떄, 버전관리가 필요한 락을 사용하는 경우 @Version 어노테이션이 무조건 필요하다.
     *
     * LockMode.OPTIMISTIC,  OPTIMISTIC_FORCE_INCREMENT, PESSIMISTIC_FORCE_INCREMENT를 사용한다면
     * 해당 엔티티에는 @Version 어노테이션이 필수이다.
     * */
    @Version
    private var version: Long = 0

    constructor()
    constructor(title: String, context: String) {
        this.title = title
        this.context = context
    }
}
