package py.edu.ucom.is2.proyectocamel.config;

import java.time.Clock;
import java.time.ZoneId;

import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.camel.spi.IdempotentRepository;
import org.apache.camel.support.processor.idempotent.MemoryIdempotentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.jms.ConnectionFactory;

@Configuration
public class IntegracionConfig {

    private static final Logger LOG = LoggerFactory.getLogger(IntegracionConfig.class);

    @Bean
    ConnectionFactory connectionFactory(@Value("${sipap.artemis.broker-url}") String brokerUrl) {
        LOG.info("Configurando conexión Artemis con broker URL: {}", brokerUrl);
        return new ActiveMQConnectionFactory(brokerUrl);
    }

    @Bean
    Clock sipapClock(@Value("${sipap.zona-horaria}") String zonaHoraria) {
        return Clock.system(ZoneId.of(zonaHoraria));
    }

    @Bean("transferenciasIdempotentRepository")
    IdempotentRepository transferenciasIdempotentRepository() {
        return MemoryIdempotentRepository.memoryIdempotentRepository(1000);
    }
}
