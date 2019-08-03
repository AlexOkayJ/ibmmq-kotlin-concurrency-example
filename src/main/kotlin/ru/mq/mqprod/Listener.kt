package ru.mq.mqprod

import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Profile
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component


@Component
@Profile("PROD")
class Listener(private val messageChannel: MessageChannel) {

    @JmsListener(destination = "\${ibm.mq.inQueue}", containerFactory = "listenerFactory")
    fun processMessage(message: String) = runBlocking {
        messageChannel.sendMessage(message)
    }
}