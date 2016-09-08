package alien4cloud.configuration;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableAsync
@EnableScheduling
public class SchedulingConfiguration {

    @Value("${paas_monitor.threadpool_size}")
    private int paasMonitorThreadPoolSize;

    @Bean(name = "paas-monitor-scheduler")
    public Executor getPaaSScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(paasMonitorThreadPoolSize);
        threadPoolTaskScheduler.setThreadNamePrefix("paas-monitor-");
        return threadPoolTaskScheduler;
    }

    @Bean(name = "node-type-score-scheduler")
    public Executor getNodeTypeScoreScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(1);
        threadPoolTaskScheduler.setThreadNamePrefix("node-type-score-");
        return threadPoolTaskScheduler;
    }

}