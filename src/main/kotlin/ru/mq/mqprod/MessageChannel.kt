package ru.mq.mqprod

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component

@Component
class MessageChannel {

    private var sendChannel = Channel<String>()

    fun getChannel(): Channel<String> {
        return sendChannel
    }

    fun sendMessage(message: String) = runBlocking {
        sendChannel.send(message)
    }
}

