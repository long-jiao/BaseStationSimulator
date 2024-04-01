package com.cpe.simulator.cpe;

import com.cpe.simulator.mapper.DeviceInfoMapper;
import com.cpe.simulator.util.CommonUtil;
import com.cpe.simulator.util.InformConstants;
import lombok.extern.slf4j.Slf4j;
import org.dslforum.cwmp_1_0.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CpeActionsService {

    @Resource
    private CpeDBReader cpeDBReader;

    @Resource
    private DeviceInfoMapper deviceInfoMapper;

    @Value("${download.path}")
    private String downLoadPath;

    /**
     * 0:不执行，1：执行下载版本
     */
    @Value("${download.execute}")
    private String downLoadVersion;

    @Value("${download.time}")
    private int delayTime;

    @Resource
    private ThreadPoolTaskExecutor processMsgPoolManagement;

    @Resource
    private ThreadPoolTaskExecutor downloadPoolManagement;

    @Resource
    private ThreadPoolTaskExecutor uploadFilePoolManagement;

    @Value("${uploadFileDir:./config}")
    private String uploadFileDir;

    public static void main(String[] args) {
//        System.out.println("Starting CpeConfDB");
//
//        //def c = CpeConfDB.readFromGetMessages('testfiles/parameters_zyxel2602/')
//        CpeDBReader c = CpeDBReader.readFromGetMessages("/dump/microcell/");
//        System.out.println(" Hashtable >>>>>> " + c.confs.toString());
//        System.out.println(" SerialNumber ---->  " + ((ConfParameter) c.confs.get("Device.DeviceInfo.SerialNumber")).value);
//        //c.serialize("test.txt");
//        //System.out.println( " " + CpeDBReader.deserialize("test.txt"));
//        ArrayList<EventStruct> eventKeyList = new ArrayList<EventStruct>();
//        EventStruct eventStruct = new EventStruct();
//        eventStruct.setEventCode("1 BOOT");
//        eventKeyList.add(eventStruct);
//        CpeActionsService cpeactions = new CpeActionsService(c);
//        Envelope evn = cpeactions.doInform(eventKeyList, "");
//        String str = JibxHelper.marshalObject(evn, "cwmp_1_0");
//        System.out.println("" + str);
    }

    public Envelope doInform(ArrayList eventKeyList, String sn, List<ParameterValueStruct>... extraPara) {
        Inform inform = new Inform();
        DeviceIdStruct deviceId = new DeviceIdStruct();
        inform.setDeviceId(deviceId);
        inform.setMaxEnvelopes(1);
        inform.setCurrentTime(new Date());
        inform.setRetryCount(0);

        deviceId.setManufacturer(cpeDBReader.getValue(sn, InformConstants.MU_MANUFACTURER));
        deviceId.setOUI(cpeDBReader.getValue(sn, InformConstants.MU_MANUFACTUREROUI));
        deviceId.setSerialNumber(sn);
        deviceId.setProductClass(cpeDBReader.getValue(sn, InformConstants.MU_PRODUCTCLASS));


        ParameterValueList pvlist = new ParameterValueList();
        // use a mixed list of fixed and custom keys
        ArrayList<String> pList = new ArrayList<>();

        pList.add(InformConstants.ROOT_DATAMODEL_VERSION);
        pList.add(InformConstants.MU_HARDWARE_VERSION);
        pList.add(InformConstants.MU_SOFTWARE_VERSION);
        pList.add(InformConstants.PROVISIONING_CODE);
        pList.add(InformConstants.PARAMETER_KEY);
        pList.add(InformConstants.CONNECTION_REQUEST_URL);

        ArrayList<ParameterValueStruct> arr = new ArrayList();
        for (String p : pList) {
            ParameterValueStruct pvstruct = new ParameterValueStruct();
            pvstruct.setName(p);
            String value = cpeDBReader.getValue(sn, p);
            pvstruct.setValue(value);
            arr.add(pvstruct);
        }

        // 对于value change消息类型，携带变化的参数
        for (List<ParameterValueStruct> paraStructList : extraPara) {
            arr.addAll(paraStructList);
        }

        pvlist.setParameterValueStruct(arr);
        inform.setParameterList(pvlist);

        //ArrayList<String> tlist = new ArrayList();
        //tlist.add("1 BOOT");
        EventList eventList = new EventList();
        eventList.setEventStruct(eventKeyList);
        inform.setEvent(eventList);

        return inEnvelope(inform);
    }

    public Envelope doGetRPCMethods() {
        GetRPCMethodsResponse resp = new GetRPCMethodsResponse();
        MethodList methodList = new MethodList();
//        String[] methods = {"GetRPCMethods", "SetParameterValues", "GetParameterValues", "GetParameterNames",
//                "SetParameterAttributes", "GetParameterAttributes", "AddObject", "DeleteObject",
//                "Reboot", "Download", "ScheduleInform", "Upload", "FactoryReset"};
//        methodList.getArray().getAnyList().addAll(methods);
        resp.setMethodList(methodList);
        return inEnvelope(resp);
    }


    public Envelope doGetParameterValues(String sn, GetParameterValues getParameterValues) {
        return this.doGetParameterValues(getParameterValues, sn);
    }

    public Envelope doGetParameterValues(GetParameterValues getParameterValues, String sn) {
        ParameterValueList pvl = new ParameterValueList();
        String[] nameList = getParameterValues.getParameterNames().getStrings();
        GetParameterValuesResponse valresp = new GetParameterValuesResponse();

        if ((nameList.length == 1) && nameList[0].trim().isEmpty()) {
            for (String paraPath : cpeDBReader.getSnToParaPathToValue().columnKeySet()) {
                ParameterValueStruct pvs = new ParameterValueStruct();
                pvs.setName(paraPath);
                pvs.setValue(cpeDBReader.getValue(sn, paraPath));
                pvl.getParameterValueStruct().add(pvs);
            }
        } else {
            for (int i = 0; i < nameList.length; i++) {
                String paramname = nameList[i];
                if (paramname.endsWith(".")) {
                    Iterator it = cpeDBReader.getSnToParaPathToValue().row(sn).entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String, String> pairs = (Map.Entry) it.next();
                        String keyname = pairs.getKey();
                        if (keyname.startsWith(paramname)) {
                            ParameterValueStruct pvs = new ParameterValueStruct();
                            pvs.setName(keyname);
                            pvs.setValue(pairs.getValue());
                            pvl.getParameterValueStruct().add(pvs);
                        }
                    }
                } else if (cpeDBReader.getSnToParaPathToValue().contains(sn, paramname)) {
                    ParameterValueStruct pvs = new ParameterValueStruct();
                    pvs.setName(paramname);
                    pvs.setValue(cpeDBReader.getValue(sn, paramname));
                    pvl.getParameterValueStruct().add(pvs);
                } else {
                    log.info("undefine paraPath: " + paramname);
                }
            }
        }

        if (nameList[0].equals("Device.")) {
            processMsgPoolManagement.submit(new ValueChangeCellAvailableState(sn, 1));
        }

        valresp.setParameterList(pvl);
        return inEnvelope(valresp);
    }

    public Envelope doGetParameterNames(GetParameterNames getParameterName) {
        return this.doGetParameterNames(getParameterName, true);
    }

    public Envelope doGetParameterNames(GetParameterNames getParameterName, boolean learn) {

        GetParameterNamesResponse nameresp = new GetParameterNamesResponse();
        ParameterInfoList pil = new ParameterInfoList();
        String paramname = getParameterName.getParameterPath();

        if (paramname.equals("")) {
            paramname = cpeDBReader.props.getProperty("RootNode");
        }
        nameresp.setParameterList(pil);
        return inEnvelope(nameresp);
    }

    public Envelope doSetParameterValues(String sn, SetParameterValues setParameterValues) {
        return this.doSetParameterValues(sn, setParameterValues, true);
    }

    public Envelope doSetParameterValues(String sn, SetParameterValues setParameterValues, boolean learn) {
        ArrayList<ParameterValueStruct> nameList = setParameterValues.getParameterList().getParameterValueStruct();
        for (int i = 0; i < nameList.size(); i++) {
            ParameterValueStruct pvs = nameList.get(i);
            String paraName = pvs.getName();
            String paraValue = Optional.ofNullable(pvs.getValue()).orElseGet(() -> "");

            if (paraName.equals(InformConstants.SOFTWARECTRL_ACTIVATEENABLE) && paraValue.equals(InformConstants.SOFTWARECTRL_ACTIVATEENABLE_VALUE)) {
                processMsgPoolManagement.submit(new CpeActivateRunable(sn, InformConstants.SOFTWARECTRL_ACTIVATE_METHOD));
            } else if (paraName.contains(InformConstants.SOFTWARECTRL_ROLLBACK_ENABLE) && paraValue.equals(InformConstants.SOFTWARECTRL_ACTIVATEENABLE_VALUE)) {
                processMsgPoolManagement.submit(new CpeActivateRunable(sn, InformConstants.SOFTWARECTRL_ROLLBACK_METHOD));
            }

            cpeDBReader.setValue(sn, paraName, paraValue);
        }
        SetParameterValuesResponse valresp = new SetParameterValuesResponse();
        valresp.setStatus(SetParameterValuesResponse.Status._0);
        return inEnvelope(valresp);
    }

    public Envelope doGetParameterAttributes(GetParameterAttributes getParameterAttributes) {

//        String[] nameList = getParameterAttributes.getParameterNames().getStrings();
        String[] nameList = new String[1];
        GetParameterAttributesResponse valresp = new GetParameterAttributesResponse();
        ParameterAttributeList pal = new ParameterAttributeList();
//        for (int i = 0; i < nameList.length; i++) {
//            String paramname = nameList[i];
//            if (paramname.endsWith(".")) {
//                // System.out.println(" paramname ----> " + paramname);
//                HashMap valobj = this.cpeDBReader.confs;
//                Iterator it = valobj.entrySet().iterator();
//                while (it.hasNext()) {
//                    Map.Entry pairs = (Map.Entry) it.next();
//                    String keyname = (String) pairs.getKey();
//                    if (keyname.startsWith(paramname)) {
//                        Object obj = pairs.getValue();
//                        if (obj instanceof ConfParameter) {
//                            ConfParameter cp = (ConfParameter) obj;
//                            ParameterAttributeStruct pas = new ParameterAttributeStruct();
//                            pas.setName(cp.name);
//                            pas.setNotification(Notification.convert(cp.notification));
//                            AccessList accessList = new AccessList();
//                            accessList.setStrings(cp.accessList.split(","));
//                            pas.setAccessList(accessList);
//                            pal.getParameterAttributeStruct().add(pas);
//                            //System.out.println("Adding Nested --->>>  " + cp.name + " = " + cp.value);
//                        }
//                    }
//                }
//            } else if (this.cpeDBReader.confs.keySet().contains(paramname)) {
//                Object obj = this.cpeDBReader.confs.get(nameList[i]);
//                if (obj instanceof ConfParameter) {
//                    ConfParameter cp = (ConfParameter) obj;
//                    ParameterAttributeStruct pas = new ParameterAttributeStruct();
//                    pas.setName(cp.name);
//                    pas.setNotification(Notification.convert(cp.notification));
//                    AccessList accessList = new AccessList();
//                    accessList.setStrings(cp.accessList.split(","));
//                    pas.setAccessList(accessList);
//                    pal.getParameterAttributeStruct().add(pas);
//                    //System.out.println("Adding Direct --->>>  " + cp.name + " = " + cp.value);
//                }
//            }
//        }
        valresp.setParameterList(pal);
        return inEnvelope(valresp);

    }


    public Envelope doSetParameterAttributes(SetParameterAttributes setParameterAttributes) {
        ParameterValueList pvl = new ParameterValueList();
//        ArrayList<SetParameterAttributesStruct> nameList = setParameterAttributes.getParameterList().getSetParameterAttributesStruct();
        ArrayList<SetParameterAttributesStruct> nameList = new ArrayList<>();
        SetParameterValuesResponse valresp = new SetParameterValuesResponse();
//        for (int i = 0; i < nameList.size(); i++) {
//            SetParameterAttributesStruct spvs = nameList.get(i);
//            if (this.cpeDBReader.confs.keySet().contains(spvs.getName())) {
//                Object obj = this.cpeDBReader.confs.get(spvs.getName());
//                if (obj instanceof ConfParameter) {
//                    ConfParameter cp = (ConfParameter) obj;
//                    String[] aclist = spvs.getAccessList().getStrings();
//                    if (spvs.isNotificationChange()) {
//                        cp.notification = spvs.getNotification().toString();
//                    }
//                    if (spvs.isAccessListChange()) {
//                        cp.accessList = Arrays.toString(aclist);
//                    }
//                    // System.out.println("Setting Value --->>>  " + cp.notification + " = " + cp.accessList);
//                }
//            }
//        }
        valresp.setStatus(SetParameterValuesResponse.Status._0);
        return inEnvelope(valresp);
    }

    public Envelope doAddObject(AddObject addObject) {
        AddObjectResponse respobj = new AddObjectResponse();
        Random rn = new Random();
        respobj.setInstanceNumber(rn.nextInt(442422424) + 1);
        respobj.setStatus(AddObjectResponse.Status._0);
        return inEnvelope(respobj);
    }

    public Envelope doDeleteObject(DeleteObject deleteObject, String sn) {
        String paraPath = deleteObject.getObjectName();
        if (paraPath.length() > 0) {
            if (paraPath.endsWith(".")) {
                Set<String> pathSet = cpeDBReader.getSnToParaPathToValue().row(sn).keySet().stream().filter(it -> it.startsWith(paraPath)).collect(Collectors.toSet());
                pathSet.forEach(it ->
                        cpeDBReader.getSnToParaPathToValue().remove(sn, it)
                );
            } else {
                cpeDBReader.getSnToParaPathToValue().remove(sn, paraPath);
            }
        }

        DeleteObjectResponse respobj = new DeleteObjectResponse();
        respobj.setStatus(DeleteObjectResponse.Status._0);
        return inEnvelope(respobj);
    }

    public Envelope doReboot(Reboot reboot) {
        RebootResponse respobj = new RebootResponse();
        return inEnvelope(respobj);
    }

    public Envelope doDownload(Download download, String sn) {
        DownloadResponse respobj = new DownloadResponse();
        respobj.setStartTime(new Date());
        CPEDownloadRunable cds = new CPEDownloadRunable(download, downLoadPath, sn, downLoadVersion);
        cds.setDelayTime(delayTime);
        downloadPoolManagement.submit(cds);
        respobj.setStatus(DownloadResponse.Status._1);
        respobj.setCompleteTime(new Date());
        return inEnvelope(respobj);
    }

    public Envelope doUpload(Upload upload, String sn) {
        UploadResponse respobj = new UploadResponse();
        CpeUploadRunable cpeUploadRunable = new CpeUploadRunable(upload, uploadFileDir, sn);
        uploadFilePoolManagement.submit(cpeUploadRunable);
        respobj.setStartTime(new Date());
        respobj.setStatus(UploadResponse.Status._1);
        respobj.setCompleteTime(new Date());
        return inEnvelope(respobj);
    }

    public Envelope doFactoryReset(FactoryReset reboot) {
        FactoryResetResponse respobj = new FactoryResetResponse();
        return inEnvelope(respobj);
    }
	
	/*
	def doGetParameterAttributes( GetParameterAttributes getParameterAttributes ){		

		def nameList = getParameterAttributes.parameterNames.any
		def attrs = confdb.confs.keySet().findAll{ confkey ->  
				nameList.any{confkey.startsWith(it)} &&
				confdb.confs[confkey] instanceof ConfParameter &&
				confdb.confs[confkey].notification != null				
			}.collect{
				new ParameterAttributeStruct(
						name: it, 
						notification: Integer.parseInt(confdb.confs[it].notification),
						accessList: new AccessList(any: confdb.confs[it].accessList.split(","))  ) 
			}		
		return inEnvelope(new GetParameterAttributesResponse(parameterList: new ParameterAttributeList(any: attrs)))
	}

	def doSetParameterAttributes(SetParameterAttributes setParameterAttributes){
		// add error handling
		setParameterAttributes.parameterList.getAny().each{
			def conf = confdb.confs[it.name]			
			conf.accessList = it.accessList.getAny().join(",")
			conf.notification = it.notification.toString()
		}		
		return inEnvelope(new SetParameterValuesResponse(status: 0))
	}
	 */

    /*public static Envelope inEnvelope(Object cwmpObject) {
        Envelope envlope = new Envelope();
        Body body = new Body();
        ArrayList bodyobj = new ArrayList();
        bodyobj.add(cwmpObject);
        body.setObjects(bodyobj);
        envlope.setBody(body);
        return envlope;
    }
*/
    public static Envelope inEnvelope(Object cwmpObject) {
        Envelope envlope = new Envelope();
        Body body = new Body();
        Header header = new Header();
        ID id = new ID();
        id.setString(CommonUtil.getCwmpId());
        id.setMustUnderstand(true);
        // id.setString(headerID);
//                NoMoreRequests noMore = new NoMoreRequests();
//                noMore.setString("0");
        ArrayList headobj = new ArrayList();
        headobj.add(id);
//                headobj.add(noMore);
        header.setObjects(headobj);
        ArrayList bodyobj = new ArrayList();
        bodyobj.add(cwmpObject);
        body.setObjects(bodyobj);
        envlope.setHeader(header);
        envlope.setBody(body);
        return envlope;
    }

	/*def inEnvelope( cwmpObject, headerID ){		
		ID id = new ID(value: headerID, mustUnderstand: Boolean.TRUE )
		Header header = new Header(any: [ id ])
		Envelope envelope = new Envelope(body: new Body(any: [cwmpObject]), header: header);		
		return envelope
	}*/

    private boolean isMatching(String paramname, String keyname) {
        return this.isMatching(paramname, keyname, false);
    }

    private boolean isMatching(String paramname, String keyname, boolean isNextLevel) {
        return keyname.startsWith(paramname)
                && (!isNextLevel || depthOf(paramname) == depthOf(keyname) + 1);
    }

    private int depthOf(String param) {
        return param.split(".").length;
    }
}