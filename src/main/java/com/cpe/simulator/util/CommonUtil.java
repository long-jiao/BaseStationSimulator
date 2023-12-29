package com.cpe.simulator.util;


import java.io.*;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CommonUtil {

    public static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static DateTimeFormatter alarmEventFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static DateTimeFormatter reportSinglaTraceFileFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");


    public static String getCurrentTime() {
        ZonedDateTime now = ZonedDateTime.now();
        return now.format(formatter) + now.getOffset();
    }

    public static String formatTriggerEventTime(LocalDateTime dateTime) {
        return dateTime.format(alarmEventFormatter);
    }

    public static String getCwmpId() {
        int hashCode = UUID.randomUUID().hashCode();
        if (hashCode < 0) {
            hashCode = -hashCode;
        }
        String cwmpIdSrc = String.valueOf(hashCode);
        int subLength = cwmpIdSrc.length() < 9 ? cwmpIdSrc.length() : 9;
        return cwmpIdSrc.substring(0, subLength);
    }

    public static <T extends Serializable> T deepClone(T srcObject){
        T resultObject = null;
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(srcObject);
            objectOutputStream.close();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            resultObject = (T) objectInputStream.readObject();
            objectInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultObject;
    }

    public static boolean strHasLength(String sourceStr) {
        if (Objects.isNull(sourceStr) || sourceStr.length() == 0) {
            return false;
        }
        return true;
    }

    public static String formatFileDataTime(LocalDateTime time) {
        return reportSinglaTraceFileFormatter.format(time);
    }





    public static void main(String[] args) {
        System.out.println(getCurrentTime());

        System.out.println(formatTriggerEventTime(LocalDateTime.now()));
    }



}
