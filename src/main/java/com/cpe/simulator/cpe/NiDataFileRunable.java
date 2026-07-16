package com.cpe.simulator.cpe;

import com.cpe.simulator.message.ConcurrentManagement;
import com.cpe.simulator.util.CommonConstans;
import com.cpe.simulator.util.CommonUtil;
import com.cpe.simulator.util.InformConstants;
import com.cpe.simulator.util.SpringUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dslforum.cwmp_1_0.AutonomousTransferComplete;
import org.dslforum.cwmp_1_0.Envelope;
import org.dslforum.cwmp_1_0.EventStruct;
import org.dslforum.cwmp_1_0.FaultStruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Data
public class NiDataFileRunable implements Runnable{

    private String itemSn;

    private String uploadFileDir;

    // 单位分钟
    private String uploadPeriod;

    private String uploadUrl;


    public void sendTransferCompleteInform(String serialNumber, File file) {
        String url = uploadUrl;
        ArrayList<EventStruct> eventKeyList = new ArrayList<>();
        EventStruct eventStruct = new EventStruct();
        eventStruct.setEventCode(EventStructConstants.EVENT_AUT_TRANSFER_COMPLETE);
        eventKeyList.add(eventStruct);
        EventStruct mDownLoadEvent = new EventStruct();
        mDownLoadEvent.setEventCode(EventStructConstants.EVENT_M_UPLOAD_TRANSFER_COMPLETE);
        eventKeyList.add(mDownLoadEvent);

        CpeActionsService cpeActionsService = SpringUtil.getBean(CpeActionsService.class);
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
            autonomousTransferComplete.setFileType(InformConstants.NI_DATA_FILE_TYPE);
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
            log.error(serialNumber + " send NiData file upload transfer message failed", e);
        } finally {
            try {
                reentrantLock.unlock();
            } catch (Exception e) {
                log.error("unlock failed...", e);
            }
        }
    }

    @Override
    public void run() {
        String sourceFilePath = uploadFileDir + File.separator + "niData.tar.gz";
        File file = new File(sourceFilePath);
        if (!file.exists()) {
            log.error("The NI data file doesn't exist......");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        org.springframework.core.io.Resource resource1 = null;
        try {
            resource1 = new ByteArrayResource(Files.readAllBytes(Paths.get(sourceFilePath, new String[0])));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        HttpEntity<byte[]> request = (HttpEntity<byte[]>) new HttpEntity(resource1, headers);

        String fileName = "gNB_Ni_Measure_" + CommonUtil.formatFileDataTime(LocalDateTime.now()) + "+0800_" + itemSn + ".tar.gz";
        String uploadUrlPath = uploadUrl + fileName;
        log.info("Ni data uploadUrl:" + uploadUrlPath);

        RestTemplate restTemplate = SpringUtil.getBean(RestTemplate.class);
        ResponseEntity<Object> response = restTemplate.exchange(uploadUrlPath, HttpMethod.PUT, request, Object.class);

        int resultCode = response.getStatusCode().value();
        boolean uploadResult = resultCode == HttpStatus.CREATED.value() || resultCode == HttpStatus.OK.value();
        if (uploadResult) {
            sendTransferCompleteInform(itemSn, file);
        }
    }
}
