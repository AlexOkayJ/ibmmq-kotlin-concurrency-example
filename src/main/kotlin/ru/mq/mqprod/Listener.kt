package ru.mq.mqprod

import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Profile
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Component
@Profile("PROD")
class Listener(private val messageChannel: MessageChannel) {

    @JmsListener(destination = "\${ibm.mq.queue}", containerFactory = "listenerFactory")
    fun processMessage(message: String) = runBlocking {
         messageChannel.sendMessage(message)
    }
}