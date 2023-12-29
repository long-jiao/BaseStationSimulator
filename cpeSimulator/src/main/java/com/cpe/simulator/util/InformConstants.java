package com.cpe.simulator.util;

import java.util.ArrayList;
import java.util.List;

public class InformConstants {
    /**
     * rootNode=Device.
     * rootDataModelVersion=Device.RootDataModelVersion
     * muHardwareVersion=Device.DeviceInfo.MU.1.HardwareVersion
     * muSoftwareVersion=Device.DeviceInfo.MU.1.SoftwareVersion
     * provisioningCode=Device.DeviceInfo.MU.1.ProvisioningCode
     * parameterKey=Device.ManagementServer.ParameterKey
     * connectionRequestURL=Device.ManagementServer.ConnectionRequestURL
     */

    public static final String ROOT_NODE = "Device.";
    public static final String ROOT_DATAMODEL_VERSION = "Device.RootDataModelVersion";
    public static final String MU_HARDWARE_VERSION = "Device.DeviceInfo.MU.1.HardwareVersion";
    public static final String MU_SOFTWARE_VERSION = "Device.DeviceInfo.MU.1.SoftwareVersion";
    public static final String PROVISIONING_CODE = "Device.DeviceInfo.MU.1.ProvisioningCode";
    public static final String PARAMETER_KEY = "Device.ManagementServer.ParameterKey";
    public static final String CONNECTION_REQUEST_URL = "Device.ManagementServer.ConnectionRequestURL";

    public static final String MU_SERIALNUMBER = "Device.DeviceInfo.MU.1.SerialNumber";

    public static final String MU_MANUFACTURER = "Device.DeviceInfo.MU.1.Manufacturer";
    public static final String MU_MANUFACTUREROUI = "Device.DeviceInfo.MU.1.ManufacturerOUI";
    public static final String MU_PRODUCTCLASS = "Device.DeviceInfo.MU.1.ProductClass";


    //4：等待激活 5：激活中
    public static final String MU_ACTIVATE_STATUS = "Device.DeviceInfo.MU.1.SwUpgrade.Stage";

    public static final String MU_ACTIVATE_STATUS_WAITE_ACTIVATE = "4";
    public static final String MU_ACTIVATE_STATUS_ACTIVATING = "5";

    public static final String MU_DOWNLOAD_STATUS = "Device.DeviceInfo.MU.1.X_7C8334_DownloadStatus";
    public static final String MU_DOWNLOAD_STATUS_FINISH = "1";
    public static final String MU_DOWNLOAD_STATUS_CLEAR = "0";


    public static final String SOFTWARECTRL_ACTIVATEENABLE = "Device.SoftwareCtrl.ActivateEnable";
    public static final String SOFTWARECTRL_ACTIVATEENABLE_VALUE = "1";

    public static final String SOFTWARECTRL_ACTIVATE_IN_PROGRESS = "activate_2";
    public static final String SOFTWARECTRL_ACTIVATE_COMPLETE = "activate_100";

    public static final String SOFTWARE_BACKUP_VERSION = "Device.SoftwareCtrl.SystemCurrentVersion";
    public static final String SOFTWARE_CURRENT_VERSION = "Device.SoftwareCtrl.SystemBackupVersion";


    public static final String EXECUTE_DOWNLOAD = "1";
    public static final String NOT_EXECUTE_DOWNLOAD = "0";

    public static final String GNBID_PATH = "Device.X_7C8334_gNodeBFunction.1.gNodeBCommon.gNBId";
    public static final String CELLID_PATH_SUFFIX = ".CellConfig.NR.RAN.RF.CellId";
    public static final String CELLID_PATH_PREFIX = "Device.Services.X_7C8334_CUFAPService.";

    public static final String PERFMGMT_ENABLE_PATH = "Device.FAP.PerfMgmt.Config.1.Enable";
    public static final String PERFMGMT_URL_PATH = "Device.FAP.PerfMgmt.Config.1.URL";

    public static final String RRD_DATA_SWITCH = "Device.X_7C8334_StateRpt.Enable";


    public static final String AUTO_NOMOUS_TRANSFERCOMPLETE_FILE_TYPE = "X 7C8334 PerformaceReport";

    public static final String VENDOR_CONFIGURATION_FILE_UPLOAD = "1 Vendor Configuration File";
    public static final String VENDOR_CONFIGURATION_FILE_DOWNLOAD = "3 Vendor Configuration File";
    public static final String FIRMWARE_UPGRADE_IMAGE_FILE = "1 Firmware Upgrade Image";

    public static final String FILE_TYPE_WIFI_LOG_UPLOAD = "X %s WifiLog";
    public static final String FILE_TYPE_CHR_LOG_UPLOAD = "X %s CHRLog";
    public static final String FILE_TYPE_BLACKBOX_LOG_UPLOAD = "X %s BlackBox";
    public static final String FILE_TYPE_DEVICE_LOG_UPLOAD = "X %s DeviceLog";


    public static final List<String> ALARM_IDENTIFIER_LIST = new ArrayList<>();

    static {
        ALARM_IDENTIFIER_LIST.add("0x00000001");
        ALARM_IDENTIFIER_LIST.add("0x00000002");
        ALARM_IDENTIFIER_LIST.add("0x00000003");
        ALARM_IDENTIFIER_LIST.add("0x00000004");
        ALARM_IDENTIFIER_LIST.add("0x00000005");
        ALARM_IDENTIFIER_LIST.add("0x00000006");
        ALARM_IDENTIFIER_LIST.add("0x00000007");
        ALARM_IDENTIFIER_LIST.add("0x00000008");
        ALARM_IDENTIFIER_LIST.add("0x00000009");
        ALARM_IDENTIFIER_LIST.add("0x0000000A");
        ALARM_IDENTIFIER_LIST.add("0x0000000B");
        ALARM_IDENTIFIER_LIST.add("0x0000000C");
        ALARM_IDENTIFIER_LIST.add("0x0000000D");
        ALARM_IDENTIFIER_LIST.add("0x0000000E");
        ALARM_IDENTIFIER_LIST.add("0x0000000F");
        ALARM_IDENTIFIER_LIST.add("0x00000010");
        ALARM_IDENTIFIER_LIST.add("0x00000011");
        ALARM_IDENTIFIER_LIST.add("0x00000012");
        ALARM_IDENTIFIER_LIST.add("0x00000013");
        ALARM_IDENTIFIER_LIST.add("0x00000014");
        ALARM_IDENTIFIER_LIST.add("0x00000015");
        ALARM_IDENTIFIER_LIST.add("0x00000016");
        ALARM_IDENTIFIER_LIST.add("0x00000017");
        ALARM_IDENTIFIER_LIST.add("0x00000018");
        ALARM_IDENTIFIER_LIST.add("0x0000001D");
        ALARM_IDENTIFIER_LIST.add("0x0000001E");
        ALARM_IDENTIFIER_LIST.add("0x50010501");
        ALARM_IDENTIFIER_LIST.add("0x50010502");
    }

    public static final String DEVICE_OUI = "7C8334";

    public static final String ALARM_NOTIFY_TYPE_CREATE = "0";
    public static final String ALARM_NOTIFY_TYPE_CLEAR = "2";


}
