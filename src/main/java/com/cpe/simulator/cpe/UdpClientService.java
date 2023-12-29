package com.cpe.simulator.cpe;

import com.cpe.simulator.util.InformConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.net.*;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
public class UdpClientService {

    @Resource
    private List<String> registerEnbSn;

    @Resource
    private CpeDBReader cpeDBReader;

    @Value("${omc.url}")
    private String omcUrl;

    private InetAddress inetAddress;

    private static String deviceData = "0000001a00000000b2db5000000000008d24b00000000000000003b00000000000001848000000000000014000000000000001a80000000000000000000000" +
            "0000000000000000380000003800000049ffffff80000000460000004a0000000b000000000000000000000000000000000000000000000000000000000000000000000000";

    private byte[] deviceDataBytes;

    @PostConstruct
    public void initDataToBytes() {
        deviceDataBytes = new byte[deviceData.length() / 2];
        for (int i = 0; i < deviceData.length() / 2; i++) {
            String subStr = deviceData.substring(i * 2, i * 2 + 2);
            deviceDataBytes[i] = (byte) Integer.parseInt(subStr, 16);
        }
        inetAddress = initInetAddress();
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    public void sendDeviceMonitoInfo() {
        if (Objects.isNull(inetAddress)) {
            inetAddress = initInetAddress();
        }

        log.info("send udp message of rrdData");
        try (DatagramSocket socket = new DatagramSocket()) {
            for (String itemSn : registerEnbSn) {
                String rrdSwitch = cpeDBReader.getValueOrDefault(itemSn, InformConstants.RRD_DATA_SWITCH, "0");
                if (rrdSwitch.equals("0")) {
                    continue;
                }

                byte[] deviceSnBytes = itemSn.getBytes();
                byte[] allBytes = new byte[deviceSnBytes.length + deviceDataBytes.length];
                System.arraycopy(deviceSnBytes, 0, allBytes, 0, deviceSnBytes.length);
                System.arraycopy(deviceDataBytes, 0, allBytes, deviceSnBytes.length, deviceDataBytes.length);
                DatagramPacket packet = new DatagramPacket(allBytes, allBytes.length, inetAddress, 9010);
                socket.send(packet);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private InetAddress initInetAddress() {
        InetAddress inetAddress = null;
        try {
            URI uri = URI.create(omcUrl);
            inetAddress = InetAddress.getByName(uri.getHost());
        } catch (UnknownHostException e) {
            log.error(e.getMessage(), e);
        }
        return inetAddress;
    }

}
