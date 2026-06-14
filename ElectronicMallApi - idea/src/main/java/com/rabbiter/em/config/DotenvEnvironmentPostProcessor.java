package com.rabbiter.em.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 环境变量加载器 - 在 Spring Boot 启动时加载 .env 文件
 * 确保 application.yml 中的 ${XXX} 占位符能正确解析
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DotenvEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            // 尝试在项目根目录或父目录查找 .env 文件
            String userDir = System.getProperty("user.dir");
            File envFile = findEnvFile(userDir);
            
            if (envFile != null && envFile.exists()) {
                System.out.println("[DotenvEnvironmentPostProcessor] 找到 .env 文件: " + envFile.getAbsolutePath());
                Map<String, Object> envVars = loadEnvFile(envFile);
                MutablePropertySources propertySources = environment.getPropertySources();
                
                // 添加到最前面，优先级最高
                propertySources.addFirst(new MapPropertySource("dotenv", envVars));
                
                // 同时设置到 System 环境变量中
                envVars.forEach((key, value) -> {
                    if (System.getProperty(key) == null) {
                        System.setProperty(key, value.toString());
                    }
                });
                
                // 打印关键配置加载状态
                String apiKey = (String) envVars.get("DEEPSEEK_API_KEY");
                if (apiKey != null) {
                    System.out.println("[DotenvEnvironmentPostProcessor] ✅ DEEPSEEK_API_KEY 已加载: " + apiKey.substring(0, Math.min(10, apiKey.length())) + "...");
                } else {
                    System.out.println("[DotenvEnvironmentPostProcessor] ❌ DEEPSEEK_API_KEY 未在 .env 文件中找到");
                }
            } else {
                System.out.println("[DotenvEnvironmentPostProcessor] ❌ 未找到 .env 文件，当前工作目录: " + userDir);
            }
        } catch (Exception e) {
            System.out.println("[DotenvEnvironmentPostProcessor] ❌ 加载 .env 文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private File findEnvFile(String startDir) {
        // 先查找当前目录
        File current = new File(startDir, ".env");
        if (current.exists()) {
            return current;
        }
        // 查找父目录
        File parent = new File(startDir).getParentFile();
        if (parent != null) {
            File parentEnv = new File(parent, ".env");
            if (parentEnv.exists()) {
                return parentEnv;
            }
        }
        return null;
    }

    private Map<String, Object> loadEnvFile(File envFile) throws IOException {
        Map<String, Object> envVars = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // 跳过注释和空行
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                // 解析 KEY=VALUE
                int equalIndex = line.indexOf('=');
                if (equalIndex > 0) {
                    String key = line.substring(0, equalIndex).trim();
                    String value = line.substring(equalIndex + 1).trim();
                    // 去除引号
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    envVars.put(key, value);
                }
            }
        }
        return envVars;
    }
}
