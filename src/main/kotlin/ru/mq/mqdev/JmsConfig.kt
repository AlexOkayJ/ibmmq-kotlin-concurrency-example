package ru.mq.mqdev

import com.ibm.mq.jms.MQQueueConnectionFactory
import com.ibm.msg.client.wmq.WMQConstants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.core.JmsTemplate
import org.springframework.jms.listener.SimpleMessageListenerContainer
import org.springframework.jms.support.QosSettings
import javax.jms.DeliveryMode
import javax.jms.Message

@Configuration
@EnableJms
@Profile("DEV")
class JmsConfig {

    @Autowired
    lateinit var listener: Listener

    @Autowired
    lateinit var env: Environment

    @Value("\${ibm.mq.host}")
    private lateinit var host: String
    @Value("\${ibm.mq.port}")
    private lateinit var port: String
    @Value("\${ibm.mq.queue-manager}")
    private lateinit var queueManager: String
    @Value("\${ibm.mq.channel}")
    private lateinit var channel: String
    @Value("\${ibm.mq.inQueue}")
    private lateinit var queue: String
    @Value("\${ibm.mq.timeout}")
    private lateinit var timeout: String
    @Value("\${ibm.mq.local}")
    private lateinit var localAdress: String
    @Value("\${ibm.mq.cipher}")
    private lateinit var cipher: String

    @Bean
    fun mqQueueConnectionFactory(): MQQueueConnectionFactory {
        systemSSLProperty()
        val mqQueueConnectionFactory = MQQueueConnectionFactory()
        try {
            mqQueueConnectionFactory.hostName = host
            mqQueueConnectionFactory.localAddress = localAdress
            mqQueueConnectionFactory.sslCipherSuite = cipher
            mqQueueConnectionFactory.queueManager = queueManager
            mqQueueConnectionFactory.port = port.toInt()
            mqQueueConnectionFactory.channel = channel
            mqQueueConnectionFactory.transportType = WMQConstants.WMQ_CM_CLIENT
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return mqQueueConnectionFactory
    }

    @Bean
    @Primary
    fun queueTemplate(mqQueueConnectionFactory: MQQueueConnectionFactory): JmsTemplate {
        val jmsTemplate = JmsTemplate(mqQueueConnectionFactory)
        jmsTemplate.setQosSettings(
            QosSettings(
                DeliveryMode.NON_PERSISTENT,
                Message.DEFAULT_PRIORITY,
                Message.DEFAULT_TIME_TO_LIVE
            )
        )
        jmsTemplate.receiveTimeout = timeout.toLong()
        return jmsTemplate
    }

    @Bean
    fun queueContainer(mqQueueConnectionFactory: MQQueueConnectionFactory): SimpleMessageListenerContainer {
        val container = SimpleMessageListenerContainer()
        container.connectionFactory = mqQueueConnectionFactory
        container.destinationName = queue
        container.messageListener = listener
        container.start()
        return container
    }


    fun systemSSLProperty() {
        System.setProperty("com.ibm.msg.client.commonservices.log.status", env.getProperty("server.ssl.log-status"))
        java.security.Security.setProperty("jdk.tls.disabledAlgorithms", "")
        System.setProperty("com.ibm.jsse2.disableSSLv3", env.getProperty("server.ssl.disableSSLv3"))
        System.setProperty("com.ibm.disableSSLv3", env.getProperty("server.ssl.disableSSLv3"))
        System.setProperty("https.protocols", env.getProperty("server.ssl.https-protocols"))
        System.setProperty("javax.net.ssl.trustStore", env.getProperty("server.ssl.trust-store"))
        System.setProperty("javax.net.ssl.trustStorePassword", env.getProperty("server.ssl.trust-store-password"))
        System.setProperty("javax.net.ssl.keyStore", env.getProperty("server.ssl.key-store"))
        System.setProperty("javax.net.ssl.keyStorePassword", env.getProperty("server.ssl.key-store-password"))
    }
}
