package com.example.springdb.study.jpabook.ch15_advanced_and_optimizing.batch

import com.example.springdb.study.jpabook.ch15_advanced_and_optimizing.models.Ch15Product
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.hibernate.CacheMode
import org.hibernate.ScrollMode
import org.hibernate.Session
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

@DataJpaTest
class Ch15BatchTest {
    @Autowired
    @PersistenceContext
    lateinit var em: EntityManager

    /**
     * 대량 데이터 처리 시, 영속성 컨텍스트에 많은 객체가 쌓이면 메모리 부족 등의 문제가 발생할 수 있다.
     * 따라서 일정 단위로 flush/clear 하여 메모리 사용을 최소화할 수 있다.
     *
     * 하지만 별도의 설정이 없다면, 엔티티 개수만큼 insert/update 쿼리가 각각 전송되어 네트워크 및 DB 부하가 크다.
     * 이를 해결하기 위해서는 아래와 같은 조건이 필요하다:
     *
     * 1. 사용하는 DB가 sequence를 지원할 것 (batch insert의 경우)
     * 2. 엔티티 ID 전략이 IDENTITY가 아닐 것 (예: SEQUENCE)
     * 3. application 설정:
     *      spring.jpa.properties.hibernate.jdbc.batch_size=100
     *      spring.jpa.properties.hibernate.order_inserts=true
     *      spring.jpa.properties.hibernate.order_updates=true
     *
     * 위 설정이 적용되면, 100개의 insert 쿼리가 JDBC batch로 묶여 한번에 DB로 전송된다.
     * 실제 SQL은 prepare된 하나의 insert 쿼리를 100번 addBatch한 후, executeBatch()로 전송된다.
     *
     */

    @Test
    fun batch_insert() {
        val tx = em.transaction
        tx.begin()

        for (i in 0..(100 * 2) - 1) {
            val product = Ch15Product("product_{$i}", i * 100)
            em.persist(product)

            if ((i + 1) % 100 == 0) {
                em.flush()
                em.clear()
            }
        }

        tx.commit()
        em.close()
    }

    @Test
    fun batch_update_paging() {
        val tx = em.transaction
        val perPage = 100
        val page = 10
        tx.begin()

        for (i in 0..page - 1) {
            val resultList = em
                .createQuery("SELECT p From Ch15Product p", Ch15Product::class.java)
                .setFirstResult(i * perPage)
                .setMaxResults(perPage)
                .resultList
        }

        tx.commit()
        em.close()
    }

    @Test
    fun batch_update_scroll() {
        // Hibernate는 scroll 이란 이름으로 JDBC 커서를 지원한다.
        val tx = em.transaction
        val session = em.unwrap(Session::class.java)
        tx.begin()

        val scroll = session
            .createQuery("SELECT p from Ch15Product p", Ch15Product::class.java)
            .setCacheMode(CacheMode.IGNORE) // 2차 캐시 기능을 끈다.
            .scroll(ScrollMode.FORWARD_ONLY)

        var count = 0
        while (scroll.next()) {
            val p = scroll.get() as Ch15Product
            p.price = p.price!! * 100

            count++
            if (count % 100 == 0) {
                session.flush()
                session.clear()
            }
        }
        tx.commit()
        em.close()
    }
}
