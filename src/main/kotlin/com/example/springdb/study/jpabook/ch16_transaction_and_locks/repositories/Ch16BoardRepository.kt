package com.example.springdb.study.jpabook.ch16_transaction_and_locks.repositories

import com.example.springdb.study.jpabook.ch16_transaction_and_locks.models.Ch16Board
import org.springframework.data.jpa.repository.JpaRepository

interface Ch16BoardRepository : JpaRepository<Ch16Board, Long>
