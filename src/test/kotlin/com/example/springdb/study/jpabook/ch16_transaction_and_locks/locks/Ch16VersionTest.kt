package com.example.springdb.study.jpabook.ch16_transaction_and_locks.locks

import com.example.springdb.study.jpabook.ch16_transaction_and_locks.models.Ch16AttachedFile
import com.example.springdb.study.jpabook.ch16_transaction_and_locks.models.Ch16Board
import com.example.springdb.study.jpabook.ch16_transaction_and_locks.models.Ch16UpdateDto
import com.example.springdb.study.jpabook.ch16_transaction_and_locks.repositories.Ch16BoardRepository
import com.example.springdb.study.jpabook.ch16_transaction_and_locks.services.Ch16BoardService
import com.example.springdb.study.logger
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.LockModeType
import jakarta.persistence.OptimisticLockException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.orm.ObjectOptimisticLockingFailureException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DataJpaTest
@Import(Ch16BoardService::class)
class Ch16VersionTest {
    private val log = logger()

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
     * 상황:
     *         엔티티(Ch16Board)에 @Version 이라는 어노테이션이 붙은 필드가 있다.
     *         Db 작업시, LockModeType을 설정하지 않있다. (`LockModeType.NONE`으로 적용됨)
     *         트렌젝션1 조회 -> 트렌젝션2가 같은 데이터를 조회&수정 후 커밋(version이 올라감) -> 트렌젝션1이 해당 데이터를 수정 후 커밋
     *
     * 기대:
     *         서로 조회만 하는 경우는 version이 상향 되진 않지만 트렌젝션2가 수정 & 커밋을 했기 때문에 version이 업 되었다.
     *         그 후 트렌젝션1이 수정 후 커밋을 하려고 하는데 이미 트렌젝션2에 의해 version이 업 되었다.
     *         그로 인해 에러가 발생한다.
     * 이점:
     *         두 번의 갱신 문제를 예방한다 (항상 먼저 커밋하는 것이 반영되게 함)
     * */
    @Test
    @DisplayName("DB 작업시, LockModeType.NONE  (JPA 직접 사용)")
    fun `test search with LockModeType NONE hibernate`() {
        val boardId = board.id!!
        val executor = Executors.newFixedThreadPool(2)

        val future1 = executor.submit<Ch16Board> {
            boardService.updateUsingJpaDirectly(
                updateDto = Ch16UpdateDto(boardId, "tx1 new title", null),
                delayTime = 300L
            )
        }

        val future2 = executor.submit<Ch16Board> {
            boardService.updateUsingJpaDirectly(
                updateDto = Ch16UpdateDto(boardId, "tx2 new title", null),
                delayTime = null
            )
        }

        Thread.sleep(50)

        try {
            future2.get()
            future1.get()
        } catch (e: ExecutionException) {
            // JPA를 바로 사용하면 jakarta.persistence.OptimisticLockException 발생
            assertTrue { e.cause is OptimisticLockException }
        }

        executor.shutdown()
    }

    @Test
    @DisplayName("DB 작업시, LockModeType.NONE  ( JpaRepository(SpringDataJpa) 사용 )" )
    fun `test search with LockModeType NONE SpringDataJpa`() {
        val boardId = board.id!!
        val executor = Executors.newFixedThreadPool(2)

        val future1 = executor.submit<Ch16Board> {
            boardService.updateUsingRepository(
                updateDto = Ch16UpdateDto(boardId, "tx1 new title", null),
                delayTime = 300L
            )
        }

        val future2 = executor.submit<Ch16Board> {
            boardService.updateUsingRepository(
                updateDto = Ch16UpdateDto(boardId, "tx2 new title", null),
                delayTime = null
            )
        }

        Thread.sleep(50)

        try {
            future2.get()
            future1.get()
        } catch (e: ExecutionException) {
            // SpringDataJpa를 사용하면 org.springframework.orm.ObjectOptimisticLockingFailureException 발생
            assertTrue { e.cause is ObjectOptimisticLockingFailureException }
        }

        executor.shutdown()
    }

