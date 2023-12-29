package com.cpe.simulator;

import com.cpe.simulator.bean.RegisterEnbInfo;
import com.cpe.simulator.http.HttpsClientRequestFactory;
import com.cpe.simulator.message.ConcurrentManagement;
import com.cpe.simulator.util.CommonConstans;
import com.cpe.simulator.util.CommonUtil;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.cpe.simulator.mapper")
@Slf4j
public class CpeSimulatorApplication {

    @Value("${snFilePath:D:\\testDownLoadSoft\\sn.xlsx}")
    private String snFilePath;

    @Value("${kpi.counterFilePath}")
    private String counterFilePath;

    @Value("${omc.url}")
    private String omcUrl;

    private List<String> snList = new ArrayList<>();

    public static void main(String[] args) {
        SpringApplication.run(CpeSimulatorApplication.class, args);
    }


    @Bean
    public RestTemplate initRestTemplate() {
//        return new RestTemplate(new HttpsClientRequestFactory(omcUrl));

        return configRestemplate();
    }

    @Bean(name = "RegisterEnbInfoList")
    public List<RegisterEnbInfo> initRegisterEnb() {
        List<RegisterEnbInfo> enbSnList = new ArrayList<>();
        XSSFWorkbook xssfWorkbook = null;
        try {
            xssfWorkbook = new XSSFWorkbook(snFilePath);
            XSSFSheet snSheet = xssfWorkbook.getSheetAt(0);
            int snNums = snSheet.getPhysicalNumberOfRows();
            for (int i = 1; i < snNums; i++) {
                XSSFRow row = snSheet.getRow(i);
                String itemSn = row.getCell(0).toString();
                if (!CommonUtil.strHasLength(itemSn)) {
                    continue;
                }

                Optional<RegisterEnbInfo> optional = enbSnList.stream().filter(it -> it.getSn().equals(itemSn)).findFirst();
                if (optional.isPresent()) {
                    continue;
                }
                RegisterEnbInfo itemEnbInfo = new RegisterEnbInfo();
                itemEnbInfo.setSn(itemSn);
                XSSFCell typeCell = row.getCell(3);
                if (Objects.isNull(typeCell)) {
                    itemEnbInfo.setStationType(CommonConstans.BASESTATION_TYPE_BS5514);
                } else {
                    typeCell.setCellType(CellType.STRING);
                    String bsType = typeCell.getStringCellValue();
                    if (CommonUtil.strHasLength(bsType)) {
                        itemEnbInfo.setStationType(bsType);
                    } else {
                        itemEnbInfo.setStationType(CommonConstans.BASESTATION_TYPE_BS5514);
                    }
                }
                enbSnList.add(itemEnbInfo);
                ConcurrentManagement.BASE_STATION_SEND_DATA_LOCK.put(itemSn, new ReentrantLock());
            }
            xssfWorkbook.close();
        } catch (IOException e) {
            log.error("", e);
        }

        return enbSnList;
    }


    @Bean(name = "RegisterEnbSn")
    public List<String> initRegisterSnList() {
        if (snList.size() == 0) {
            XSSFWorkbook xssfWorkbook = null;
            try {
                xssfWorkbook = new XSSFWorkbook(snFilePath);
                XSSFSheet snSheet = xssfWorkbook.getSheetAt(0);
                int snNums = snSheet.getPhysicalNumberOfRows();
                for (int i = 1; i < snNums; i++) {
                    XSSFRow row = snSheet.getRow(i);
                    String itemSn = row.getCell(0).toString();
                    if (!CommonUtil.strHasLength(itemSn)) {
                        continue;
                    }
                    if (!snList.contains(itemSn)) {
                        snList.add(itemSn);
                    }
                }
                xssfWorkbook.close();
            } catch (IOException e) {
                log.error("", e);
            }
        }
        return snList;
    }

    @Bean(name = "objectToCounters")
    public Multimap<String, String> initCounters() {
        Multimap<String, String> objectToCounters = ArrayListMultimap.create();
        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook(counterFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        XSSFSheet sheet = workbook.getSheet("NR.Counters");
        for (int rows = sheet.getPhysicalNumberOfRows(), i = 1; i < rows; ++i) {
            XSSFRow row = sheet.getRow(i);
            String measureObjectType = row.getCell(0).toString();
            if (!CommonUtil.strHasLength(measureObjectType)) {
                break;
            }
            String counter = row.getCell(6).toString();
            objectToCounters.put(measureObjectType, counter.replace("C", ""));
        }
        return objectToCounters;
    }

    public RestTemplate configRestemplate() {
        SSLContext sslContext = null;
        try {
            sslContext = new SSLContextBuilder().loadTrustMaterial(null, (arg0, arg1) -> true).build();


            HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;
            SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);

            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslConnectionSocketFactory).build();
            PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            poolingHttpClientConnectionManager.setMaxTotal(600);
            poolingHttpClientConnectionManager.setDefaultMaxPerRoute(800);

            CloseableHttpClient build = HttpClientBuilder.create().disableCookieManagement().setSSLContext(sslContext).setConnectionManager(poolingHttpClientConnectionManager).build();
            HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(build);
            httpComponentsClientHttpRequestFactory.setReadTimeout(90000);
            httpComponentsClientHttpRequestFactory.setConnectTimeout(60000);
            return new RestTemplate(httpComponentsClientHttpRequestFactory);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

        return new RestTemplate();
    }
}
