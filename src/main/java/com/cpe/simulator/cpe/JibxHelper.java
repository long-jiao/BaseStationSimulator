package com.cpe.simulator.cpe;

import lombok.extern.slf4j.Slf4j;
import org.jibx.runtime.*;

import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

@Slf4j
public class JibxHelper {
	
	public final static String STRING_ENCODING = "UTF8";
    public final static String URL_ENCODING = "UTF-8";
	
	public static String marshalObject(Object message, String system)	{
		String packageName = "org.dslforum." + system;
		String bindingName = "binding";

		try {
			IBindingFactory jc = BindingDirectory.getFactory(bindingName, packageName);
			IMarshallingContext marshaller = jc.createMarshallingContext();
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			marshaller.marshalDocument(message, URL_ENCODING, null, out);
			String xml = out.toString(STRING_ENCODING);
			return xml;
		} catch (UnsupportedEncodingException e) {
			log.error("", e);
		} catch (JiBXException e) {
			log.error("", e);
		}
		return null;
	}
	/**
	 * Unmarshal this xml Message to an object.
	 * @param xml
	 * @param version
	 * @return
	 */
	public static Object unmarshalMessage(String xml, String version)
	{
		String packageName = "org.dslforum." + version;
		String bindingName = "binding";
		xml = convertCwmpVersion(xml, "cwmp-1-1", "cwmp-1-0");

		try {
			IBindingFactory jc = BindingDirectory.getFactory(bindingName, packageName);
			IUnmarshallingContext unmarshaller = jc.createUnmarshallingContext();
			Reader inStream = new StringReader(xml);
			Object message = unmarshaller.unmarshalDocument( inStream, bindingName);
			return message;
		} catch (JiBXException e) {
			log.error("messgae from Omc:" + xml);
			log.error("", e);
		}
		return null;
	}
	
	public static Object unmarshalMessage(Class className, String xml, String version) {
		String packageName = "org.dslforum." + version;
		String bindingName = "binding";
		try {
			IBindingFactory jc = BindingDirectory.getFactory(bindingName, className);
			IUnmarshallingContext unmarshaller = jc.createUnmarshallingContext();
			Reader inStream = new StringReader(xml);
			Object message = unmarshaller.unmarshalDocument( inStream, bindingName);
			return message;
		} catch (JiBXException e) {
			log.error("", e);
		}
		return null;
	}

	public static String convertCwmpVersion(String xml, String sourceVersion, String targetVersion) {
		return xml.replace(sourceVersion, targetVersion);
	}


	public static void main(String[] args) {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ZSI=\"http://www.zolera.com/schemas/ZSI/\" xmlns:cwmp=\"urn:dslforum-org:cwmp-1-0\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><SOAP-ENV:Header><cwmp:ID>592593212</cwmp:ID><cwmp:HoldRequests>0</cwmp:HoldRequests></SOAP-ENV:Header><SOAP-ENV:Body><cwmp:GetParameterValues><ParameterNames><cwmp:string xsi:type=\"xsd:string\">Device.</cwmp:string></ParameterNames></cwmp:GetParameterValues></SOAP-ENV:Body></SOAP-ENV:Envelope>";
		String xml2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><SOAP-ENV:Envelope xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ZSI=\"http://www.zolera.com/schemas/ZSI/\" xmlns:cwmp=\"urn:dslforum-org:cwmp-1-0\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><SOAP-ENV:Header><cwmp:ID>592593212</cwmp:ID><cwmp:HoldRequests>0</cwmp:HoldRequests></SOAP-ENV:Header><SOAP-ENV:Body><cwmp:GetParameterValues><ParameterNames>Device.</ParameterNames></cwmp:GetParameterValues></SOAP-ENV:Body></SOAP-ENV:Envelope>";
		String version = "cwmp_1_0";
		Object result = unmarshalMessage(xml, version);
		System.out.println(result);


	}
}
