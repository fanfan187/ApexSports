package com.fanfan.apexsports;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ApexSports 学生运动会管理系统启动类
 *
 * @author fanfan187
 */
@SpringBootApplication
@MapperScan("com.fanfan.apexsports.mappers")
public class ApexSportsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApexSportsApplication.class, args);
    }

}
