package com.cpe.simulator.executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class ExecutorManagement {

    @Resource
    private List<String> registerEnbSn;

    @Bean(name = "hearBeatMsgPoolManagement")
    public ThreadPoolTaskExecutor initThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("heatBeat-thread");
        int coreSize = Double.valueOf(Math.ceil(registerEnbSn.size() * 1.2)).intValue();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(coreSize);
        executor.setQueueCapacity(1021);
        executor.setKeepAliveSeconds(5 * 60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        return executor;
    }

    @Bean(name = "processMsgPoolManagement")
    public ThreadPoolTaskExecutor initPrecessMessagePool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("processMsg-thread");
        int coreSize = Double.valueOf(Math.ceil(registerEnbSn.size() * 1.2)).intValue();
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(coreSize);
        executor.setQueueCapacity(1021);
        executor.setKeepAliveSeconds(5 * 60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        return executor;
    }

    @Bean(name = "downloadPoolManagement")
    public ThreadPoolTaskExecutor initDownloadMessagePool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("downLoad-thread");
        executor.setCorePoolSize(registerEnbSn.size());
        executor.setMaxPoolSize(registerEnbSn.size() * 2);
        executor.setQueueCapacity(1021);
        executor.setKeepAliveSeconds(5 * 60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        return executor;
    }

    @Bean(name = "kpiFileBuildPoolManagement")
    public ThreadPoolTaskExecutor initKpiFileBuildPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("kpiFile-thread");
        executor.setCorePoolSize(registerEnbSn.size());
        executor.setMaxPoolSize(registerEnbSn.size());
        executor.setQueueCapacity(1021);
        executor.setKeepAliveSeconds(5 * 60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        return executor;
    }

    @Bean(name = "uploadFilePoolManagement")
    public ThreadPoolTaskExecutor initUploadFilePool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("upLoadFile-thread");
        int coreNumber = registerEnbSn.size() / 2 + 1;
        executor.setCorePoolSize(coreNumber);
        executor.setMaxPoolSize(coreNumber);
        executor.setQueueCapacity(1021);
        executor.setKeepAliveSeconds(5 * 60);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

}
