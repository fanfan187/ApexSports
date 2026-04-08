package com.fanfan.apexsports;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ApexSports 学生运动会管理系统启动类
 * <p>
 * Spring Boot 应用程序入口，配置了 MyBatis Mapper 扫描路径
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@SpringBootApplication
@MapperScan("com.fanfan.apexsports.mappers")
public class ApexSportsApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApexSportsApplication.class, args);
    }

}
