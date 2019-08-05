package ru.mq.mqprod

import org.springframework.beans.factory.annotation.Value
import org.springframework.jms.core.JmsTemplate
import org.springframework.stereotype.Component


@Component
class Sender(private val jmsTemplate: JmsTemplate) {

    @Value("\${ibm.mq.queue}")
    private lateinit var queue: String

    fun sendMessage(msg: String) {
        jmsTemplate.convertAndSend(queue, msg)
    }
}