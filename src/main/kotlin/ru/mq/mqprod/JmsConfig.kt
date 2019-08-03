package ru.mq.mqprod

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.jms.config.JmsListenerContainerFactory
import org.springframework.jms.core.JmsTemplate
import org.springframework.jms.support.QosSettings
import org.springframework.jndi.JndiTemplate
import org.springframework.util.backoff.FixedBackOff
import ru.mq.logger
import javax.jms.ConnectionFactory
import javax.jms.DeliveryMode
import javax.jms.Message

@Configuration
@EnableJms
@Profile("PROD")
class JmsConfig {

    private val log by logger()

    @Value("\${spring.jms.jndi-name}")
    private lateinit var manager: String

    @Bean
    @Primary
    fun buildJmsTemplate(): JmsTemplate {
        log.info("{}", "Try to create JmsTemplate")
        val jmsTemplate = JmsTemplate(getJNDIConnectionFactory(manager))
        jmsTemplate.setQosSettings(
            QosSettings(
                DeliveryMode.NON_PERSISTENT,
                Message.DEFAULT_PRIORITY,
                Message.DEFAULT_TIME_TO_LIVE
            )
        )
        log.info("{}", "JmsTemplate create successful")
        return jmsTemplate
    }

    @Bean
    fun listenerFactory(configurer: DefaultJmsListenerContainerFactoryConfigurer): JmsListenerContainerFactory<*> {
        log.info("{}", "Try to create listenerFactory")
        val connectionFactory = getJNDIConnectionFactory(manager)
        val factory = DefaultJmsListenerContainerFactory()
        factory.setBackOff(FixedBackOff())
        factory.setRecoveryInterval(5000)
        log.info("{}", "factory backOff is: $factory")
        configurer.configure(factory, connectionFactory)
        log.info("{}", "listenerFactory create successful")
        return factory
    }

    private fun getJNDIConnectionFactory(jndiName: String): ConnectionFactory {
        val jndiTemplate = JndiTemplate()
        return jndiTemplate.lookup(jndiName) as ConnectionFactory
    }
}