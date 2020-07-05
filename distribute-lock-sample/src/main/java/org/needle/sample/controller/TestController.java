package org.needle.sample.controller;

import org.needle.lock.core.DistributeLock;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/connect")
    @DistributeLock(name = "first")
    public Object connect(){
        return "connect successful";
    }
}
