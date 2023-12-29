package com.cpe.simulator;

import com.cpe.simulator.mapper.DeviceInfoMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
public class MapperTest {

    @Resource
    private DeviceInfoMapper deviceInfoMapper;

    @Test
    public void testMapper() {

        System.out.println(deviceInfoMapper.selectList(null).size());
    }


}
