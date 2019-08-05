package ru.mq.mqprod

import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.jms.config.JmsListenerContainerFactory
import org.springframework.util.backoff.FixedBackOff
import ru.mq.logger
import javax.jms.ConnectionFactory

@Configuration
@EnableJms
@Profile("PROD")
class JmsConfig {

    private val log by logger()

    @Bean
    fun listenerFactory(configurer: DefaultJmsListenerContainerFactoryConfigurer, connectionFactory: ConnectionFactory): JmsListenerContainerFactory<*> {
        log.info("{}", "Try to create listenerFactory")
        val factory = DefaultJmsListenerContainerFactory()
        factory.setBackOff(FixedBackOff())
        factory.setRecoveryInterval(5000)
        log.info("{}", "factory backOff is: $factory")
        configurer.configure(factory, connectionFactory)
        log.info("{}", "listenerFactory create successful")
        return factory
    }

}