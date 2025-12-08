import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.resolver.NoopAddressResolverGroup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.net.InetSocketAddress;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {

        HttpClient httpClient = HttpClient.create()
                // ⚠️ Empêche TOUTE résolution DNS locale
                .resolver(NoopAddressResolverGroup.INSTANCE)
                .tcpConfiguration(tcpClient ->
                    tcpClient.doOnConnected(conn ->
                        conn.addHandlerFirst(
                                new HttpProxyHandler(
                                        new InetSocketAddress("10.175.113.1", 3128),
                                        "h93588",
                                        "Bonjournp@2026"
                                )
                        )
                    )
                );

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}