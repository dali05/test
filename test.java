apigee:
  base-url: "https://xxxx.apigee.net/api"
  ssl:
    key-store: "classpath:ssl/apigee-client.p12"
    key-store-password: "changeit"
    key-store-type: "PKCS12"
    trust-store: "classpath:ssl/apigee-truststore.jks"
    trust-store-password: "changeit"
    trust-store-type: "JKS"

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;

@Configuration
public class ApigeeClientConfig {

    @Value("${apigee.base-url}")
    private String apigeeBaseUrl;

    @Value("${apigee.ssl.key-store}")
    private Resource keyStore;

    @Value("${apigee.ssl.key-store-password}")
    private String keyStorePassword;

    @Value("${apigee.ssl.key-store-type}")
    private String keyStoreType;

    @Value("${apigee.ssl.trust-store}")
    private Resource trustStore;

    @Value("${apigee.ssl.trust-store-password}")
    private String trustStorePassword;

    @Value("${apigee.ssl.trust-store-type}")
    private String trustStoreType;

    @Bean
    public WebClient apigeeWebClient() throws Exception {
        // Keystore (certificat client)
        KeyStore ks = KeyStore.getInstance(keyStoreType);
        try (InputStream is = keyStore.getInputStream()) {
            ks.load(is, keyStorePassword.toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, keyStorePassword.toCharArray());

        // Truststore (CA Apigee)
        KeyStore ts = KeyStore.getInstance(trustStoreType);
        try (InputStream is = trustStore.getInputStream()) {
            ts.load(is, trustStorePassword.toCharArray());
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);

        SslContext sslContext = SslContextBuilder
                .forClient()
                .keyManager(kmf)
                .trustManager(tmf)
                .build();

        HttpClient httpClient = HttpClient.create()
                .secure(ssl -> ssl.sslContext(sslContext));

        return WebClient.builder()
                .baseUrl(apigeeBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}