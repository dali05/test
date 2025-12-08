@Bean
public WebClient webClient() {

    HttpClient httpClient = HttpClient.create()
            .tcpConfiguration(tcpClient ->
                tcpClient.doOnConnected(conn ->
                    conn.addHandlerFirst(new HttpProxyHandler(
                            new InetSocketAddress("10.175.113.1", 3128),
                            "h93588",
                            "TON_MOT_DE_PASSE"
                    ))
                )
            );

    return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
}