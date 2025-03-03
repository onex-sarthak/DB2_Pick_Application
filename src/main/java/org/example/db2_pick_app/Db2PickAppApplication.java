package org.example.db2_pick_app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement

public class Db2PickAppApplication {

    public static Logger logger = LoggerFactory.getLogger(Db2PickAppApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(Db2PickAppApplication.class, args);


        logger.info("info message");
        logger.warn("warn message");
        logger.error("error message");


    }


}
