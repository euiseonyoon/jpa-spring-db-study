package com.example.springdb.study.jpabook.ch16_transaction_and_locks.services

import com.example.springdb.study.jpabook.ch16_transaction_and_locks.models.Ch16AttachedFile
import com.example.springdb.study.jpabook.ch16_transaction_and_locks.models.Ch16Board
import com.example.springdb.study.jpabook.ch16_transaction_and_locks.models.Ch16UpdateDto
import com.example.springdb.study.jpabook.ch16_transaction_and_locks.repositories.Ch16BoardRepository
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class Ch16BoardService(
    private val em: EntityManager,
    private val boardRepository: Ch16BoardRepository
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun updateUsingJpaDirectly(updateDto: Ch16UpdateDto, delayTime: Long?): Ch16Board {
        val board = em.find(Ch16Board::class.java, updateDto.targetId)
        updateDto.newTitle?.let { board.title = it }
        updateDto.newContext?.let { board.context = it }

        if (delayTime != null) {
            Thread.sleep(delayTime)
        }
        em.flush()
        em.clear()
        return board
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun updateUsingRepository(updateDto: Ch16UpdateDto, delayTime: Long?): Ch16Board {
        val board = boardRepository.findById(updateDto.targetId).orElse(null)
        updateDto.newTitle?.let { board.title = it }
        updateDto.newContext?.let { board.context = it }

        if (delayTime != null) {
            Thread.sleep(delayTime)
        }
        val updatedBoard = boardRepository.save(board)
        return updatedBoard
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun findUsingJpaDirectly(id: Long, delayTime: Long): Ch16Board {
        // SELECT 만 해도 version이 업 된다.
        val board = em.find(Ch16Board::class.java, id, LockModeType.OPTIMISTIC)

        Thread.sleep(delayTime)

        em.flush()
        return board
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun findUsingRepository(id: Long, delayTime: Long): Ch16Board? {
        // SELECT 만 해도 version이 업 된다.
        val board = boardRepository.searchById(id)

        Thread.sleep(delayTime)

        em.flush()
        return board
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun addAttachedFiles(id: Long, files: List<Ch16AttachedFile>): Ch16Board {
        val board = em.find(Ch16Board::class.java, id, LockModeType.OPTIMISTIC_FORCE_INCREMENT)
        board.addAttachedFiles(files)
        em.flush()
        return board
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun addAttachedFiles2(id: Long, files: List<Ch16AttachedFile>): Ch16Board {
        val board = em.find(Ch16Board::class.java, id)
        board.addAttachedFiles(files)
        em.lock(board, LockModeType.OPTIMISTIC_FORCE_INCREMENT)
        em.persist(board)
        em.flush()
        return board
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun readWithPessimisticLock(targetId: Long, delayTime: Long?): Ch16Board {
        val board = em.find(Ch16Board::class.java, targetId, LockModeType.PESSIMISTIC_READ)

        if (delayTime != null) {
            Thread.sleep(delayTime)
        }

        return board
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun updateWithPessimisticLock(updateDto: Ch16UpdateDto, delayTime: Long?, lockTimeOut: Long?): Ch16Board {
        // Lock Timeout 설정 (PostgreSQL)
        if (lockTimeOut != null) {
            em.createNativeQuery("SET LOCAL lock_timeout = '${lockTimeOut}ms'").executeUpdate()
        }

        val properties: MutableMap<String, Any> = mutableMapOf()
        if (lockTimeOut != null) {
            properties["javax.persistence.lock.timeout"] = lockTimeOut
        }

        val board = em.find(Ch16Board::class.java, updateDto.targetId, LockModeType.PESSIMISTIC_WRITE, properties)
        updateDto.newTitle?.let { board.title = it }
        updateDto.newContext?.let { board.context = it }

        if (delayTime != null) {
            Thread.sleep(delayTime)
        }

        em.flush()
        return board
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun updateWithPessimisticLockSpringDataJpa(updateDto: Ch16UpdateDto, delayTime: Long?, lockTimeOut: Long?): Ch16Board {
        // Lock Timeout 설정 (PostgreSQL)
        if (lockTimeOut != null) {
            em.createNativeQuery("SET LOCAL lock_timeout = '${lockTimeOut}ms'").executeUpdate()
        }

        val board = boardRepository.searchByIdForPessimisticUpdate(updateDto.targetId)
        updateDto.newTitle?.let { board!!.title = it }
        updateDto.newContext?.let { board!!.context = it }

        if (delayTime != null) {
            Thread.sleep(delayTime)
        }

        em.flush()
        return board!!
    }

}
