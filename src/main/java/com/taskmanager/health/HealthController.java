package com.taskmanager.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/keep-alive")
    public String keepAlive() {
        // 只是為了讓伺服器知道有人在連線，回傳簡單字串即可
        return "I am alive!";
    }
}
