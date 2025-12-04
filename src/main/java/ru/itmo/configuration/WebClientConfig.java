package ru.itmo.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    private final String baseUrl;

    public WebClientConfig(@Value("${service.a.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Bean("serviceAClient")
    public WebClient serviceAClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}


//package configuration;
//
//import io.netty.handler.ssl.SslContext;
//import io.netty.handler.ssl.SslContextBuilder;
//import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.client.reactive.ReactorClientHttpConnector;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.netty.http.client.HttpClient;
//
//import javax.net.ssl.SSLException;
//
//@Configuration
//public class WebClientConfig {
//
//    @Bean("serviceAClient")
//    public WebClient serviceAClient() {
//        try {
//            // Создаем SSL контекст, который доверяет всем сертификатам (для самоподписанных)
//            SslContext sslContext = SslContextBuilder
//                    .forClient()
//                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
//                    .build();
//
//            HttpClient httpClient = HttpClient.create()
//                    .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));
//
//            return WebClient.builder()
//                    .baseUrl("https://localhost:8218")
//                    .clientConnector(new ReactorClientHttpConnector(httpClient))
//                    .build();
//        } catch (SSLException e) {
//            throw new RuntimeException("Failed to create SSL context for WebClient", e);
//        }
//    }
//}