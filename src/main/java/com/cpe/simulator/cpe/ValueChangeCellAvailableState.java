package com.cpe.simulator.cpe;

import com.cpe.simulator.util.SpringUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dslforum.cwmp_1_0.Envelope;
import org.dslforum.cwmp_1_0.EventStruct;
import org.dslforum.cwmp_1_0.ParameterValueStruct;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class ValueChangeCellAvailableState implements Runnable{

    private String sn;

    private int state;


    public ValueChangeCellAvailableState(String sn, int state) {
        this.sn = sn;
        this.state = state;
    }


    @Override
    public void run() {
        try {
            TimeUnit.SECONDS.sleep(2);
            ArrayList<EventStruct> eventKeyList = new ArrayList<>();
            EventStruct eventStruct = new EventStruct();
            eventStruct.setEventCode(EventStructConstants.EVENT_VALUE_CHANGED);
            eventKeyList.add(eventStruct);
            CpeActionsService cpeActionsService = SpringUtil.getBean(CpeActionsService.class);
            List<ParameterValueStruct> arr = new ArrayList();
            ParameterValueStruct pvstruct = new ParameterValueStruct();
            pvstruct.setName("Device.Services.X_7C8334_CUFAPService.1.CellConfig.NR.RAN.RF.CellAvailableState");
            pvstruct.setValue(String.valueOf(state));

            arr.add(pvstruct);
            Envelope informMessage = cpeActionsService.doInform(eventKeyList, sn, arr);
            CPEClientSession cpeClientSession = SpringUtil.getBean(CPEClientSession.class);

            cpeClientSession.sendInform(informMessage, sn);
            log.info("ValueChangeCellAvailableState send complete.....");
        } catch (Exception e) {
            log.error("ValueChangeCellAvailableState...", e);
        }
     }
}
