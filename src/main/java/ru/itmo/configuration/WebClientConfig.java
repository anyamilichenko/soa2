//package ru.itmo.configuration;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.reactive.function.client.WebClient;
//
//@Configuration
//public class WebClientConfig {
//
//    private final String baseUrl;
//
//    public WebClientConfig(@Value("${service.a.base-url}") String baseUrl) {
//        this.baseUrl = baseUrl;
//    }
//
//    @Bean("serviceAClient")
//    public WebClient serviceAClient() {
//        return WebClient.builder()
//                .baseUrl(baseUrl)
//                .build();
//    }
//}


package ru.itmo.configuration;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;

@Configuration
public class WebClientConfig {

    @Value("${service.a.base-url}")
    private String baseUrl;

    @Bean("serviceAClient")
    public WebClient serviceAClient() throws Exception {
        // ================== ЗАГРУЗКА KEYSTORE (клиентский сертификат сервиса B) ==================
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = new ClassPathResource("service-b.p12").getInputStream()) {
            keyStore.load(is, "secret123".toCharArray());
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "secret123".toCharArray());

        // ================== ЗАГРУЗКА TRUSTSTORE (доверяем сервису A) ==================
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = new ClassPathResource("service-b-truststore.p12").getInputStream()) {
            trustStore.load(is, "secret123".toCharArray());
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        // ================== СОЗДАНИЕ SSL КОНТЕКСТА ==================
        SslContext sslContext = SslContextBuilder.forClient()
                .keyManager(keyManagerFactory)           // Наш клиентский сертификат (для mTLS если нужно)
                .trustManager(trustManagerFactory)      // Доверяем сертификату сервиса A
                .build();

        // ================== СОЗДАНИЕ HTTP CLIENT С SSL ==================
        HttpClient httpClient = HttpClient.create()
                .secure(spec -> spec.sslContext(sslContext));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
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