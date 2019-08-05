package ru.mq

import kotlinx.coroutines.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import ru.mq.mqprod.Sender

@Component
class SendScheduler(val sender: Sender) {

    /**
     * Just send every second message to MQ
     */
    @Scheduled(fixedRate = 2000)
    fun task() = runBlocking {
        sender.sendMessage("Hello world bro")
    }
}