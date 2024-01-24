package com.cpe.simulator.cpe;


import com.cpe.simulator.message.ConcurrentManagement;
import com.cpe.simulator.util.InformConstants;
import com.cpe.simulator.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.dslforum.cwmp_1_0.Envelope;
import org.dslforum.cwmp_1_0.EventStruct;
import org.dslforum.cwmp_1_0.ParameterValueStruct;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class CpeActivateRunable implements Runnable{

    private String sn;

    private String rollBackOrActivate;

    public CpeActivateRunable(String sn, String method) {
        this.sn = sn;
        this.rollBackOrActivate = method;
    }

    @Override
    public void run() {
        try {
            TimeUnit.SECONDS.sleep(20);
            setMuActivateStage(sn, InformConstants.MU_ACTIVATE_STATUS_ACTIVATING);

            ArrayList<EventStruct> eventKeyList = new ArrayList<>();
            EventStruct eventStruct = new EventStruct();
            eventStruct.setEventCode(EventStructConstants.EVENT_VALUE_CHANGED);
            eventKeyList.add(eventStruct);
            CpeActionsService cpeActionsService = SpringUtil.getBean(CpeActionsService.class);
            List<ParameterValueStruct> arr = new ArrayList();
            ParameterValueStruct pvstruct = new ParameterValueStruct();
            pvstruct.setName("Device.DeviceInfo.MU.1.X_7C8334_SwUpgradeStage");
            if (rollBackOrActivate.equals(InformConstants.SOFTWARECTRL_ACTIVATE_METHOD)) {
                pvstruct.setValue(InformConstants.SOFTWARECTRL_ACTIVATE_IN_PROGRESS);
                log.info("device:" + sn + ".." + "send value change.." + InformConstants.SOFTWARECTRL_ACTIVATE_IN_PROGRESS);
            } else if(rollBackOrActivate.equals(InformConstants.SOFTWARECTRL_ROLLBACK_METHOD)){
                pvstruct.setValue(InformConstants.SOFTWARECTRL_ROLLBACK_IN_PROGRESS);
                log.info("device:" + sn + ".." + "send value change.." + InformConstants.SOFTWARECTRL_ROLLBACK_IN_PROGRESS);
            }
            arr.add(pvstruct);
            Envelope informMessage = cpeActionsService.doInform(eventKeyList, sn, arr);
            CPEClientSession cpeClientSession = SpringUtil.getBean(CPEClientSession.class);

            cpeClientSession.sendInform(informMessage, sn);

            TimeUnit.SECONDS.sleep(20);

            if (rollBackOrActivate.equals(InformConstants.SOFTWARECTRL_ACTIVATE_METHOD)) {
                pvstruct.setValue(InformConstants.SOFTWARECTRL_ACTIVATE_COMPLETE);
                log.info("device:" + sn + ".." + "send value change.." + InformConstants.SOFTWARECTRL_ACTIVATE_COMPLETE);
            } else if(rollBackOrActivate.equals(InformConstants.SOFTWARECTRL_ROLLBACK_METHOD)){
                pvstruct.setValue(InformConstants.SOFTWARECTRL_ROOLBACK_COMPLETE);
                log.info("device:" + sn + ".." + "send value change.." + InformConstants.SOFTWARECTRL_ROOLBACK_COMPLETE);
            }

            informMessage = cpeActionsService.doInform(eventKeyList, sn, arr);
            cpeClientSession.sendInform(informMessage, sn);
            AtomicBoolean sendFlag = ConcurrentManagement.HEART_BEAT_SEND_FLAG.getOrDefault(sn, new AtomicBoolean(true));
            sendFlag.set(false);
            ConcurrentManagement.HEART_BEAT_SEND_FLAG.put(sn, sendFlag);

            convertCurrentVersionAndBackUpVersion(sn);
            List<String> eventCodeList = new ArrayList<>();
            eventCodeList.add(EventStructConstants.EVENT_BOOT);
            eventCodeList.add(EventStructConstants.EVENT_BOOT_STRAP);
            setMuActivateStage(sn, InformConstants.MU_ACTIVATE_STATUS_WAITE_ACTIVATE);
            ConcurrentManagement.BOOT_EVENT_CODE.put(sn, eventCodeList);

            TimeUnit.SECONDS.sleep(120);
        } catch (Exception e) {
            log.error("active task...", e);
        } finally {
            updateMuDownloadStatus();
            AtomicBoolean sendFlag = ConcurrentManagement.HEART_BEAT_SEND_FLAG.getOrDefault(sn, new AtomicBoolean(true));
            sendFlag.set(true);
            ConcurrentManagement.HEART_BEAT_SEND_FLAG.put(sn, sendFlag);
        }
    }

    private void setMuActivateStage(String sn, String activateStatus) {
        CpeDBReader cpeDBReader = SpringUtil.getBean(CpeDBReader.class);
        cpeDBReader.setValue(sn, InformConstants.MU_ACTIVATE_STATUS, activateStatus);
    }

    private void convertCurrentVersionAndBackUpVersion(String sn) {
        CpeDBReader cpeDBReader = SpringUtil.getBean(CpeDBReader.class);
        String currentVersion = cpeDBReader.getValue(sn, InformConstants.SOFTWARE_CURRENT_VERSION);
        String backUpVersion = cpeDBReader.getValue(sn, InformConstants.SOFTWARE_BACKUP_VERSION);
        cpeDBReader.setValue(sn, InformConstants.SOFTWARE_CURRENT_VERSION, backUpVersion);
        cpeDBReader.setValue(sn, InformConstants.SOFTWARE_BACKUP_VERSION, currentVersion);
        cpeDBReader.setValue(sn, InformConstants.MU_SOFTWARE_VERSION, currentVersion);
    }

    private void updateMuDownloadStatus() {
        CpeDBReader cpeDBReader = SpringUtil.getBean(CpeDBReader.class);
        cpeDBReader.setValue(sn, InformConstants.MU_DOWNLOAD_STATUS, InformConstants.MU_DOWNLOAD_STATUS_CLEAR);
    }
}
