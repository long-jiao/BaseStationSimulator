package com.cpe.simulator.cpe;

import com.cpe.simulator.bean.RegisterEnbInfo;
import com.cpe.simulator.util.CommonConstans;
import com.cpe.simulator.util.InformConstants;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dslforum.cwmp_1_0.Envelope;
import org.dslforum.cwmp_1_0.GetParameterValuesResponse;
import org.dslforum.cwmp_1_0.ParameterValueList;
import org.dslforum.cwmp_1_0.ParameterValueStruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author anders
 */
@Service
@Slf4j
public class CpeDBReader implements Serializable {

    private static final long serialVersionUID = -2634321577569129211L;
    private String dumploc;
    Properties props = new Properties();

    @Value("${omc.url}")
    private String omcUrl;

    @Getter
    @Setter
    private Table<String, String, String> snToParaPathToValue = HashBasedTable.create();

    @Getter
    @Setter
    private Map<String, Set<String>> serialNumToCellIdSet = new HashMap<>();

    @Value("${cpe.paraValuePath}")
    private String valueFilePath;

    @Value("${cpe.bs5524ValuePath}")
    private String bs5524ValuePath;

    @Value("${cwmpver}")
    private String cwmpver;

    @Resource
    private List<RegisterEnbInfo> registerEnbInfoList;

    @Value("${BS.Ip}")
    private String baseStationIp;

    @Value("${http.port}")
    private int httpPort;

    private String cpeUrlPrefix;

    private CpeDBReader() {
    }

    public CpeDBReader(String dumploc) {
        this.dumploc = dumploc;
    }


    public String getDumpLocation() {
        return this.dumploc;
    }

    @PostConstruct
    public void readFromGetMessages() {
        initBaseStationIP();

        String xmlcontent2 = deserialize(valueFilePath);
        Envelope classobj2 = (Envelope) JibxHelper.unmarshalMessage(xmlcontent2, cwmpver);

        Object tempobj2 = classobj2.getBody().getObjects().get(0);
        ParameterValueList pvlist = ((GetParameterValuesResponse) tempobj2).getParameterList();
        List valuelist = pvlist.getParameterValueStruct();
        List<String> bsType1SnList = registerEnbInfoList.stream().filter(it -> it.getStationType().equals(CommonConstans.BASESTATION_TYPE_BS5514)).map(it -> it.getSn()).collect(Collectors.toList());
        for (String itemSn : bsType1SnList) {
            for (Iterator e = valuelist.iterator(); e.hasNext(); ) {
                ParameterValueStruct pvs = (ParameterValueStruct) e.next();
                String namestr = pvs.getName();
                String valuestr = pvs.getValue();
                snToParaPathToValue.put(itemSn, namestr, valuestr);
                if (namestr.startsWith(InformConstants.CELLID_PATH_PREFIX) && namestr.endsWith(InformConstants.CELLID_PATH_SUFFIX)) {
                    Set<String> cellIdSet = serialNumToCellIdSet.getOrDefault(itemSn, new HashSet<>());
                    cellIdSet.add(valuestr);
                    serialNumToCellIdSet.put(itemSn, cellIdSet);
                }
            }
            snToParaPathToValue.put(itemSn, "Device.ManagementServer.URL", omcUrl);
            snToParaPathToValue.put(itemSn, "Device.ManagementServer.ConnectionRequestURL", cpeUrlPrefix + itemSn);
            snToParaPathToValue.put(itemSn, InformConstants.MU_SERIALNUMBER, itemSn);
        }

        String xmlcontent2Type2 = deserialize(bs5524ValuePath);
        Envelope classobj2Type2 = (Envelope) JibxHelper.unmarshalMessage(xmlcontent2Type2, cwmpver);

        Object tempobj2Type2 = classobj2Type2.getBody().getObjects().get(0);
        ParameterValueList pvlistType2 = ((GetParameterValuesResponse) tempobj2Type2).getParameterList();
        List valuelist2Type2 = pvlistType2.getParameterValueStruct();
        List<String> bsType2SnList = registerEnbInfoList.stream().filter(it -> it.getStationType().equals(CommonConstans.BASESTATION_TYPE_BS5524)).map(it -> it.getSn()).collect(Collectors.toList());
        for (String itemSn : bsType2SnList) {
            for (Iterator e = valuelist2Type2.iterator(); e.hasNext(); ) {
                ParameterValueStruct pvs = (ParameterValueStruct) e.next();
                String namestr = pvs.getName();
                String valuestr = pvs.getValue();
                snToParaPathToValue.put(itemSn, namestr, valuestr);
                if (namestr.startsWith(InformConstants.CELLID_PATH_PREFIX) && namestr.endsWith(InformConstants.CELLID_PATH_SUFFIX)) {
                    Set<String> cellIdSet = serialNumToCellIdSet.getOrDefault(itemSn, new HashSet<>());
                    cellIdSet.add(valuestr);
                    serialNumToCellIdSet.put(itemSn, cellIdSet);
                }
            }
            snToParaPathToValue.put(itemSn, "Device.ManagementServer.URL", omcUrl);
            snToParaPathToValue.put(itemSn, "Device.ManagementServer.ConnectionRequestURL", cpeUrlPrefix + itemSn);
            snToParaPathToValue.put(itemSn, InformConstants.MU_SERIALNUMBER, itemSn);
        }
    }

