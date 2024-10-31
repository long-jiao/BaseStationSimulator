package com.cpe.simulator.cpe;

import com.cpe.simulator.message.ConcurrentManagement;
import com.cpe.simulator.util.CommonConstans;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.dslforum.cwmp_1_0.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Slf4j
public class CPEClientSession {

    XmlFormatter xmlFmt = null;

    @Resource
    private RestTemplate restTemplate;

    @Value("${omc.url}")
    private String omcUrl;

    @Value("${cwmpver}")
    private String cwmpver;

    @Resource
    private CpeActionsService cpeActionsService;

    public static void main(String[] args) {
        String msg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ZSI=\"http://www.zolera.com/schemas/ZSI/\" xmlns:cwmp=\"urn:dslforum-org:cwmp-1-1\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><SOAP-ENV:Header><cwmp:ID>186690476</cwmp:ID><cwmp:HoldRequests>0</cwmp:HoldRequests></SOAP-ENV:Header><SOAP-ENV:Body><cwmp:InformResponse><MaxEnvelopes>1</MaxEnvelopes></cwmp:InformResponse></SOAP-ENV:Body></SOAP-ENV:Envelope>";
        Envelope classobj = (Envelope) JibxHelper.unmarshalMessage(msg, "cwmp_1_0");
        InformResponse iresp = (InformResponse) classobj.getBody().getObjects().get(0);
        System.out.println(iresp);
    }

    public void sendInform(Envelope envelope, String sn) {
        ReentrantLock reentrantLock = ConcurrentManagement.BASE_STATION_SEND_DATA_LOCK.get(sn);
        try {
            reentrantLock.tryLock(CommonConstans.tryLockWaitTime, TimeUnit.MINUTES);

            String informBody = JibxHelper.marshalObject(envelope, cwmpver);
            ACSResponse acsresp = sendData(informBody, new ArrayList<>());
            log.error(sn + ":Response():" + acsresp.getResponse());
//            Envelope downloadresponse sendclassobj = (Envelope) JibxHelper.unmarshalMessage(acsresp.getResponse(), cwmpver);
//            InformResponse iresp = (InformResponse) classobj.getBody().getObjects().get(0);

            acsresp = sendData(null, acsresp.getCookies());
            log.error(sn + ":SendNull,Response():" + acsresp.getResponse());
            handleACSRequest(acsresp, sn);
        } catch (Exception e) {
            log.error(sn + " send inform get lock failed...", e);
        } finally {
            try {
                reentrantLock.unlock();
            } catch (Exception e) {
                log.error("unlock failed...", e);
            }
        }

        log.info(sn + " sendInform process end...." + ((ID)envelope.getHeader().getObjects().get(0)).getString());
    }

    public ACSResponse sendDownLoadCompleteInform(Envelope envelope, String sn) {
        String informBody = JibxHelper.marshalObject(envelope, cwmpver);
        log.info("downLoad transfer inform:" + informBody);
        ACSResponse acsresp = sendData(informBody, new ArrayList<>());
        ACSResponse newAcsresp = sendData(null, acsresp.getCookies());
        handleACSRequest(newAcsresp, sn);
        log.info("sendDownLoadCompleteInform process end....");
        return acsresp;
    }

    public void sendTransferCompleteInform(Envelope envelope, ACSResponse acsResponse, String sn) {
        String informBody = JibxHelper.marshalObject(envelope, cwmpver);
        log.info("downLoad transfer inform:" + informBody);
        ACSResponse acsresp = sendData(informBody, acsResponse.getCookies());

        acsresp = sendData(null, acsresp.getCookies());
        handleACSRequest(acsresp, sn);
        log.info("sendTransferCompleteInform process end....");
    }

