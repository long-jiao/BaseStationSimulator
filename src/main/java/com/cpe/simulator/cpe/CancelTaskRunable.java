package com.cpe.simulator.cpe;


import com.cpe.simulator.message.ConcurrentManagement;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Future;

@Data
@Slf4j
public class CancelTaskRunable implements Runnable{

    private Future<?> future;

    private String sn;

    @Override
    public void run() {
        try {
            ConcurrentManagement.SN_MR_TASK_FUTURE.remove(sn);
            log.error("cancel task....");
            future.cancel(false);
        } catch (Exception e) {
            log.error("cancel task error: {}", e.getMessage());
        }

    }
}
