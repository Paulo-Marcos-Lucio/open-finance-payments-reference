package dev.pmlsp.openfinance.payments.infrastructure.simulator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class SimulatorSchedulerConfig {

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService simulatorScheduler() {
        return Executors.newScheduledThreadPool(2);
    }
}
