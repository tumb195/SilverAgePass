package com.learning.silveragepass.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tomcat 配置类
 * 配置 Tomcat 以允许 URL 中的特殊字符（如方括号和点）
 * 
 * 注意：虽然这可以解决 URL 解析错误，但最佳实践是在客户端正确编码 URL 参数
 * 此配置仅作为防御性措施
 */
@Configuration
public class TomcatConfig {

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addConnectorCustomizers(connector -> {
            // 允许 URL 中的特殊字符（方括号、点等）
            // 这些字符在 RFC 7230 和 RFC 3986 中通常需要编码
            // 注意：点(.)在查询参数中通常是允许的，但方括号需要明确允许
            connector.setProperty("relaxedQueryChars", "[]");
            connector.setProperty("relaxedPathChars", "[]");
        });
        return factory;
    }
}