    private void initBaseStationIP() {
        try {
            String ip;
            if (StringUtils.hasLength(baseStationIp)) {
                ip = baseStationIp;
            } else {
                InetAddress addr = InetAddress.getLocalHost();
                ip = addr.getHostAddress();
            }
            if (ip.contains(":")) {
                ip = "[" + ip + "]";
            }

            cpeUrlPrefix = "http://" + ip + ":" + httpPort + "/";
        } catch (UnknownHostException e) {
            log.error(e.getMessage(), e);
            cpeUrlPrefix = "http://" + "127.0.0.1" + ":" + httpPort + "/";
        }
    }
	
	public String getValueOrDefault(String sn, String paraPath, String defaultValue) {
        if (snToParaPathToValue.contains(sn, paraPath)) {
            return snToParaPathToValue.get(sn, paraPath);
        }
        return defaultValue;
    }

    public String getValue(String sn, String paraPath) {
        return getValueOrDefault(sn, paraPath, "");
    }

    public void setValue(String sn, String paraPath, String newValue) {
        snToParaPathToValue.put(sn, paraPath, newValue);
    }


    static void serialize(String filepath, String content) throws IOException {
        if (filepath == null) {
            throw new IllegalArgumentException("File should not be null.");
        }
        File aFile = new File(filepath);
        Writer output = new BufferedWriter(new FileWriter(aFile));
        try {
            output.write(content);
        } finally {
            output.close();
        }
    }

    public String deserialize(String filepath) {
        //File aFile = new File (filepath);
        log.info("deserialize CpeConfDB >>>>>> : " + filepath);
        StringBuilder contents = new StringBuilder();
        try {
            BufferedReader input = null;
            File infile = new File(filepath);
            if (infile.exists()) {
                input = new BufferedReader(new FileReader(infile));
            } else {
                InputStream is = this.getClass().getResourceAsStream(filepath);
                input = new BufferedReader(new InputStreamReader(is));
            }
            try {
                String line = null;
                while ((line = input.readLine()) != null) {
                    contents.append(line);
                }
            } finally {
                input.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return contents.toString();
    }

    private Properties setCPEKeyProperties(String baseDir) {
        Properties tprops = null;
        String filepath = baseDir + "cpekeys.properties";
        InputStream is = null;
        try {
            File infile = new File(filepath);
            if (infile.exists()) {
                is = new FileInputStream(infile);
                System.out.println("Properties has been loaded from FLAT File >>>>>> " + filepath);
            } else {
                is = this.getClass().getResourceAsStream(filepath);
                System.out.println("Properties has been loaded from JAR File >>>>>> " + filepath);
            }
            if (is != null) {
                tprops = new Properties();
                tprops.load(is);
                System.out.println("Properties has been loaded successfully >>>>>> " + tprops.toString());
            }
        } catch (IOException ex) {
            System.out.println("Exception in reading CPE Keys property file " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
        }
        if (tprops == null) {
            tprops = new Properties();
            tprops.setProperty("RootNode", "InternetGatewayDevice.");
            tprops.setProperty("MgmtServer_URL", "InternetGatewayDevice.ManagementServer.URL");
            tprops.setProperty("ConnectionRequestURL", "InternetGatewayDevice.ManagementServer.ConnectionRequestURL");
            tprops.setProperty("PeriodicInformInterval", "InternetGatewayDevice.ManagementServer.PeriodicInformInterval");
            tprops.setProperty("SerialNumber", "InternetGatewayDevice.DeviceInfo.SerialNumber");
            tprops.setProperty("ExternalIPAddress", "InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANPPPConnection.1.ExternalIPAddress");
            tprops.setProperty("AdditionalInformParameters", "");
            System.out.println("Properties has been loaded from Default Keys >>>>>> ");
        }
        return tprops;
    }

    public static void main(String[] args) {
        System.out.println("Starting CpeConfDB");

        //def c = CpeConfDB.readFromGetMessages('testfiles/parameters_zyxel2602/')
        //CpeDBReader c = CpeDBReader.readFromGetMessages("D://Paraam//ACS//femto//microcell//");
//        CpeDBReader c = new CpeDBReader().readFromGetMessages("/dump/microcell/");

        //CpeDBReader c = CpeDBReader.readFromGetMessages("D://Paraam//ACS//groovy_src//groovycpe//testfiles//parameters_zyxel2602//");
//        System.out.println(" Hashtable >>>>>> " + c.confs.toString());
//        System.out.println(" SerialNumber ---->  " + ((ConfParameter) c.confs.get("Device.DeviceInfo.SerialNumber")).value);
//        //System.out.println( " SerialNumber ---->  " + ((ConfParameter)c.confs.get("InternetGatewayDevice.DeviceInfo.SerialNumber")).value);
        //c.serialize("test.txt");
        //System.out.println( " " + CpeDBReader.deserialize("test.txt"));
    }
}
