package com.cpe.simulator.cpe;


import com.cpe.simulator.message.ConcurrentManagement;
import com.cpe.simulator.util.CommonConstans;
import com.cpe.simulator.util.CommonUtil;
import com.cpe.simulator.util.InformConstants;
import com.cpe.simulator.util.SpringUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.dslforum.cwmp_1_0.*;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Data
@Slf4j
public class CPEDownloadRunable implements Runnable {

	private Download download;
	private String downloadPath;

	private String sn;
	private String execute;

	private int delayTime = 30;

	public CPEDownloadRunable(Download download, String downloadPath, String sn, String execute) {
		this.download = download;
		this.downloadPath = downloadPath;
		this.sn = sn;
		this.execute = execute;
	}

	@Override
	public void run() {
		LocalDateTime startTime = LocalDateTime.now();
//		log.info(sn + " begin download,time:" + startTime);
		String randomVersionName = String.valueOf(new Random().nextInt());
		String fileType = download.getFileType();
		log.error("fileType is:" +fileType);
		boolean downLoadResult = true;
		if (execute.equals(InformConstants.EXECUTE_DOWNLOAD)) {
			String fileName = null;
			if (InformConstants.FIRMWARE_UPGRADE_IMAGE_FILE.equals(fileType)) {
				fileName = sn + "_" + randomVersionName + ".zip";
			} else if (InformConstants.VENDOR_CONFIGURATION_FILE_DOWNLOAD.equals(fileType)){
				fileName = sn + "_" + randomVersionName + ".xml";
			} else {
				fileName = "test" + randomVersionName + ".zip";
			}

			String targetPath = downloadPath + fileName;
			RequestCallback requestCallback = request -> request.getHeaders();
			RestTemplate restTemplate = SpringUtil.getBean(RestTemplate.class);
			log.info(sn + ":" + download.getURL());

			try {
				restTemplate.execute(download.getURL(), HttpMethod.GET, requestCallback, clientHttpResponse -> {
					Files.copy(clientHttpResponse.getBody(), Paths.get(targetPath));
					return null;
				});
				TimeUnit.SECONDS.sleep(2);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				downLoadResult = false;
			}
		} else {
			try {
				TimeUnit.SECONDS.sleep(delayTime);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		LocalDateTime endTime = LocalDateTime.now();
		log.info(sn + " download success" + ", cost time(second):" + Duration.between(startTime, endTime).getSeconds());
		if (CommonUtil.strHasLength(fileType) && fileType.contains(InformConstants.FIRMWARE_UPGRADE_IMAGE_FILE) && downLoadResult) {
			updateCpeSystemBackUpVersion(download.getURL(), sn);
			updateMuDownloadStatus();
		}

		ArrayList<EventStruct> eventKeyList = new ArrayList<>();
		EventStruct eventStruct = new EventStruct();
		eventStruct.setEventCode(EventStructConstants.EVENT_TRANSFER_COMPLETE);
		eventKeyList.add(eventStruct);
		EventStruct mDownLoadEvent = new EventStruct();
		mDownLoadEvent.setEventCode(EventStructConstants.EVENT_M_DOWNLOAD_TRANSFER_COMPLETE);
		mDownLoadEvent.setCommandKey(download.getCommandKey());
		eventKeyList.add(mDownLoadEvent);
		CpeActionsService cpeActionsService = SpringUtil.getBean(CpeActionsService.class);

		Envelope informMessage = cpeActionsService.doInform(eventKeyList, sn);
		CPEClientSession cpeClientSession = SpringUtil.getBean(CPEClientSession.class);

		ReentrantLock reentrantLock = ConcurrentManagement.BASE_STATION_SEND_DATA_LOCK.get(sn);
		try {
			reentrantLock.tryLock(CommonConstans.tryLockWaitTime, TimeUnit.MINUTES);

			ACSResponse acsResponse = cpeClientSession.sendDownLoadCompleteInform(informMessage, sn);
			TransferComplete transferComplete = new TransferComplete();
			transferComplete.setCommandKey(download.getCommandKey());
			FaultStruct faultStruct = new FaultStruct();
			if (downLoadResult) {
				faultStruct.setFaultCode(FaultStruct.FaultCode._0);
			} else {
				faultStruct.setFaultCode(FaultStruct.FaultCode._9010);
			}
			faultStruct.setFaultString("");
			transferComplete.setFaultStruct(faultStruct);
			transferComplete.setStartTime(new Date());
			transferComplete.setCompleteTime(new Date());
			Envelope envelope = CpeActionsService.inEnvelope(transferComplete);
			cpeClientSession.sendTransferCompleteInform(envelope, acsResponse, sn);
		} catch (Exception e) {
			log.error(sn + " send download complete inform get lock failed...", e);
		} finally {
			reentrantLock.unlock();
		}
	}

	private void updateCpeSystemBackUpVersion(String url, String sn) {
//		String downLoadUrl = "http://172.16.1.194:8214/api/httpfs/v1/download/softVer/BS5514_V1.30.30B1_mmw_e026af.zip";
		String versionPackageName = url.substring(url.lastIndexOf("/") + 1);
		String versionName = versionPackageName.replace(".zip", "");
		CpeDBReader cpeDBReader = SpringUtil.getBean(CpeDBReader.class);
		cpeDBReader.setValue(sn, "Device.SoftwareCtrl.SystemBackupVersion", versionName);
	}

	private void updateMuDownloadStatus() {
		CpeDBReader cpeDBReader = SpringUtil.getBean(CpeDBReader.class);
		cpeDBReader.setValue(sn, InformConstants.MU_DOWNLOAD_STATUS, InformConstants.MU_DOWNLOAD_STATUS_FINISH);
	}
}
