package com.cpe.simulator.message;


import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentManagement {

    public static Map<String, List<String>> BOOT_EVENT_CODE = Maps.newConcurrentMap();

    public static AtomicBoolean GENERATE_ALARM = new AtomicBoolean(false);

    public static Map<String, ReentrantLock> BASE_STATION_SEND_DATA_LOCK = Maps.newConcurrentMap();

    public static Map<String, AtomicBoolean> HEART_BEAT_SEND_FLAG = Maps.newConcurrentMap();

    public static Map<String, Future> SN_MR_TASK_FUTURE = Maps.newConcurrentMap();

    public static Map<String, Future> SN_NI_DATA_TASK_FUTURE = Maps.newConcurrentMap();

}
