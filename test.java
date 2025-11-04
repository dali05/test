WebClient webClient = WebClient.create("http://localhost:8081/api/configs");

public ConfigResponse getConfigById(UUID id) {
    return webClient.get()
        .uri("/{id}", id)
        .retrieve()
        .bodyToMono(ConfigResponse.class)
        .block();
}

@Service
public class ConfigService {
    private final WebClient webClient = WebClient.builder()
        .baseUrl("http://localhost:8081/api/configs")
        .build();

    public ConfigResponse getConfigById(UUID id) {
        return webClient.get()
            .uri("/{id}", id)
            .retrieve()
            .bodyToMono(ConfigResponse.class)
            .block(); // ou asynchrone selon ton besoin
    }
}


@Service
public class ConfigService {

    private final WebClient webClient = WebClient.builder()
        .baseUrl("http://localhost:8081/api/configs")
        .build();

    public ConfigResponse getConfigById(UUID id) {
        return webClient.get()
            .uri("/{id}", id)
            .retrieve()
            .bodyToMono(ConfigResponse.class)
            .block(); // pour usage impératif
    }
}
