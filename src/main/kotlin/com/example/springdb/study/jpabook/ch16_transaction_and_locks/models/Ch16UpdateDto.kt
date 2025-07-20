package com.example.springdb.study.jpabook.ch16_transaction_and_locks.models

data class Ch16UpdateDto(
    val targetId: Long,
    val newTitle: String?,
    val newContext: String?
)
