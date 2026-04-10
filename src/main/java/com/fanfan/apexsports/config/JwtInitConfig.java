package com.fanfan.apexsports.config;

import com.fanfan.apexsports.utils.JwtUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 初始化配置
 * <p>
 * 在 Spring 容器启动后自动初始化 {@link JwtUtil} 的签名密钥和过期时间，
 * 将配置属性注入静态工具类
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class JwtInitConfig {

    private final JwtProperties jwtProperties;

    /**
     * 容器启动后初始化 JwtUtil
     */
    @PostConstruct
    public void init() {
        JwtUtil.init(jwtProperties.getSecret(), jwtProperties.getExpiration());
    }
}
