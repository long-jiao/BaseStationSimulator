package com.cpe.simulator.cpe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CpeDBReaderTest {

    @Resource
    private CpeDBReader cpeDBReader;

    @Test
    void readFromGetMessages() {
        System.out.println(cpeDBReader.getValueOrDefault("test", "Device.DeviceInfo.MU.1.HardwareVersion", "notExist"));
    }
}