package com.cpe.simulator.cpe;

import com.cpe.simulator.util.CommonUtil;
import com.cpe.simulator.util.InformConstants;
import com.cpe.simulator.util.SpringUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dslforum.cwmp_1_0.Upload;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.time.LocalDateTime;

@Data
@Slf4j
public class CpeUploadRunable implements Runnable {

    private Upload upload;

    private String uploadFileDir;

    private String sn;

    public CpeUploadRunable(Upload upload, String uploadFileDir, String sn) {
        this.upload = upload;
        this.uploadFileDir = uploadFileDir;
        this.sn = sn;
    }

    // url: http://172.172.16.254:18088/api/httpfs/v1/upload/dataFile/uploadedData/902230500174/wg_902230500174_20230411101159.cfg
    @Override
    public void run() {
        String fileType = upload.getFileType();
        UploadFileService uploadFileService = SpringUtil.getBean(UploadFileService.class);
        String url = upload.getURL();
        log.info("upload file type is:" + fileType + ",url:" + upload.getURL());
        CpeDBReader cpeDBReader = SpringUtil.getBean(CpeDBReader.class);
        String ouiValue = cpeDBReader.getValue(sn, InformConstants.MU_MANUFACTUREROUI);
        fileType = fileType.replace(ouiValue, "%s");
        String sourceFilePath = null;
        if (InformConstants.VENDOR_CONFIGURATION_FILE_UPLOAD.equals(fileType)) {
            sourceFilePath = uploadFileDir + File.separator + "config.cfg";
        } else if (InformConstants.FILE_TYPE_WIFI_LOG_UPLOAD.equals(fileType) || InformConstants.FILE_TYPE_DEVICE_LOG_UPLOAD.equals(fileType) ||
                InformConstants.FILE_TYPE_CHR_LOG_UPLOAD.equals(fileType) || InformConstants.FILE_TYPE_BLACKBOX_LOG_UPLOAD.equals(fileType)) {
            sourceFilePath = uploadFileDir + File.separator + "baseStation.log";
        }

        String timeStamp = LocalDateTime.now().toString();
        if (InformConstants.FILE_TYPE_WIFI_LOG_UPLOAD.equals(fileType)) {
            url = url + "Wifi_" + timeStamp + ".log";
        } else if (InformConstants.FILE_TYPE_DEVICE_LOG_UPLOAD.equals(fileType)) {
            url = url + "DeviceLog_" + timeStamp + ".log";
        } else if (InformConstants.FILE_TYPE_CHR_LOG_UPLOAD.equals(fileType)) {
            url = url + "Chr_" + timeStamp + ".log";
        } else if (InformConstants.FILE_TYPE_BLACKBOX_LOG_UPLOAD.equals(fileType)) {
            url = url + "BlackBox_" + timeStamp + ".log";
        }

        if (!CommonUtil.strHasLength(sourceFilePath)) {
            log.error("The tool not support the file type:" + fileType);
            return;
        }

        int resultCode = uploadFileService.uploadFile(url, sourceFilePath);
        boolean uploadResult = resultCode == HttpStatus.CREATED.value() || resultCode == HttpStatus.OK.value();
        if (uploadResult) {
            log.info("upload file success, url:" + url);
        } else {
            log.error("upload failed......" + url);
        }
        uploadFileService.sendUploadInform(upload.getCommandKey(), sn, uploadResult);
    }
}
