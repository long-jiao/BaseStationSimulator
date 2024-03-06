package com.cpe.simulator.cpe;

import com.cpe.simulator.message.ConcurrentManagement;
import com.cpe.simulator.util.CommonConstans;
import com.cpe.simulator.util.InformConstants;
import com.cpe.simulator.util.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.dslforum.cwmp_1_0.AutonomousTransferComplete;
import org.dslforum.cwmp_1_0.Envelope;
import org.dslforum.cwmp_1_0.EventStruct;
import org.dslforum.cwmp_1_0.FaultStruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.RequestScope;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class PmFileServie {

    @Resource
    private RestTemplate restTemplate;

    @Resource
    private CpeDBReader cpeDBReader;

    @Value("${omc.url}")
    private String omcUrl;

    @Resource
    private CpeActionsService cpeActionsService;

    public String uploadFile(String filePath, String serialNumber) {
        File file = new File(filePath);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        org.springframework.core.io.Resource resource1 = null;
        try {
            resource1 = new ByteArrayResource(Files.readAllBytes(Paths.get(filePath, new String[0])));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        HttpEntity<byte[]> request = (HttpEntity<byte[]>) new HttpEntity(resource1, headers);

        String uploadUrl = omcUrl.replace("acs", "api/httpfs/v1/upload/pm/") + serialNumber + "/" + file.getName();
        log.info("uploadUrl:" + uploadUrl);
        ResponseEntity<Object> response = restTemplate.exchange(uploadUrl, HttpMethod.PUT, request, Object.class);
        return String.valueOf(response.getStatusCode().value());
    }

    public boolean sendUploadSuccessMsg(String filePath, String serialNumber) {
        File file = new File(filePath);
        String url = omcUrl.replace("acs", "api/httpfs/v1/upload/pm/") + serialNumber;
        ArrayList<EventStruct> eventKeyList = new ArrayList<>();
        EventStruct eventStruct = new EventStruct();
        eventStruct.setEventCode(EventStructConstants.EVENT_AUT_TRANSFER_COMPLETE);
        eventKeyList.add(eventStruct);
        EventStruct mDownLoadEvent = new EventStruct();
        mDownLoadEvent.setEventCode(EventStructConstants.EVENT_M_UPLOAD_TRANSFER_COMPLETE);
        eventKeyList.add(mDownLoadEvent);

        Envelope informMessage = cpeActionsService.doInform(eventKeyList, serialNumber);
        CPEClientSession cpeClientSession = SpringUtil.getBean(CPEClientSession.class);

        ReentrantLock reentrantLock = ConcurrentManagement.BASE_STATION_SEND_DATA_LOCK.get(serialNumber);
        try {
            reentrantLock.tryLock(CommonConstans.tryLockWaitTime, TimeUnit.MINUTES);
            ACSResponse acsResponse = cpeClientSession.sendDownLoadCompleteInform(informMessage, serialNumber);
            AutonomousTransferComplete autonomousTransferComplete = new AutonomousTransferComplete();
            autonomousTransferComplete.setAnnounceURL("");
            autonomousTransferComplete.setTransferURL(url);
            autonomousTransferComplete.setIsDownload("1");
            autonomousTransferComplete.setFileType(InformConstants.AUTO_NOMOUS_TRANSFERCOMPLETE_FILE_TYPE);
            autonomousTransferComplete.setFileSize(String.valueOf(file.length()));
            autonomousTransferComplete.setTargetFileName(file.getName());
            FaultStruct faultStruct = new FaultStruct();
            faultStruct.setFaultCode(FaultStruct.FaultCode._0);
            faultStruct.setFaultString("");
            autonomousTransferComplete.setFaultStruct(faultStruct);
            autonomousTransferComplete.setStartTime(new Date());
            autonomousTransferComplete.setCompleteTime(new Date());
            Envelope envelope = CpeActionsService.inEnvelope(autonomousTransferComplete);
            cpeClientSession.sendTransferCompleteInform(envelope, acsResponse, serialNumber);
        } catch (Exception e) {
            log.error(serialNumber + " send kpi file upload transfer message failed", e);
            return false;
        } finally {
            try {
                reentrantLock.unlock();
            } catch (Exception e) {
                log.error("unlock failed...", e);
            }
        }

        return true;
    }
}
