package ru.mq.service

interface MessageService {
    suspend fun doWork(message: String): String
}