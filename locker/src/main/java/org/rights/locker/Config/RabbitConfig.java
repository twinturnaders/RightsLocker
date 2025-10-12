package org.rights.locker.Config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String QUEUE = "rl.jobs";
    public static final String EXCHANGE = "rl";
    public static final String ROUTING = "jobs";


    @Bean
    Queue rlQueue() { return QueueBuilder.durable(QUEUE).build(); }
    @Bean
    DirectExchange rlExchange() { return new DirectExchange(EXCHANGE, true, false); }
    @Bean
    Binding rlBind() { return BindingBuilder.bind((Exchange) rlQueue()).to(rlExchange()).with(ROUTING); }
}