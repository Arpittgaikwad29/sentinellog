package com.sentinellog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SentinelLogApplication {

    public static void main(String[] args) {
        SpringApplication.run(SentinelLogApplication.class, args);
        System.out.println("""
                \s
                ███████╗███████╗███╗   ██╗████████╗██╗███╗   ██╗███████╗██╗      ██████╗  ██████╗
                ██╔════╝██╔════╝████╗  ██║╚══██╔══╝██║████╗  ██║██╔════╝██║     ██╔═══██╗██╔════╝
                ███████╗█████╗  ██╔██╗ ██║   ██║   ██║██╔██╗ ██║█████╗  ██║     ██║   ██║██║  ███╗
                ╚════██║██╔══╝  ██║╚██╗██║   ██║   ██║██║╚██╗██║██╔══╝  ██║     ██║   ██║██║   ██║
                ███████║███████╗██║ ╚████║   ██║   ██║██║ ╚████║███████╗███████╗╚██████╔╝╚██████╔╝
                ╚══════╝╚══════╝╚═╝  ╚═══╝   ╚═╝   ╚═╝╚═╝  ╚═══╝╚══════╝╚══════╝ ╚═════╝  ╚═════╝
                \s
                  Real-time log anomaly detection  →  http://localhost:8080
                \s
                """);
    }
}
