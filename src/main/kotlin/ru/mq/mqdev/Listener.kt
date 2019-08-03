package ru.mq.mqdev

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import ru.mq.logger
import ru.mq.mqprod.MessageChannel
import javax.jms.Message
import javax.jms.MessageListener
import javax.jms.TextMessage

@Component
@Profile("DEV")
class Listener(private val messageChannel: MessageChannel) : MessageListener {

    private val log by logger()

    @ObsoleteCoroutinesApi
    private val messageWorker = newFixedThreadPoolContext(4, "messageWorker")


    @ExperimentalCoroutinesApi
    @ObsoleteCoroutinesApi
    override fun onMessage(message: Message) = runBlocking(messageWorker) {
        when (message) {
            is TextMessage -> messageChannel.sendMessage(message.text).also {
                log.debug("{}", "Input message:\r\n${message.text}")
            }
            else -> log.info("{}", "Message type is unknown")
        }
    }

}
