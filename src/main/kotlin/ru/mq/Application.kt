package ru.mq

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.jms.annotation.EnableJms

@EnableJms
@SpringBootApplication(exclude = [MessageSourceAutoConfiguration::class])
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}





