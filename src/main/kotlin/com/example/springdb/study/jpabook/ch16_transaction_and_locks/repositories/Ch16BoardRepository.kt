package com.example.springdb.study.jpabook.ch16_transaction_and_locks.repositories

import com.example.springdb.study.jpabook.ch16_transaction_and_locks.models.Ch16Board
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface Ch16BoardRepository : JpaRepository<Ch16Board, Long> {

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT b FROM Ch16Board b WHERE id = :id")
    fun searchById(@Param("id") id: Long): Ch16Board?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Ch16Board b WHERE b.id = :id")
    fun searchByIdForPessimisticUpdate(@Param("id") id: Long): Ch16Board?
}
