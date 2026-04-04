package com.hospitalinfo.hospitalinformationsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.hospitalinfo.hospitalinformationsystem.mapper")
public class HospitalInformationSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(HospitalInformationSystemApplication.class, args);
    }

}
