package com.cpe.simulator.cpe;

import com.cpe.simulator.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.dslforum.cwmp_1_0.Envelope;
import org.dslforum.cwmp_1_0.EventStruct;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Slf4j
public class ConnectionRequestRunable implements Runnable{

    private String sn;

    public ConnectionRequestRunable(String sn) {
        this.sn = sn;
    }

    @Override
    public void run() {
        log.info(LocalDateTime.now() + ":" + "send connection request...." + sn);
        ArrayList<EventStruct> eventKeyList = new ArrayList<>();
        EventStruct eventStruct = new EventStruct();
        eventStruct.setEventCode(EventStructConstants.EVENT_CONNECTION_REQUEST);
        eventKeyList.add(eventStruct);
        CpeActionsService cpeActionsService = SpringUtil.getBean(CpeActionsService.class);
        Envelope informMessage = cpeActionsService.doInform(eventKeyList, sn);
        CPEClientSession cpeClientSession = SpringUtil.getBean(CPEClientSession.class);
        cpeClientSession.sendInform(informMessage, sn);
    }
}
