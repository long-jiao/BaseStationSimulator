package com.cpe.simulator.bean;

import lombok.Data;

import java.util.Map;

@Data
public class EventCodeProcessInfo {

    private String eventCode;

    private String sn;

    private Map<String, Object> parameterPathToValue;
}
