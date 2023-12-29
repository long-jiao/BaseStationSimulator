package com.cpe.simulator.bean;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("DeviceInfo")
public class DeviceInfo {

    private Long id;
    private String manufacturer;
    private String oui;
    private String productClass;

    @TableField(exist = false)
    private String sn;
}
