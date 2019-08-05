package ru.mq.service

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.selects.select
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import ru.mq.logger
import ru.mq.mqprod.MessageChannel
import ru.mq.mqprod.Sender


/**
 *  I pretty recommend watch Roman Elizarov talk about this pattern from KotlinConf 2018 @link https://www.youtube.com/watch?v=a3agLJQ6vt8
 *  And also you can read this article @link https://medium.com/@elizarov/deadlocks-in-non-hierarchical-csp-e5910d137cc for better understanding how it work
 */
@Service
class MessageWorker(
    private val sender: Sender,
    private val messageService: MessageService,
    private val messageChannel: MessageChannel
) {
    private val log by logger()

    @Value("\${ibm.mq.workers.number}")
    private lateinit var workersNumber: String //number of coroutines

    @Value("\${ibm.mq.thread.number}")
    private lateinit var threadsNumber: String //number of threads

    @ObsoleteCoroutinesApi
    private val messageThreadPool by lazy {
        newFixedThreadPoolContext(threadsNumber.toInt(), "messageWorker-pool")
    }

    @ObsoleteCoroutinesApi
    @Bean
    fun buildMessageWorker() = GlobalScope.launch(messageThreadPool) {
        messageProcess(messageChannel.getChannel())
    }

    private fun CoroutineScope.downloader(
        inMessages: ReceiveChannel<String>,
        workChannel: SendChannel<String>,
        resultChannel: ReceiveChannel<String>
    ) = launch {
        while (true) {
            select<Unit> {
                resultChannel.onReceive { resultMessage ->
                    sendResult(resultMessage)
                }
                inMessages.onReceive { inMessage ->
                    workChannel.send(inMessage)
                }
            }
        }
    }

    private fun CoroutineScope.worker(
        workChannel: ReceiveChannel<String>,
        resultChannel: SendChannel<String>
    ) = launch {
        for (message in workChannel) {
            val result = uploadContent(message)
            resultChannel.send(result)
        }
    }

    /**
     * Do some work asynchronously
     */
    private suspend fun uploadContent(message: String): String {
        return messageService.doWork(message)
    }

    /**
     * Send answer
     */
    private fun sendResult(resultMessage: String) {
        runCatching {
            //In this example, we didn't have que for answer. If we start send message
            //to sender out listener start read it cause its the same que
//            sender.sendMessage(resultMessage)
        }.getOrElse { thr ->
            log.error("{}", "$thr")
        }
    }

    private fun CoroutineScope.messageProcess(inMessages: ReceiveChannel<String>) {
        val workers = workersNumber.toInt()
        log.info("{}", "Create processing for messaging, workers count is: $workersNumber")
        val workChannel = Channel<String>()

        val resultChannel = Channel<String>(1) // don't touch it or you can catch a deadlock

        repeat(workers) { worker(workChannel, resultChannel) }
        downloader(inMessages, workChannel, resultChannel)
        log.info("{}", "Processing create successful")
    }

}



