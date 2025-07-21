package com.example.springdb.study.jpabook.ch16_transaction_and_locks.models

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Version

@Entity
class Ch16Board {
    @Id
    @GeneratedValue
    val id: Long? = null

    var title: String? = null
    var context: String? = null

    @OneToMany(mappedBy = "board", cascade = [CascadeType.ALL], orphanRemoval = true)
    var attachedFiles: MutableSet<Ch16AttachedFile> = mutableSetOf()

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

    fun getVersion(): Long = version

    constructor()
    constructor(title: String, context: String) {
        this.title = title
        this.context = context
    }

    fun addAttachedFiles(files: List<Ch16AttachedFile>) {
        this.attachedFiles += files

        files.forEach { file ->
            if (file.board != this) {
                file.board = this
            }
        }
    }
}
