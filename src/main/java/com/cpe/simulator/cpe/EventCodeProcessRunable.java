package com.cpe.simulator.cpe;

import com.cpe.simulator.bean.EventCodeProcessInfo;
import com.cpe.simulator.util.InformConstants;
import com.cpe.simulator.util.SpringUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dslforum.cwmp_1_0.Envelope;
import org.dslforum.cwmp_1_0.EventStruct;
import org.dslforum.cwmp_1_0.ParameterValueStruct;
import org.springframework.web.servlet.view.BeanNameViewResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class EventCodeProcessRunable implements Runnable{

    private EventCodeProcessInfo eventCodeProcessInfo;

    public EventCodeProcessRunable(EventCodeProcessInfo eventCodeProcessInfo) {
        this.eventCodeProcessInfo = eventCodeProcessInfo;
    }

    public EventCodeProcessRunable() {}


    @Override
    public void run() {
        try {
            TimeUnit.SECONDS.sleep(30);

            ArrayList<EventStruct> eventKeyList = new ArrayList<>();
            EventStruct eventStruct = new EventStruct();
            eventStruct.setEventCode(eventCodeProcessInfo.getEventCode());
            eventKeyList.add(eventStruct);
            CpeActionsService cpeActionsService = SpringUtil.getBean(CpeActionsService.class);
            List<ParameterValueStruct> arr = new ArrayList();
            for (String itemParaName : eventCodeProcessInfo.getParameterPathToValue().keySet()) {
                ParameterValueStruct pvstruct = new ParameterValueStruct();
                pvstruct.setName(itemParaName);
                pvstruct.setValue(eventCodeProcessInfo.getParameterPathToValue().get(itemParaName).toString());

                arr.add(pvstruct);
            }

            Envelope informMessage = cpeActionsService.doInform(eventKeyList, eventCodeProcessInfo.getSn(), arr);
            CPEClientSession cpeClientSession = SpringUtil.getBean(CPEClientSession.class);
            cpeClientSession.sendInform(informMessage, eventCodeProcessInfo.getSn());

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            CpeDBReader cpeDBReader = SpringUtil.getBean(CpeDBReader.class);
            for (String itemParaName : eventCodeProcessInfo.getParameterPathToValue().keySet()) {
                cpeDBReader.setValue(eventCodeProcessInfo.getSn(), itemParaName, eventCodeProcessInfo.getParameterPathToValue().get(itemParaName).toString());
            }
        }
    }
}
