package com.cpe.simulator.cpe;

import com.cpe.simulator.message.ConcurrentManagement;
import com.cpe.simulator.util.CommonConstans;
import com.cpe.simulator.util.InformConstants;
import com.cpe.simulator.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.dslforum.cwmp_1_0.Envelope;
import org.dslforum.cwmp_1_0.EventStruct;
import org.dslforum.cwmp_1_0.FaultStruct;
import org.dslforum.cwmp_1_0.TransferComplete;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class UploadFileService {

    @Resource
    private RestTemplate restTemplate;

    public int uploadFile(String uploadUrl, String filePath) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            org.springframework.core.io.Resource resource1 = null;
            try {
                resource1 = new ByteArrayResource(Files.readAllBytes(Paths.get(filePath, new String[0])));
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
            HttpEntity<byte[]> request = (HttpEntity<byte[]>) new HttpEntity(resource1, headers);

            ResponseEntity<Object> response = restTemplate.exchange(uploadUrl, HttpMethod.PUT, request, Object.class);
            return response.getStatusCode().value();
        } catch (Exception e) {
            log.error("UploadFileService url:" + uploadUrl);
            log.error(e.getMessage(), e);
        }
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    public void sendUploadInform(String commandKey, String sn, boolean uploadResult) {
        ArrayList<EventStruct> eventKeyList = new ArrayList<>();
        EventStruct eventStruct = new EventStruct();
        eventStruct.setEventCode(EventStructConstants.EVENT_TRANSFER_COMPLETE);
        eventKeyList.add(eventStruct);
        EventStruct mUpLoadEvent = new EventStruct();
        mUpLoadEvent.setEventCode(EventStructConstants.EVENT_M_UPLOAD_TRANSFER_COMPLETE);
        mUpLoadEvent.setCommandKey(commandKey);
        eventKeyList.add(mUpLoadEvent);

        CpeActionsService cpeActionsService = SpringUtil.getBean(CpeActionsService.class);

        Envelope informMessage = cpeActionsService.doInform(eventKeyList, sn);
        CPEClientSession cpeClientSession = SpringUtil.getBean(CPEClientSession.class);

        ReentrantLock reentrantLock = ConcurrentManagement.BASE_STATION_SEND_DATA_LOCK.get(sn);
        try {
            reentrantLock.tryLock(CommonConstans.tryLockWaitTime, TimeUnit.MINUTES);

            ACSResponse acsResponse = cpeClientSession.sendDownLoadCompleteInform(informMessage, sn);
            TransferComplete transferComplete = new TransferComplete();
            transferComplete.setCommandKey(commandKey);
            FaultStruct faultStruct = new FaultStruct();
            if (uploadResult) {
                faultStruct.setFaultCode(FaultStruct.FaultCode._0);
            } else {
                faultStruct.setFaultCode(FaultStruct.FaultCode._9010);
            }
            faultStruct.setFaultString("");
            transferComplete.setFaultStruct(faultStruct);
            transferComplete.setStartTime(new Date());
            transferComplete.setCompleteTime(new Date());
            Envelope envelope = CpeActionsService.inEnvelope(transferComplete);
            cpeClientSession.sendTransferCompleteInform(envelope, acsResponse, sn);
        } catch (Exception e) {
            log.error(sn + " send download complete inform get lock failed...", e);
        } finally {
            try {
                reentrantLock.unlock();
            } catch (Exception e) {
                log.error("unlock failed...", e);
            }
        }
    }
}
