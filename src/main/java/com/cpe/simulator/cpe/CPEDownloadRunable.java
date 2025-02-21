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
import sun.security.x509.X500Name;

import java.io.FileInputStream;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Principal;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
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
		String fileName = null;
		if (execute.equals(InformConstants.EXECUTE_DOWNLOAD)) {
			if (InformConstants.FIRMWARE_UPGRADE_IMAGE_FILE.equals(fileType)) {
				fileName = sn + "_" + randomVersionName + ".zip";
			} else if (InformConstants.VENDOR_CONFIGURATION_FILE_DOWNLOAD.equals(fileType)){
				fileName = sn + "_" + randomVersionName + ".xml";
			} else if (InformConstants.VENDOR_CERTIFICATE_FILE_UPGRADE.equals(fileType)) {
				fileName = sn + "_" + CommonUtil.reportSinglaTraceFileFormatter.format(LocalDateTime.now()) + ".crt";
			} else {
				fileName = "test" + randomVersionName + ".zip";
			}

			String targetPath = downloadPath + fileName;
			RequestCallback requestCallback = request -> request.getHeaders();
			RestTemplate restTemplate = SpringUtil.getBean(RestTemplate.class);

			try {
				log.info(sn + ":" + URLDecoder.decode(download.getURL(), "UTF-8"));
				restTemplate.execute(URLDecoder.decode(download.getURL(), "UTF-8"), HttpMethod.GET, requestCallback, clientHttpResponse -> {
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
		if (downLoadResult) {
			if (CommonUtil.strHasLength(fileType) && fileType.contains(InformConstants.FIRMWARE_UPGRADE_IMAGE_FILE)) {
				updateCpeSystemBackUpVersion(download.getURL(), sn);
				updateMuDownloadStatus();
			} else if (InformConstants.VENDOR_CERTIFICATE_FILE_UPGRADE.equals(fileType) && downLoadResult) {
				updateGnodebCertInfo(downloadPath + fileName, sn);
			}
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
			try {
				reentrantLock.unlock();
			} catch (Exception e) {
				log.error("unlock failed...", e);
			}
		}
	}

	private void updateGnodebCertInfo(String filePath, String sn) {
		try {
			FileInputStream fis = new FileInputStream(filePath);
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
			X509Certificate x509Certificate = (X509Certificate) certificateFactory.generateCertificate(fis);

			CpeDBReader cpeDBReader = SpringUtil.getBean(CpeDBReader.class);
			Principal subjectDN = x509Certificate.getSubjectDN();
			if (subjectDN instanceof X500Name) {
				X500Name subjectDnX500Name = (X500Name) subjectDN;
				cpeDBReader.setValue(sn, "Device.Security.Certificate.1.Subject", subjectDnX500Name.getCommonName());
			} else {
				cpeDBReader.setValue(sn, "Device.Security.Certificate.1.Subject", subjectDN.getName());
			}

			Principal issuerDN = x509Certificate.getIssuerDN();
			if (issuerDN instanceof X500Name) {
				X500Name issuerDnX500Name = (X500Name) issuerDN;
				cpeDBReader.setValue(sn, "Device.Security.Certificate.1.Issuer", issuerDnX500Name.getCommonName());
			} else {
				cpeDBReader.setValue(sn, "Device.Security.Certificate.1.Issuer", issuerDN.getName());
			}

			cpeDBReader.setValue(sn, "Device.Security.Certificate.1.NotBefore", CommonUtil.formatDate(x509Certificate.getNotBefore()));
			cpeDBReader.setValue(sn, "Device.Security.Certificate.1.NotAfter", CommonUtil.formatDate(x509Certificate.getNotAfter()));

		} catch (Exception e) {
			log.error(e.getMessage(), e);
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
