package com.example.springdb.study.jpabook.ch16_transaction_and_locks.second_layer_cache

import com.example.springdb.study.jpabook.ch16_transaction_and_locks.services.Ch16BoardService
import com.example.springdb.study.logger
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals

@SpringBootTest
class Ch16SecondLayerCacheTest {

    private val log = logger()

    @Autowired
    @PersistenceContext
    lateinit var em: EntityManager

    @Autowired
    lateinit var boardService: Ch16BoardService

    @Test
    @Transactional
    fun test_query_cache_across_transactions() {
        // GIVEN
        boardService.set()

        val statistics = em.entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        statistics.isStatisticsEnabled = true
        statistics.clear()

        boardService.findBoardWith2LayerCache()
        log.info("===FIRST===")
        log.info("Query cache hits: ${statistics.queryCacheHitCount}")
        log.info("Query cache puts: ${statistics.queryCachePutCount}")
        // 2025-07-21T21:15:42.375+09:00  INFO 14873 --- [JPA-hibernate-study] [    Test worker] c.e.s.s.j.c.s.Ch16SecondLayerCacheTest   : ===FIRST===
        // 2025-07-21T21:15:42.375+09:00  INFO 14873 --- [JPA-hibernate-study] [    Test worker] c.e.s.s.j.c.s.Ch16SecondLayerCacheTest   : Query cache hits: 0
        // 2025-07-21T21:15:42.375+09:00  INFO 14873 --- [JPA-hibernate-study] [    Test worker] c.e.s.s.j.c.s.Ch16SecondLayerCacheTest   : Query cache puts: 1

        em.clear()
        statistics.clear()

        boardService.findBoardWith2LayerCache()
        log.info("===SECOND===")
        log.info("Query cache hits: ${statistics.queryCacheHitCount}")
        log.info("Query cache puts: ${statistics.queryCachePutCount}")
        // 2025-07-21T21:15:42.378+09:00  INFO 14873 --- [JPA-hibernate-study] [    Test worker] c.e.s.s.j.c.s.Ch16SecondLayerCacheTest   : ===SECOND===
        // 2025-07-21T21:15:42.378+09:00  INFO 14873 --- [JPA-hibernate-study] [    Test worker] c.e.s.s.j.c.s.Ch16SecondLayerCacheTest   : Query cache hits: 0
        // 2025-07-21T21:15:42.378+09:00  INFO 14873 --- [JPA-hibernate-study] [    Test worker] c.e.s.s.j.c.s.Ch16SecondLayerCacheTest   : Query cache puts: 1

        val stop = 1
    }
}
