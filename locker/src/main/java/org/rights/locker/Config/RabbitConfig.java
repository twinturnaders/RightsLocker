package org.rights.locker.Config;



import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "rl.exchange";
    public static final String QUEUE_JOBS = "rl.jobs";
    public static final String ROUTING_JOBS = "jobs.process";

    @Bean
    public Exchange rlExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }
    @Bean
    public Queue rlJobsQueue() {
        return QueueBuilder.durable(QUEUE_JOBS).build();
    }
    @Bean
    public Binding rlJobsBinding() {
        return BindingBuilder.bind(rlJobsQueue()).to(rlExchange()).with(ROUTING_JOBS).noargs();
    }
}