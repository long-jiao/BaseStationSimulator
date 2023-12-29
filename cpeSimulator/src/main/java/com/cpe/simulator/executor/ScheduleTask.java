package com.cpe.simulator.executor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class ScheduleTask {

    @Value("${download.path}")
    private String downLoadPath;

    @Scheduled(cron = "0 0 1 * * ?")
    public void cleanDownloadFile() {
        File cleanFile = new File(downLoadPath);
        if (cleanFile.exists() && cleanFile.isDirectory()) {
            log.info("begin clean download file....");
            try {
                Files.walkFileTree(Paths.get(downLoadPath), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        long lastModifiedTime = file.toFile().lastModified();
                        Instant instant = Instant.ofEpochMilli(lastModifiedTime);
                        LocalDateTime fileDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                        LocalDateTime expireDateTime = fileDateTime.plusDays(2);
                        if (expireDateTime.isBefore(LocalDateTime.now())) {
                            Files.delete(file);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }
}
