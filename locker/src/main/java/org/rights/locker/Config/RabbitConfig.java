package org.rights.locker.Config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String JOBS_QUEUE = "rl.jobs";

    @Bean
    public Queue rlQueue() {
        // durable queue survives broker restart
        return new Queue(JOBS_QUEUE, true);
    }
}