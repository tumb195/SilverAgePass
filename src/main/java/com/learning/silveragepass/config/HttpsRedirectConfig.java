package com.learning.silveragepass.config;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * HTTPS Redirect Configuration
 * 
 * Configures automatic HTTP to HTTPS redirection when SSL is enabled.
 * This ensures all health data transmissions use encrypted connections.
 * 
 * Requirements: 12.2 - TLS 1.3 and HTTPS for API communications
 * 
 * Configuration:
 * - server.ssl.enabled=true: Enable SSL
 * - server.ssl.redirect-http-to-https=true: Enable HTTP to HTTPS redirect
 * - server.port=8080: HTTP port (will redirect to HTTPS)
 * - server.ssl.port=8443: HTTPS port (target for redirects)
 * 
 * @author SilverAgePass Team
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(name = "server.ssl.redirect-http-to-https", havingValue = "true")
public class HttpsRedirectConfig {

    @Value("${server.port:8080}")
    private int httpPort;

    @Value("${server.ssl.port:8443}")
    private int httpsPort;

    /**
     * Configures Tomcat to redirect HTTP requests to HTTPS.
     * 
     * This method:
     * 1. Creates an HTTP connector on the specified HTTP port
     * 2. Configures it to redirect to the HTTPS port
     * 3. Adds a security constraint to enforce HTTPS for all URLs
     * 
     * @return ServletWebServerFactory configured with HTTP to HTTPS redirect
     */
    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                // Add security constraint to require HTTPS for all requests
                SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL");
                
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                securityConstraint.addCollection(collection);
                
                context.addConstraint(securityConstraint);
            }
        };

        // Add HTTP connector that redirects to HTTPS
        tomcat.addAdditionalTomcatConnectors(createHttpConnector());
        
        return tomcat;
    }

    /**
     * Creates an HTTP connector that redirects to HTTPS.
     * 
     * @return Connector configured for HTTP to HTTPS redirect
     */
    private Connector createHttpConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setPort(httpPort);
        connector.setSecure(false);
        connector.setRedirectPort(httpsPort);
        
        return connector;
    }
}
