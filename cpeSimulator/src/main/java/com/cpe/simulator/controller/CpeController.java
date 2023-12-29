package com.cpe.simulator.controller;


import com.cpe.simulator.cpe.ConnectionRequestRunable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@Slf4j
public class CpeController {

    @Resource
    private ThreadPoolTaskExecutor processMsgPoolManagement;

    @GetMapping("/{sn}")
    public String weakUpCpe(@PathVariable("sn") String sn, HttpServletRequest request) {
        log.info("weak up sn is: " + sn);
        processMsgPoolManagement.submit(new ConnectionRequestRunable(sn));
        return sn;
    }
}
