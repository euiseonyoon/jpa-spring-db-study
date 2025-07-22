package com.example.springdb.study

import com.example.springdb.study.springdata.common.repositories.customgeneral.MyRepositoryImpl
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
// NOTE: 이거 중요!!
@EnableJpaRepositories(repositoryBaseClass = MyRepositoryImpl::class)
// @EnableCaching. @Cacheable 어노테이션 사용한다면 활성화
class SpringDbStudyApplication

fun main(args: Array<String>) {
    runApplication<SpringDbStudyApplication>(*args)
}
