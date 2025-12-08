@Bean
public HttpClient httpClient() {

    return HttpClient.create()
            // Désactive toute résolution DNS locale
            .resolver(NoopAddressResolverGroup.INSTANCE)

            // Définit le proxy AVANT l’ouverture de la connexion
            .proxy(proxy -> proxy
                    .type(ProxyProvider.Proxy.HTTP)
                    .host("10.175.113.1")
                    .port(3128)
                    .username("h93588")
                    .password(pass -> "Bonjournp@2026")
            );
}