    public void handleACSRequest(ACSResponse acsresp, String sn) {
        String response = acsresp.getResponse();
        if (response != null && response.length() > 0) {
            Envelope envReq = (Envelope) JibxHelper.unmarshalMessage(response, cwmpver);
            Object idObj;
            try {
                idObj = envReq.getHeader().getObjects().get(0);
            } catch (IndexOutOfBoundsException oob) {
                oob.printStackTrace();
                idObj = new ID();
                ((ID) idObj).setMustUnderstand(true);
                sn = "testSN";
                ((ID) idObj).setString(String.format("NOID_%s_SIM_TR69_ID", sn));
            }
            Object reqobj = envReq.getBody().getObjects().get(0);

            log.info("Has New Request by ClassName ===== " + reqobj.getClass().getName() + ":" + sn);
            Envelope envResp = getClientResponse(idObj, reqobj, sn);
            String respBody;
            if (this.xmlFmt == null)
                respBody = JibxHelper.marshalObject(envResp, cwmpver);
            else
                respBody = this.xmlFmt.format(JibxHelper.marshalObject(envResp, cwmpver));

            ACSResponse newresp = sendData(respBody, acsresp.getCookies());

            String reqname = reqobj.getClass().getSimpleName();
            Integer switchId = MessageType.reqProps.get(reqname);
            if (switchId == 10) {
                log.info(sn + ":downloadresponse send, from omc response is: " + newresp.getResponse());
            }
            handleACSRequest(newresp, sn);
        }
    }

    private ACSResponse sendData(String reqString, List<String> cookieList) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        if (!StringUtils.hasLength(reqString)) {
            headers.setContentLength(0);
        }
        if (!CollectionUtils.isEmpty(cookieList)) {
            headers.put(HttpHeaders.COOKIE, cookieList);
        }

        HttpEntity<String> formEntity = new HttpEntity<>(reqString, headers);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(omcUrl, formEntity, String.class);
        List<String> setCookieList = responseEntity.getHeaders().get(HttpHeaders.SET_COOKIE);
        ACSResponse acsresp = new ACSResponse();
        if (cookieList.size() > 0) {
            acsresp.setCookies(cookieList);
        } else {
            acsresp.setCookies(setCookieList);
        }
        acsresp.setHeaders(responseEntity.getHeaders());

        int responseHttpCode = responseEntity.getStatusCode().value();
        if (responseHttpCode == HttpStatus.OK.value()) {
            String rdata = responseEntity.getBody();
            acsresp.setResponse(rdata);
        } else if (responseHttpCode == HttpStatus.NO_CONTENT.value()) {
        } else {
            log.error("send msg to omc failed...." + responseHttpCode);
        }
        return acsresp;
    }

    public Envelope getClientResponse(Object idobject, Object reqobject, String sn) {
        Envelope toreturn = null;
        String reqname = reqobject.getClass().getSimpleName();
        Integer switchId = MessageType.reqProps.get(reqname);

        switch (switchId) {

            case MessageType.GET_RPC_METHODS_ID:
                toreturn = cpeActionsService.doGetRPCMethods();
                break;

            case MessageType.GET_PARAMETER_VALUES_ID:
                GetParameterValues gpv = (GetParameterValues) reqobject;
                toreturn = cpeActionsService.doGetParameterValues(sn, gpv);
                break;

            case MessageType.SET_PARAMETER_VALUES_ID:
                SetParameterValues spv = (SetParameterValues) reqobject;
                toreturn = cpeActionsService.doSetParameterValues(sn, spv);
                break;

            case MessageType.GET_PARAMETER_NAMES_ID:
                GetParameterNames gpn = (GetParameterNames) reqobject;
                toreturn = cpeActionsService.doGetParameterNames(gpn);
                break;

            case MessageType.GET_PARAMETER_ATTRIBUTES_ID:
                GetParameterAttributes gpa = (GetParameterAttributes) reqobject;
                toreturn = cpeActionsService.doGetParameterAttributes(gpa);
                break;

            case MessageType.SET_PARAMETER_ATTRIBUTES_ID:
                SetParameterAttributes spa = (SetParameterAttributes) reqobject;
                toreturn = cpeActionsService.doSetParameterAttributes(spa);
                break;

            case MessageType.ADD_OBJECT_ID:
                AddObject ao = (AddObject) reqobject;
                toreturn = cpeActionsService.doAddObject(ao);
                break;

            case MessageType.DELETE_OBJECT_ID:
                DeleteObject dobj = (DeleteObject) reqobject;
                toreturn = cpeActionsService.doDeleteObject(dobj, sn);
                break;

            case MessageType.REBOOT_ID:
                Reboot reboot = (Reboot) reqobject;
                toreturn = cpeActionsService.doReboot(reboot);
                break;

            case MessageType.DOWNLOAD_ID:
                Download dwobj = (Download) reqobject;
                toreturn = cpeActionsService.doDownload(dwobj, sn);
                break;

//            case MessageType.SCHEDULE_INFORM_ID:
//                ScheduleInform schInform = (ScheduleInform) reqobject;
//                int delaysec = schInform.getDelaySeconds();
//                SchedulerInform siclass = new SchedulerInform(delaysec, username, passwd, authtype, useragent, xmlFmt);
//                Thread sithread = new Thread(siclass, "SchInformThread");
//                sithread.start();
//                break;

            case MessageType.UPLOAD_ID:
                Upload upload = (Upload) reqobject;
                toreturn = cpeActionsService.doUpload(upload, sn);
                break;

            case MessageType.FACTORY_RESET_ID:
                FactoryReset factreset = (FactoryReset) reqobject;
                toreturn = cpeActionsService.doFactoryReset(factreset);
                break;

        }

        if (toreturn != null || Objects.nonNull(idobject)) {
            toreturn.getHeader().setObjects(Lists.newArrayList(idobject));
        }

        return toreturn;
    }


