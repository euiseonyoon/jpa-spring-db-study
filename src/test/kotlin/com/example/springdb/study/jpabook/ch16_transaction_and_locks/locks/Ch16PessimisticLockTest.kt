package com.example.springdb.study.jpabook.ch16_transaction_and_locks.locks

import com.example.springdb.study.jpabook.ch16_transaction_and_locks.models.Ch16Board
import com.example.springdb.study.jpabook.ch16_transaction_and_locks.models.Ch16UpdateDto
import com.example.springdb.study.jpabook.ch16_transaction_and_locks.repositories.Ch16BoardRepository
import com.example.springdb.study.jpabook.ch16_transaction_and_locks.services.Ch16BoardService
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.LockTimeoutException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.test.assertTrue

@SpringBootTest
class Ch16PessimisticLockTest {

    @Autowired
    lateinit var emf: EntityManagerFactory

    @Autowired
    lateinit var boardService: Ch16BoardService

    @Autowired
    lateinit var boardRepository: Ch16BoardRepository

    @Autowired
    lateinit var em: EntityManager

    lateinit var board: Ch16Board

    @BeforeEach
    fun init() {
        // @DataJpaTest는 각 테스트를 Transaction 으로 묶는다.
        // 테스트를 하려면 이미 데이터가 저장되어 있어야 되는데 커밋이 되지 않으니 안된다.
        // 그래서 의도적으로 트렌젝션을 만들고 억지로 커밋한다.
        val em = emf.createEntityManager() // <- Spring의 프록시 아님

        val initTx = em.transaction
        initTx.begin()

        val newBoard = Ch16Board("title", "this is context")
        this.board = newBoard
        em.persist(newBoard)
        em.flush()
        em.clear()

        initTx.commit()
        em.close()
    }

    /**
     * 아래 Pessimistic lock은 DB마다 다르게 적용될 수 있어서, 실제로 사용하는 PostgreSQL을 사용하였다.
     * */
    @Test
    @DisplayName("PostgreSQL에서 read락은 공유락 이기 때문에 여러 트렌젝션이 획득해도 문제되지 않는다.")
    fun `test LockModeType Pessimistic Read`() {
        val boardId = board.id!!
        val executor = Executors.newFixedThreadPool(2)

        // WHEN
        val future1 = executor.submit<Ch16Board> {
            boardService.readWithPessimisticLock(
                targetId = boardId,
                delayTime = 300L
            )
        }

        Thread.sleep(50)

        val future2 = executor.submit<Ch16Board> {
            boardService.readWithPessimisticLock(
                targetId = boardId,
                delayTime = null
            )
        }

        // THEN
        assertDoesNotThrow {
            future1.get()
            future2.get()
        }

        executor.shutdown()
    }

    @Test
    @DisplayName("""
       Pessimistic WRITE lock을 걸었을 때, 해당 lock이 풀릴때까지 대기, 그 후에 프로세스가 진행된다.
       아래 테스트에서는 두번째 트렌젝션에 lockTimeOut을 주지 않았다.
    """)
    fun `test LockModeType Pessimistic Write No lock time out`() {
        val boardId = board.id!!
        val executor = Executors.newFixedThreadPool(2)

        // WHEN
        val delayTime = 1000L
        val future1 = executor.submit<Ch16Board> {
            boardService.updateWithPessimisticLock(
                updateDto = Ch16UpdateDto(boardId, "tx1 new title", null),
                delayTime = delayTime,
                lockTimeOut = null,
            )
        }

        // future1이 lock을 잡을 수 있게 약간 늦게 시작.
        Thread.sleep(50)

        val future2 = executor.submit<Ch16Board> {
            boardService.updateWithPessimisticLock(
                updateDto = Ch16UpdateDto(boardId, "tx2 new title", null),
                delayTime = null,
                lockTimeOut = null,
            )
        }

        // THEN
        val littleMargin = 100L
        val start = System.currentTimeMillis()
        future1.get()
        future2.get()
        val end = System.currentTimeMillis()
        val executionTime = end - start

        assertTrue { abs(executionTime - delayTime) <= littleMargin }

        val updatedBoard = boardRepository.findById(boardId)
        assertTrue{ updatedBoard.isPresent && updatedBoard.get().title == "tx2 new title" }

        executor.shutdown()
    }

    @Test
    @DisplayName("""
       Pessimistic WRITE lock을 걸었을 때, 해당 lock이 풀릴때까지 대기, 그 후에 프로세스가 진행된다.
       아래 테스트에서는 두번째 트렌젝션에 lockTimeOut을 주었다.
       결과는, 트렌젝션1이 락을 너무 오래 잡고 있어서, 두번째 트렌젝션의 lockTimOut 때문에 예외 발생한다.
    """)
    fun `test LockModeType Pessimistic Write with lock time out`() {
        val boardId = board.id!!
        val executor = Executors.newFixedThreadPool(2)

        val delayTime = 500L
        val lockTimeOut = 100L

        // WHEN
        val future1 = executor.submit<Ch16Board> {
            boardService.updateWithPessimisticLock(
                updateDto = Ch16UpdateDto(boardId, "tx1 new title", null),
                delayTime = delayTime,
                lockTimeOut = null,
            )
        }

        // future1이 lock을 잡을 수 있게 약간 늦게 시작.
        Thread.sleep(50)

        val future2 = executor.submit<Ch16Board> {
            boardService.updateWithPessimisticLock(
                updateDto = Ch16UpdateDto(boardId, "tx2 new title", null),
                delayTime = null,
                lockTimeOut = lockTimeOut,
            )
        }

        // THEN
        val cause = assertThrows<ExecutionException>{
            future1.get()
            future2.get()
        }.cause
        assertTrue { cause is LockTimeoutException }

        executor.shutdown()
    }

}
