package org.onextel.db2_pick_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement

public class Db2PickAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(Db2PickAppApplication.class, args);
    }
}
