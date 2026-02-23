package com.thomassabu.transactionmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Transaction Monitor Service.
 */
@SpringBootApplication
public class TransactionMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionMonitorApplication.class, args);
    }
}