//    public void dumpCurrentConfiguration(String dumploc, String serial) {
//        try {
//            GetParameterNames allParameterNames = new GetParameterNames();
//            allParameterNames.setParameterPath(cpeActions.confdb.props.getProperty("RootNode"));
//            Envelope envNames = cpeActions.doGetParameterNames(allParameterNames);
//            String namesDump = JibxHelper.marshalObject(envNames, "cwmp_1_0");
//            CpeDBReader.serialize(dumploc + "getnames_" + serial + ".xml", new XmlFormatter().format(namesDump));
//
//            ParameterNames pn = new ParameterNames();
//            Set<String> namesSet = new HashSet<String>();
//            namesSet.add(cpeActions.confdb.props.getProperty("RootNode"));
//            pn.setStrings(namesSet.toArray(new String[namesSet.size()]));
//
//            GetParameterValues allParameterValues = new GetParameterValues();
//            allParameterValues.setParameterNames(pn);
//            Envelope envValues = cpeActions.doGetParameterValues(allParameterValues, false, cpeActions.confdb.confs);
//            String valuesDump = JibxHelper.marshalObject(envValues, "cwmp_1_0");
//            CpeDBReader.serialize(dumploc + "getvalues_" + serial + ".xml", new XmlFormatter().format(valuesDump));
//
//            GetParameterValues learnedParameterValues = new GetParameterValues();
//            learnedParameterValues.setParameterNames(pn);
//            Envelope envLearnedValues = cpeActions.doGetParameterValues(learnedParameterValues, false, cpeActions.confdb.learns);
//            String learnedValuesDump = JibxHelper.marshalObject(envLearnedValues, "cwmp_1_0");
//            CpeDBReader.serialize(dumploc + "learnedvalues_" + serial + ".xml", new XmlFormatter().format(learnedValuesDump));
//        } catch (IOException ioex) {
//            ioex.printStackTrace();
//        }
//    }


