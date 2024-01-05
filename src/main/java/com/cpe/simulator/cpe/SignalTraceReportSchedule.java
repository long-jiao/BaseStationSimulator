package com.cpe.simulator.cpe;

import com.cpe.simulator.util.CommonUtil;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;

@Component
@Slf4j
public class SignalTraceReportSchedule {

    @Resource
    private ThreadPoolTaskExecutor kpiFileBuildPoolManagement;

    @Resource
    private List<String> registerEnbSn;

    @Value("${omc.url}")
    private String omcUrl;

    @Value("${uploadFileDir:./config}")
    private String uploadFileDir;

    @Value("${reportSignalTrace:false}")
    private boolean reportSignal;

    @Resource
    private RestTemplate restTemplate;

    @Scheduled(fixedRate = 900000)
    public void uploadSingalTraceReport() {
        if (!reportSignal) {
            log.info("reportSignalTrace value is: " + reportSignal);
            return;
        }
        for (String itemSn : registerEnbSn) {
            kpiFileBuildPoolManagement.submit(new Runnable() {
                @Override
                public void run() {
                    String sourceFilePath = uploadFileDir + File.separator + "signalTrace.xst";
                    File file = new File(sourceFilePath);
                    if (!file.exists()) {
                        log.error("The signalTrace file doesn't exist......");
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

                    String fileName = CommonUtil.formatFileDataTime(LocalDateTime.now()) + "." + itemSn + ".00060b6f9efc9c26.xst";
                    String uploadUrl = omcUrl.replace("acs", "api/httpfs/v1/upload/traceData/") + itemSn + "/" + fileName;
                    log.info("uploadUrl:" + uploadUrl);
                    ResponseEntity<Object> response = restTemplate.exchange(uploadUrl, HttpMethod.PUT, request, Object.class);
                    log.info("upload signal trace report result: " + response.getStatusCode());
                }
            });
        }
    }
}
