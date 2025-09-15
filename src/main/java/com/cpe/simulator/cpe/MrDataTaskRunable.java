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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Data
@Slf4j
public class MrDataTaskRunable implements Runnable {

    private String serialNumber;

    private String uploadPeriod;

    private String beginTime;

    private String endTime;

    private String uploadFileDir;

    private String uploadUrl;

    @Override
    public void run() {
        String sourceFilePath = uploadFileDir + File.separator + "mrDatafile.gz";

        File file = new File(sourceFilePath);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        org.springframework.core.io.Resource resource1 = null;
        try {
            resource1 = new ByteArrayResource(Files.readAllBytes(Paths.get(sourceFilePath, new String[0])));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        HttpEntity<byte[]> request = (HttpEntity<byte[]>) new HttpEntity(resource1, headers);

        String targetFileName = getMrFileName();
        long length = file.length();
        String url = uploadUrl + "/" + targetFileName;
        log.info("uploadUrl:" + url);

        RestTemplate restTemplate = SpringUtil.getBean(RestTemplate.class);
        ResponseEntity<Object> response = restTemplate.exchange(uploadUrl, HttpMethod.PUT, request, Object.class);
        log.info("upload Mr file result:" + response.getStatusCode().value());
        sendUploadSuccessMsg(length, targetFileName, url);
    }

    public boolean sendUploadSuccessMsg(long fileSize, String fileName, String url) {

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
            autonomousTransferComplete.setFileType(InformConstants.AUTO_NOMOUS_TRANSFERCOMPLETE_FILE_TYPE);
            autonomousTransferComplete.setFileSize(String.valueOf(fileSize));
            autonomousTransferComplete.setTargetFileName(fileName);
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

    private String getMrFileName() {
        CpeDBReader cpeDBReader = SpringUtil.getBean(CpeDBReader.class);
        String ouiValue = cpeDBReader.getValue(serialNumber, InformConstants.MU_MANUFACTUREROUI);
        StringBuilder nameBuilder = new StringBuilder("NR_MRO_");
        nameBuilder.append(ouiValue).append("_").append(serialNumber).append("_");
        LocalDateTime now = LocalDateTime.now();
        String formatTime = now.format(CommonUtil.dateTimeFormatter);
        nameBuilder.append(formatTime).append(".csv.gz");
        return nameBuilder.toString();
    }
}