//    private static URI getBaseURI() {
//        return UriBuilder.fromUri("http://192.168.1.50:8085/ws?wsdl").build();
//    }

	/*private static String getHeader() {
		return "<soap:Header><cwmp:ID soap:mustUnderstand=\"1\">100110</cwmp:ID></soap:Header>";
	}

	private static String envString () {
		return "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
				"xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:cwmp=\"urn:dslforum-org:cwmp-1-0\" " +
				"xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"></soap:Envelope>";
	}

	private static String getInformString() {
		String informString  = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
				"xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:cwmp=\"urn:dslforum-org:cwmp-1-0\" " +
				"xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
				"<soap:Header><cwmp:ID soap:mustUnderstand=\"1\">100110</cwmp:ID></soap:Header><soap:Body>" +
				"<cwmp:Inform><DeviceId><Manufacturer>Conexant</Manufacturer><OUI>002615</OUI><ProductClass>ADSL2+ IAD</ProductClass>" +
				"<SerialNumber>00261559a496</SerialNumber></DeviceId><Event soapenc:arrayType=\"cwmp:EventStruct[1]\">" +
				"<EventStruct><EventCode>4 VALUE CHANGE</EventCode><CommandKey></CommandKey></EventStruct></Event>" +
				"<MaxEnvelopes>1</MaxEnvelopes><CurrentTime>1970-01-03T03:15:45Z</CurrentTime><RetryCount>0</RetryCount>" +
				"<ParameterList soapenc:arrayType=\"cwmp:ParameterValueStruct[9]\"><ParameterValueStruct>" +
				"<Name>InternetGatewayDevice.DeviceInfo.SpecVersion</Name><Value xsi:type=\"xsd:string\">1.0</Value>" +
				"</ParameterValueStruct><ParameterValueStruct><Name>InternetGatewayDevice.DeviceInfo.HardwareVersion</Name>" +
				"<Value xsi:type=\"xsd:string\">Solos 4615 RD / Solos 461x CSP v1.0</Value></ParameterValueStruct><ParameterValueStruct>" +
				"<Name>InternetGatewayDevice.DeviceInfo.SoftwareVersion</Name><Value xsi:type=\"xsd:string\">10.4.3.12.12</Value>" +
				"</ParameterValueStruct><ParameterValueStruct><Name>InternetGatewayDevice.DeviceInfo.ProvisioningCode</Name>" +
				"<Value xsi:type=\"xsd:string\"></Value></ParameterValueStruct><ParameterValueStruct>" +
				"<Name>InternetGatewayDevice.ManagementServer.ConnectionRequestURL</Name>" +
				"<Value xsi:type=\"xsd:string\">http://192.168.1.1:7547/wsdl</Value></ParameterValueStruct>" +
				"<ParameterValueStruct><Name>InternetGatewayDevice.ManagementServer.ParameterKey</Name>" +
				"<Value xsi:type=\"xsd:string\">222333</Value></ParameterValueStruct><ParameterValueStruct>" +
				"<Name>InternetGatewayDevice.DeviceSummary</Name><Value xsi:type=\"xsd:string\">" +
				"InternetGatewayDevice:1.2[](X_002615_Baseline:1,X_002615_EthernetLAN:1,X_002615_WiFiLAN:1,X_002615_ADSLWAN:1,IPPing:1)," +
				"VoiceService:1.0[1](X_002615_Endpoint:1,X_002615_SIPEndpoint:1)</Value></ParameterValueStruct><ParameterValueStruct>" +
				"<Name>InternetGatewayDevice.WANDevice.1.WANConnectionDevice.7.WANPPPConnection.1.ExternalIPAddress</Name>" +
				"<Value xsi:type=\"xsd:string\">117.202.132.137</Value></ParameterValueStruct><ParameterValueStruct>" +
				"<Name>InternetGatewayDevice.ManagementServer.PeriodicInformInterval</Name>" +
				"<Value xsi:type=\"xsd:string\">1800</Value></ParameterValueStruct></ParameterList></cwmp:Inform></soap:Body></soap:Envelope>";
		return informString;
	}


	private static Element getInformBody() {
		Element node = null;
		String informString  = "<soap:Body><cwmp:Inform><DeviceId><Manufacturer>Conexant</Manufacturer><OUI>002615</OUI><ProductClass>ADSL2+ IAD</ProductClass>" +
				"<SerialNumber>00261559a496</SerialNumber></DeviceId><Event soapenc:arrayType=\"cwmp:EventStruct[1]\">" +
				"<EventStruct><EventCode>4 VALUE CHANGE</EventCode><CommandKey></CommandKey></EventStruct></Event>" +
				"<MaxEnvelopes>1</MaxEnvelopes><CurrentTime>1970-01-03T03:15:45Z</CurrentTime><RetryCount>0</RetryCount>" +
				"<ParameterList soapenc:arrayType=\"cwmp:ParameterValueStruct[9]\"><ParameterValueStruct>" +
				"<Name>InternetGatewayDevice.DeviceInfo.SpecVersion</Name><Value xsi:type=\"xsd:string\">1.0</Value>" +
				"</ParameterValueStruct><ParameterValueStruct><Name>InternetGatewayDevice.DeviceInfo.HardwareVersion</Name>" +
				"<Value xsi:type=\"xsd:string\">Solos 4615 RD / Solos 461x CSP v1.0</Value></ParameterValueStruct><ParameterValueStruct>" +
				"<Name>InternetGatewayDevice.DeviceInfo.SoftwareVersion</Name><Value xsi:type=\"xsd:string\">10.4.3.12.12</Value>" +
				"</ParameterValueStruct><ParameterValueStruct><Name>InternetGatewayDevice.DeviceInfo.ProvisioningCode</Name>" +
				"<Value xsi:type=\"xsd:string\"></Value></ParameterValueStruct><ParameterValueStruct>" +
				"<Name>InternetGatewayDevice.ManagementServer.ConnectionRequestURL</Name>" +
				"<Value xsi:type=\"xsd:string\">http://192.168.1.1:7547/wsdl</Value></ParameterValueStruct>" +
				"<ParameterValueStruct><Name>InternetGatewayDevice.ManagementServer.ParameterKey</Name>" +
				"<Value xsi:type=\"xsd:string\">222333</Value></ParameterValueStruct><ParameterValueStruct>" +
				"<Name>InternetGatewayDevice.DeviceSummary</Name><Value xsi:type=\"xsd:string\">" +
				"InternetGatewayDevice:1.2[](X_002615_Baseline:1,X_002615_EthernetLAN:1,X_002615_WiFiLAN:1,X_002615_ADSLWAN:1,IPPing:1)," +
				"VoiceService:1.0[1](X_002615_Endpoint:1,X_002615_SIPEndpoint:1)</Value></ParameterValueStruct><ParameterValueStruct>" +
				"<Name>InternetGatewayDevice.WANDevice.1.WANConnectionDevice.7.WANPPPConnection.1.ExternalIPAddress</Name>" +
				"<Value xsi:type=\"xsd:string\">117.202.132.137</Value></ParameterValueStruct><ParameterValueStruct>" +
				"<Name>InternetGatewayDevice.ManagementServer.PeriodicInformInterval</Name>" +
				"<Value xsi:type=\"xsd:string\">1800</Value></ParameterValueStruct></ParameterList></cwmp:Inform></soap:Body>";

		try {
		node =  DocumentBuilderFactory
			    .newInstance()
			    .newDocumentBuilder()
			    .parse(new ByteArrayInputStream(informString.getBytes()))
			    .getDocumentElement();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return node;
	}*/


//    public class SchedulerInform implements Runnable {
//        int delaysecs;
//        String username = null;
//        String passwd = null;
//        String authtype = null;
//        String useragent = null;
//        XmlFormatter xmlFmt = null;
//
//        public SchedulerInform(int delaysecs, String username, String passwd, String authtype, String useragent, XmlFormatter xmlFmt) {
//            this.delaysecs = delaysecs;
//            this.username = username;
//            this.passwd = passwd;
//            this.authtype = authtype;
//            this.useragent = useragent;
//            this.xmlFmt = xmlFmt;
//        }
//
//        public void run() {
//            try {
//                Thread.sleep(delaysecs * 1000);
//                ArrayList<EventStruct> eventKeyList = new ArrayList<EventStruct>();
//                EventStruct eventStruct = new EventStruct();
//                eventStruct.setEventCode("3 SCHEDULED");
//                eventKeyList.add(eventStruct);
//                Envelope informMessage = cpeActionsService.doInform(eventKeyList, "");
//
//                System.out.println("Sending ScheduleInform Message at " + (new Date()));
//                sendInform(informMessage);
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }


}
