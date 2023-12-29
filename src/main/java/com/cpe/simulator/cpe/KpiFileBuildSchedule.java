package com.cpe.simulator.cpe;

import com.cpe.simulator.util.InformConstants;
import com.cpe.simulator.util.KpiFileUtils;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class KpiFileBuildSchedule {

    @Resource
    private ThreadPoolTaskExecutor kpiFileBuildPoolManagement;

    @Resource
    private List<String> registerEnbSn;

    @Resource
    private CpeDBReader cpeDBReader;

    @Resource
    private Multimap<String, String> objectToCounters;

    @Resource
    private PmFileServie pmFileServie;

    @Value("${reportPm:false}")
    private boolean reportPm;

    private DateTimeFormatter dataFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    @Scheduled(cron = "0 0/15 * * * ?")
    public void buildKpiFile() {
        if (!reportPm) {
            log.info("reportPm value is: " + reportPm);
            return;
        }
        ZonedDateTime currentDate = ZonedDateTime.now().withSecond(0);
        String endTime = currentDate.format(dataFormatter);
        String startTime = currentDate.minusMinutes(15L).format(dataFormatter);
        Map<String, String> dateMap = new HashMap<String, String>();
        dateMap.put("startTime", startTime);
        dateMap.put("endTime", endTime);
        for (String itemSn : registerEnbSn) {
            kpiFileBuildPoolManagement.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        String gnbId = cpeDBReader.getValue(itemSn, InformConstants.GNBID_PATH);
                        Set<String> cellIdSet = cpeDBReader.getSerialNumToCellIdSet().getOrDefault(itemSn, Sets.newHashSet());
                        String srcFilePath = KpiFileUtils.generateXml(objectToCounters, gnbId, cellIdSet, dateMap, itemSn);
                        String outFilePath = KpiFileUtils.fileToGzFile(srcFilePath);
                        String uploadResult = pmFileServie.uploadFile(outFilePath, itemSn);
                        log.info("upload file:" + outFilePath + ", upload result:" + uploadResult);
                        if (pmFileServie.sendUploadSuccessMsg(outFilePath, itemSn)) {
                            Files.delete(Paths.get(srcFilePath));
                            Files.delete(Paths.get(outFilePath));
                        }
                    } catch (Exception e) {
                        log.error("delect pm file failed...", e);
                    }
                }
            });
        }
    }
}
