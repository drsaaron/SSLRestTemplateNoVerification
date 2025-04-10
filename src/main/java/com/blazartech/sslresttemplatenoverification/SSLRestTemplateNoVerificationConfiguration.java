/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.blazartech.sslresttemplatenoverification;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author scott
 */
@Configuration
public class SSLRestTemplateNoVerificationConfiguration {

    public ClientHttpRequestFactory requestFactory() {
        try {
            // don't validate SSL cert on https call. 
            // see https://stackoverflow.com/questions/4072585/disabling-ssl-certificate-validation-in-spring-resttemplate
            TrustStrategy acceptingTrustStrategy = (xcs, string) -> true;
            SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
            TlsSocketStrategy tlsSocketStrategy = new DefaultClientTlsStrategy(sslContext, new NoopHostnameVerifier());
            
            PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setTlsSocketStrategy(tlsSocketStrategy)
                    .setDefaultSocketConfig(SocketConfig.custom().setSoTimeout(Timeout.ofSeconds(5)).build())
                    .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
                    .setConnPoolPolicy(PoolReusePolicy.LIFO)
                    .build();
            
            CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager).build();
            HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
            requestFactory.setHttpClient(httpClient);
            return requestFactory;
        } catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException e) {
            throw new RuntimeException("error setting request factory: " + e.getMessage(), e);
        }
    }
    
    @Bean
    public RestTemplate sslNoVerificationRestTemplate() {
        RestTemplateBuilder builder = new RestTemplateBuilder();
        RestTemplate t = builder
                .requestFactory(() -> requestFactory())
                .defaultHeader("Content-Type", "application/json")
                .build();
        return t;
    }
}
