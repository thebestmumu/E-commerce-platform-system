package com.rabbiter.em;

import com.rabbiter.em.utils.PathUtils;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@MapperScan("com.rabbiter.em.mapper")
@SpringBootApplication
public class ElectronicMallApplication {

    public static void main(String[] args) {
        log.info("项目启动，路径：{}", PathUtils.getClassLoadRootPath());
        SpringApplication.run(ElectronicMallApplication.class, args);
    }

}
