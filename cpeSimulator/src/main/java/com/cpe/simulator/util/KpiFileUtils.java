package com.cpe.simulator.util;

import com.google.common.collect.Multimap;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.*;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.GZIPOutputStream;

@Slf4j
public class KpiFileUtils {

    private static String prefix = System.getProperty("java.io.tmpdir");
    private static DateTimeFormatter decodeformatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd.HHmmZ");
    private static String oui = "D837BE";
    private static Random rs = new Random();

    public static String generateXml(Multimap<String, String> objectToCounters, String gnbId, Set<String> cellIdSet, Map<String, String> dateMap, String serialNum) {
        Document document = DocumentHelper.createDocument();
        document.addProcessingInstruction("xml-stylesheet", "type=text/xsl href=foo.xsl");
        Element root = document.addElement("measCollecFile", "http://www.3gpp.org/ftp/specs/archive/32_series/32.435#measCollec");
        root.addNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        root.addAttribute("xsi:schemaLocation", "http://www.3gpp.org/ftp/specs/archive/32_series/32.435#measCollec http://www.3gpp.org/ftp/specs/archive/32_series/32.435#measCollec");
        Element fileHeader = root.addElement("fileHeader");
        fileHeader.addAttribute("fileFormatVersion", "32.435 V7");
        fileHeader.addElement("fileSender");
        Element measCollec = fileHeader.addElement("measCollec");
        String startTime = dateMap.get("startTime");
        measCollec.addAttribute("beginTime", startTime);
        Element measData = root.addElement("measData");
        measData.addElement("managedElement");
        Element measInfo = measData.addElement("measInfo");
        Element granPeriod = measInfo.addElement("granPeriod");
        granPeriod.addAttribute("duration", "PT900S");
        granPeriod.addAttribute("endTime", dateMap.get("endTime"));
        Element repPeriod = measInfo.addElement("repPeriod");
        repPeriod.addAttribute("duration", "PT900S");
        int i = 1;
        Map<String, String> counterToLocation = new HashMap<String, String>();
        for (String counterIndex : objectToCounters.values()) {
            Element measType = measInfo.addElement("measType");
            measType.addAttribute("p", String.valueOf(i));
            counterToLocation.put(counterIndex, String.valueOf(i));
            ++i;
            measType.setText(counterIndex);
        }
        StringBuilder sb = new StringBuilder("gNBID=").append(gnbId);
        for (String obj : objectToCounters.keySet()) {
            List<String> counters = (List<String>) objectToCounters.get(obj);
            if (obj.contains("gNB")) {
                Element measValue = measInfo.addElement("measValue");
                measValue.addAttribute("measObjLdn", sb.toString());
                addValue(measValue, counters, counterToLocation);
            } else {
                for (String j : cellIdSet) {
                    Element measValue2 = measInfo.addElement("measValue");
                    if (obj.contains("CU")) {
                        measValue2.addAttribute("measObjLdn", sb + ",CUCellIdentity=" + j);
                        addValue(measValue2, counters, counterToLocation);
                    } else {
                        measValue2.addAttribute("measObjLdn", sb + ",DUCellIdentity=" + j);
                        addValue(measValue2, counters, counterToLocation);
                    }
                }
            }
        }
        OutputFormat format = OutputFormat.createPrettyPrint();
        String filePath = getFilePath(startTime, serialNum);
        try {
            XMLWriter writer = new XMLWriter(new FileOutputStream(filePath), format);
            writer.write(document);
            writer.flush();
            writer.close();
            log.info("generate pm file,filePath:" + filePath);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return filePath;
    }

    private static String getFilePath(String startTime, String serialNum) {
        ZonedDateTime time = ZonedDateTime.parse(startTime, decodeformatter);
        ZonedDateTime time2 = time.plusMinutes(15L);
        String strTime2 = time2.format(formatter).split("\\.")[1];
        StringBuilder filePath = new StringBuilder();
        filePath.append(prefix);
        if (!prefix.endsWith(File.separator)) {
            filePath.append(File.separator);
        }
        filePath.append("A").append(time.format(formatter)).append("-").append(strTime2).append("_").append(oui).append(".");
        filePath.append(serialNum).append(".xml");
        return filePath.toString();
    }

    private static void addValue(Element element, List<String> counters, Map<String, String> counterIndexToLocation) {
        List<String> locationList = new ArrayList<String>();
        for (String item : counters) {
            locationList.add(counterIndexToLocation.get(item));
        }
        locationList.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return Integer.valueOf(o1).compareTo(Integer.valueOf(o2));
            }
        });
        for (String num : locationList) {
            Element r = element.addElement("r");
            r.addAttribute("p", num);
            String value = String.valueOf(rs.nextInt(10));
//            if (value.equals("5")) {
//                r.setText("");
//            } else {
            r.setText(value);
//            }
        }
    }

    public static String fileToGzFile(String filePath) {
        InputStream in = null;
        OutputStream out = null;
        String outFile = filePath + ".gz";
        try {
            out = new GZIPOutputStream(new FileOutputStream(outFile));
            in = new FileInputStream(filePath);
            byte[] buf = new byte[1024];
            int len = 0;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.flush();
            in.close();
            out.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            try {
                out.close();
                in.close();
            } catch (IOException e2) {
                log.error(e2.getMessage(), e2);

            }
        } finally {
            try {
                out.close();
                in.close();
            } catch (IOException e3) {
                log.error(e3.getMessage(), e3);
            }
        }

        return outFile;
    }

}
