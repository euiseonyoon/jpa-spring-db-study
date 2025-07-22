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
    fun test_query_cache_across_transactions() {
        // GIVEN
        boardService.set()

        val statistics = em.entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        statistics.isStatisticsEnabled = true
        statistics.clear()

        boardService.findBoardWith2LayerCache()
        log.info("===FIRST CALL STATS===")
        log.info("Query cache hits: ${statistics.queryCacheHitCount}")
        log.info("Query cache puts: ${statistics.queryCachePutCount}")
        log.info("Entity cache hits: ${statistics.secondLevelCacheHitCount}") // Also check entity cache
        log.info("Entity cache puts: ${statistics.secondLevelCachePutCount}")
        log.info("DB Query Executions: ${statistics.queryExecutionCount}")
        log.info("DB Entity Loads: ${statistics.entityLoadCount}")
//        2025-07-22T12:04:28.722+09:00  INFO 89544 --- [JPA-hibernate-study] [    Test worker] c.e.s.s.j.c.s.Ch16SecondLayerCacheTest   : ===FIRST CALL STATS===
//        2025-07-22T12:04:28.722+09:00  INFO 89544 --- [JPA-hibernate-study] [    Test worker] c.e.s.s.j.c.s.Ch16SecondLayerCacheTest   : Query cache hits: 0
//        2025-07-22T12:04:28.722+09:00  INFO 89544 --- [JPA-hibernate-study] [    Test worker] c.e.s.s.j.c.s.Ch16SecondLayerCacheTest   : Query cache puts: 1
//        2025-07-22T12:04:28.722+09:00  INFO 89544 --- [JPA-hibernate-study] [    Test worker] c.e.s.s.j.c.s.Ch16SecondLayerCacheTest   : Entity cache hits: 0
//        2025-07-22T12:04:28.722+09:00  INFO 89544 --- [JPA-hibernate-study] [    Test worker] c.e.s.s.j.c.s.Ch16SecondLayerCacheTest   : Entity cache puts: 3
//        2025-07-22T12:04:28.722+09:00  INFO 89544 --- [JPA-hibernate-study] [    Test worker] c.e.s.s.j.c.s.Ch16SecondLayerCacheTest   : DB Query Executions: 1
//        2025-07-22T12:04:28.723+09:00  INFO 89544 --- [JPA-hibernate-study] [    Test worker] c.e.s.s.j.c.s.Ch16SecondLayerCacheTest   : DB Entity Loads: 3

        boardService.findBoardWith2LayerCache()
        log.info("===SECOND CALL STATS===")
        log.info("Query cache hits: ${statistics.queryCacheHitCount}") // Should now be 1 or more
        log.info("Query cache puts: ${statistics.queryCachePutCount}")
        log.info("Entity cache hits: ${statistics.secondLevelCacheHitCount}")
        log.info("Entity cache puts: ${statistics.secondLevelCachePutCount}")
        log.info("DB Query Executions: ${statistics.queryExecutionCount}")
        log.info("DB Entity Loads: ${statistics.entityLoadCount}")
//        2025-07-22T12:04:41.425+09:00  INFO 89544 --- [JPA-hibernate-study] [    Test worker] c.e.s.s.j.c.s.Ch16SecondLayerCacheTest   : ===SECOND CALL STATS===
//        2025-07-22T12:04:41.425+09:00  INFO 89544 --- [JPA-hibernate-study] [    Test worker] c.e.s.s.j.c.s.Ch16SecondLayerCacheTest   : Query cache hits: 0
//        2025-07-22T12:04:41.426+09:00  INFO 89544 --- [JPA-hibernate-study] [    Test worker] c.e.s.s.j.c.s.Ch16SecondLayerCacheTest   : Query cache puts: 2
//        2025-07-22T12:04:41.426+09:00  INFO 89544 --- [JPA-hibernate-study] [    Test worker] c.e.s.s.j.c.s.Ch16SecondLayerCacheTest   : Entity cache hits: 0
//        2025-07-22T12:04:41.426+09:00  INFO 89544 --- [JPA-hibernate-study] [    Test worker] c.e.s.s.j.c.s.Ch16SecondLayerCacheTest   : Entity cache puts: 6
//        2025-07-22T12:04:41.426+09:00  INFO 89544 --- [JPA-hibernate-study] [    Test worker] c.e.s.s.j.c.s.Ch16SecondLayerCacheTest   : DB Query Executions: 2
//        2025-07-22T12:04:41.426+09:00  INFO 89544 --- [JPA-hibernate-study] [    Test worker] c.e.s.s.j.c.s.Ch16SecondLayerCacheTest   : DB Entity Loads: 6

        /**
         * TODO: 캐싱이 잘 안되는것 같다. 원인은 infinispan 이랑 뭔가 잘 안되는것 같은데...
         * 1. docker exec -it infinispan-node-1 bash
         * 2. /opt/infinispan/bin/cli.sh
         * 3. connect localhost:11222 -u user -p password
         * 4. ls caches
         * 5. 4번을 수행했을때, com.example.springdb.study.jpabook.ch16_transaction_and_locks.models.Ch16Board  이런 캐시가 없다...
         * */
    }
}