    /**
     * 상황:
     *         트렌젝션1 `LockModeType.OPTIMISTIC`을 사용하여 조회
     *         트렌젝션2가 조회&수정 후 커밋
     *         트렌젝션1을 조회 후 아무것도 하지 않고 커밋
     *
     * 기대:
     *         트렌젝션1 이 조회 후, 아무런 수정조치 없이 그대로 커밋해도,
     *         트렌젝션2로 인해 version이 바뀌었기 때문에 에러 발생
     *
     * 이점:
     *         Dirty read(아직 커밋되지 않은 데이터를 읽음), Non-Repeatable read(조회 시마다 데이터의 내용이 바뀜)
     *         을 방지 할 수 있다.
     * */
    @Test
    fun `test search with LockModeType OPTIMISTIC`() {
        val boardId = board.id!!
        val executor = Executors.newFixedThreadPool(2)

        val future1 = executor.submit<Ch16Board> {
            boardService.findUsingJpaDirectly(
                id = boardId,
                delayTime = 300L
            )
        }

        val future2 = executor.submit<Ch16Board> {
            boardService.updateUsingJpaDirectly(
                updateDto = Ch16UpdateDto(boardId, "tx2 new title", null),
                delayTime = null
            )
        }

        Thread.sleep(50)

        try {
            future2.get()
            future1.get()
        } catch (e: Exception) {
            assertTrue { e.cause is ObjectOptimisticLockingFailureException }
        }

        executor.shutdown()
    }

    @Test
    fun `test search with LockModeType OPTIMISTIC2`() {
        val boardId = board.id!!
        val executor = Executors.newFixedThreadPool(2)

        val future1 = executor.submit<Ch16Board> {
            boardService.findUsingRepository(
                id = boardId,
                delayTime = 300L
            )
        }

        val future2 = executor.submit<Ch16Board> {
            boardService.updateUsingJpaDirectly(
                updateDto = Ch16UpdateDto(boardId, "tx2 new title", null),
                delayTime = null
            )
        }

        Thread.sleep(50)

        try {
            future2.get()
            future1.get()
        } catch (e: Exception) {
            assertTrue { e.cause is ObjectOptimisticLockingFailureException }
        }

        executor.shutdown()
    }

    /**
     * OPTIMISTIC_FORCE_INCREMENT 를 사용하는 경우, 논리적인 단위의 엔티티 묶음을 관리 할 수 있다.
     *
     * 예를 들어 아래처럼 어떠한 Board에 첨부파일을 추가하는 시나리오.
     * Board의 내용에는 변함이 없다. 다만 해당 Board의 첨부파일이 추가되기만 한다.
     * 하지만 추가하는 과정에서 Board의 version을 의도적으로 상향한다면,
     * (Board + 첨부파일들(추가)) 의 논리적인 묶음으로 엔티티들을 묶어서 관리 한다.
     *
     * */
    @Test
    fun `test LockModeType OPTIMISTIC_FORCE_INCREMENT`() {
        // GIVEN 1 : 이미 board 하나가 저장 되어 있음
        val savedBoard = boardRepository.findById(board.id!!).orElse(null)
        val initialBoardVersion = savedBoard.getVersion()
        log.info("savedBoard initial version={}",initialBoardVersion)

        // THEN 1
        assertEquals(0L, initialBoardVersion)

        // GIVEN 2
        val filesToAttach = (0..1).map {
            Ch16AttachedFile("file_{$it}")
        }
        val updatedBoard = boardService.addAttachedFiles(board.id!!, filesToAttach)
        // THEN 2
        assertEquals(initialBoardVersion + 1, updatedBoard.getVersion())

        // GIVEN 3
        val updatedAgainBoard = boardService.addAttachedFiles2(board.id!!, listOf(Ch16AttachedFile("another file")))
        // THEN 3
        assertEquals(initialBoardVersion + 2, updatedAgainBoard.getVersion())
    }

}
