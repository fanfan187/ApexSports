package com.fanfan.apexsports.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 * <p>
 * 从配置文件 {@code apex.jwt} 前缀下读取签名密钥和 Token 有效期
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "apex.jwt")
public class JwtProperties {

    /** 签名密钥 */
    private String secret;

    /** Token 有效期（毫秒），默认 2 小时 */
    private long expiration = 7200000L;
}
