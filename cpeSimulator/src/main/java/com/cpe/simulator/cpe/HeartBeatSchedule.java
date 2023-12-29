package com.cpe.simulator.cpe;

import com.cpe.simulator.message.ConcurrentManagement;
import com.cpe.simulator.util.CommonUtil;
import com.cpe.simulator.util.InformConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.record.DVALRecord;
import org.dslforum.cwmp_1_0.Envelope;
import org.dslforum.cwmp_1_0.EventStruct;
import org.dslforum.cwmp_1_0.ParameterValueStruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class HeartBeatSchedule {
    @Resource
    private ThreadPoolTaskExecutor hearBeatMsgPoolManagement;

    @Resource
    private List<String> registerEnbSn;

    @Resource
    private CpeActionsService cpeActionsService;

    @Resource
    private CPEClientSession cpeClientSession;

    @Resource
    private CpeDBReader cpeDBReader;

    @Value("${reportAlarm:false}")
    private boolean reportAlarm;

    private int hearBeatNum = 0;

    private AtomicBoolean firstInform = new AtomicBoolean(true);

    private boolean triggerAlarm = false;

    private boolean processAlarm = false;

    private String triggerAlarmIdentifier;

    private Random rs = new Random();

    private int rsBoundNum = InformConstants.ALARM_IDENTIFIER_LIST.size();

    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    public void sendHeartBeatInform() {
        boolean firstHearBeat = firstInform.getAndSet(false);
        hearBeatNum++;
        if (hearBeatNum % 20 == 0) {
            triggerAlarm = false;
            processAlarm = true;
        } else if (hearBeatNum % 10 == 0) {
            triggerAlarm = true;
            processAlarm = true;
        } else {
            processAlarm = false;
        }

        if (processAlarm && reportAlarm) {
            if (triggerAlarm) {
                triggerAlarmIdentifier = InformConstants.ALARM_IDENTIFIER_LIST.get(rs.nextInt(rsBoundNum));
            }
        }

        for (String itemSn : registerEnbSn) {
            hearBeatMsgPoolManagement.submit(() -> {
                AtomicBoolean sendFlag = ConcurrentManagement.HEART_BEAT_SEND_FLAG.getOrDefault(itemSn, new AtomicBoolean(true));
                if (!sendFlag.get()) {
                    return;
                }

                ArrayList<EventStruct> eventKeyList = new ArrayList<>();
                List<ParameterValueStruct> parameterValueStructList = new ArrayList<>();
                List<String> bootEventCodeList = ConcurrentManagement.BOOT_EVENT_CODE.remove(itemSn);
                if (firstHearBeat) {
                    EventStruct bootEvent = new EventStruct();
                    bootEvent.setEventCode(EventStructConstants.EVENT_BOOT);
                    eventKeyList.add(bootEvent);
                    EventStruct bootStrapEvent = new EventStruct();
                    bootStrapEvent.setEventCode(EventStructConstants.EVENT_BOOT_STRAP);
                    eventKeyList.add(bootStrapEvent);
                } else if (CollectionUtils.isEmpty(bootEventCodeList)) {
                    EventStruct eventStruct = new EventStruct();
                    eventStruct.setEventCode(EventStructConstants.EVENT_PERIODIC);
                    eventKeyList.add(eventStruct);
                    if (processAlarm && reportAlarm) {
                        EventStruct valueChangeEvent = new EventStruct();
                        valueChangeEvent.setEventCode(EventStructConstants.EVENT_VALUE_CHANGED);
                        eventKeyList.add(valueChangeEvent);
                        parameterValueStructList.addAll(buildAlarmParameterStruct(itemSn));
                    }
                } else {
                    for (String itemEventCode : bootEventCodeList) {
                        EventStruct eventStruct = new EventStruct();
                        eventStruct.setEventCode(itemEventCode);
                        eventKeyList.add(eventStruct);
                    }
                }

                Envelope informMessage = cpeActionsService.doInform(eventKeyList, itemSn, parameterValueStructList);
//            System.out.println("Sending Periodic Message at " + LocalDateTime.now());
                cpeClientSession.sendInform(informMessage, itemSn);
            });
        }

        if (hearBeatNum >= 10000000) {
            hearBeatNum = 0;
        }
    }

    private List<ParameterValueStruct> buildAlarmParameterStruct(String sn) {
        List<ParameterValueStruct> parameterValueStructList = new ArrayList<>();
        ParameterValueStruct eventTimeValueStruct = new ParameterValueStruct();
        eventTimeValueStruct.setName("Device.FaultMgmt.ExpeditedEvent.3.EventTime");
        eventTimeValueStruct.setValue(CommonUtil.formatTriggerEventTime(LocalDateTime.now()));
        parameterValueStructList.add(eventTimeValueStruct);

        ParameterValueStruct alarmIdentifierStruct = new ParameterValueStruct();
        alarmIdentifierStruct.setName("Device.FaultMgmt.ExpeditedEvent.3.AlarmIdentifier");
        alarmIdentifierStruct.setValue(triggerAlarmIdentifier);
        parameterValueStructList.add(alarmIdentifierStruct);

        ParameterValueStruct ouiValueStruct = new ParameterValueStruct();
        ouiValueStruct.setName("Device.FaultMgmt.ExpeditedEvent.3.OUI");
        ouiValueStruct.setValue(InformConstants.DEVICE_OUI);
        parameterValueStructList.add(ouiValueStruct);

        ParameterValueStruct snValueStruct = new ParameterValueStruct();
        snValueStruct.setName("Device.FaultMgmt.ExpeditedEvent.3.SerialNumber");
        snValueStruct.setValue(sn);
        parameterValueStructList.add(snValueStruct);

        ParameterValueStruct notifyTypeValueStruct = new ParameterValueStruct();
        notifyTypeValueStruct.setName("Device.FaultMgmt.ExpeditedEvent.3.NotificationType");
        if (triggerAlarm) {
            notifyTypeValueStruct.setValue(InformConstants.ALARM_NOTIFY_TYPE_CREATE);
        } else {
            notifyTypeValueStruct.setValue(InformConstants.ALARM_NOTIFY_TYPE_CLEAR);
        }
        parameterValueStructList.add(notifyTypeValueStruct);

        ParameterValueStruct managedObjectInstanceStruct = new ParameterValueStruct();
        managedObjectInstanceStruct.setName("Device.FaultMgmt.ExpeditedEvent.3.ManagedObjectInstance");
        String gnbId = cpeDBReader.getValue(sn, InformConstants.GNBID_PATH);
        managedObjectInstanceStruct.setValue("GNBID=" + gnbId);
        parameterValueStructList.add(managedObjectInstanceStruct);

        ParameterValueStruct eventTypeStruct = new ParameterValueStruct();
        eventTypeStruct.setName("Device.FaultMgmt.ExpeditedEvent.3.EventType");
        eventTypeStruct.setValue("2");
        parameterValueStructList.add(eventTypeStruct);

        ParameterValueStruct probableCausetruct = new ParameterValueStruct();
        probableCausetruct.setName("Device.FaultMgmt.ExpeditedEvent.3.ProbableCause");
        probableCausetruct.setValue("");
        parameterValueStructList.add(probableCausetruct);

        ParameterValueStruct specificProblemStruct = new ParameterValueStruct();
        specificProblemStruct.setName("Device.FaultMgmt.ExpeditedEvent.3.SpecificProblem");
        specificProblemStruct.setValue("mock trigger alarm");
        parameterValueStructList.add(specificProblemStruct);

        ParameterValueStruct perceivedSeverityStruct = new ParameterValueStruct();
        perceivedSeverityStruct.setName("Device.FaultMgmt.ExpeditedEvent.3.PerceivedSeverity");
        perceivedSeverityStruct.setValue("2");
        parameterValueStructList.add(perceivedSeverityStruct);

        ParameterValueStruct additionalTextStruct = new ParameterValueStruct();
        additionalTextStruct.setName("Device.FaultMgmt.ExpeditedEvent.3.AdditionalText");
        additionalTextStruct.setValue("mock trigger alarm");
        parameterValueStructList.add(additionalTextStruct);

        ParameterValueStruct additionalInfoStruct = new ParameterValueStruct();
        additionalInfoStruct.setName("Device.FaultMgmt.ExpeditedEvent.3.AdditionalInformation");
        additionalInfoStruct.setValue("mock trigger alarm");
        parameterValueStructList.add(additionalInfoStruct);

        return parameterValueStructList;
    }
}
