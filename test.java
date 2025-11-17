import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OAuthClient {

    private final WebClient webClient = WebClient.builder().build();
    private final ObjectMapper mapper = new ObjectMapper();

    public String requestAccessToken(String assertion, String tokenUrl, String scope) {
        String response = webClient.post()
                .uri(tokenUrl)
                .bodyValue("grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer"
                        + "&assertion=" + assertion
                        + "&scope=" + scope)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return mapper.readTree(response).get("access_token").asText();
    }
}
