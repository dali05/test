@Configuration
@EnableConfigurationProperties(ProxyProperties.class)
public class WebClientConfig {

    @Bean
    public WebClient webClient(ProxyProperties proxyProps) {

        HttpClient client = HttpClient.create();

        if (proxyProps.isEnabled()) {
            client = client
                .resolver(NoopAddressResolverGroup.INSTANCE)
                .proxy(proxy -> proxy
                    .type(ProxyProvider.Proxy.HTTP)
                    .host(proxyProps.getHost())
                    .port(proxyProps.getPort())
                    .username(proxyProps.getUsername())
                    .password(pass -> proxyProps.getPassword())
                );
        }

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(client))
                .build();
    }
}


