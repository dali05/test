france-identite:
  base-url: https://api.playground.france-identite.gouv.fr
  authorization-path: /idakto/openid4vp/v1/authorization/data
  parse-token-path: /idakto/openid4vp/internal/v1/parsevptoken
  template-path: /template.json



// Java 17+ record : simple et immuable
@ConfigurationProperties(prefix = "france-identite")
public record FranceIdentiteProperties(
        String baseUrl,
        String authorizationPath,
        String parseTokenPath,
        String templatePath
) {}

@Configuration
@EnableConfigurationProperties(FranceIdentiteProperties.class)
public class FranceIdentiteConfig {

    @Bean
    WebClient franceIdentiteWebClient(WebClient.Builder builder,
                                      FranceIdentiteProperties props) {
        return builder
                .baseUrl(props.baseUrl())
                .build();
    }
}


@Service
@RequiredArgsConstructor
public class RequestWalletDataWorker {

    private final WebClient franceIdentiteWebClient;
    private final ObjectMapper objectMapper;
    private final FranceIdentiteProperties props;

    // on garde un template en mémoire pour éviter de relire le fichier à chaque appel
    private final ObjectNode templatePayload = loadTemplateJson();

    /**
     * Appel GET pour récupérer le vpToken
     */
    public Mono<String> getVpToken(String transactionId, String responseCode) {
        return franceIdentiteWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path(props.authorizationPath())
                        .queryParam("transaction_id", transactionId)
                        .queryParam("response_code", responseCode)
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }

    /**
     * Appel POST pour parser le vpToken
     */
    public Mono<Map<String, Object>> parseVpToken(String vpToken) {
        var body = buildFinalPayload(vpToken);

        return franceIdentiteWebClient
                .post()
                .uri(props.parseTokenPath())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    /**
     * Charge le template JSON depuis le classpath.
     * Throw une IllegalStateException si le fichier est introuvable ou invalide.
     */
    private ObjectNode loadTemplateJson() {
        try (var is = getClass().getResourceAsStream(props.templatePath())) {
            if (is == null) {
                throw new IllegalStateException("Template JSON introuvable : " + props.templatePath());
            }
            return (ObjectNode) objectMapper.readTree(is);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire le template JSON", e);
        }
    }

    /**
     * Crée le payload final à partir du template en injectant un vpToken.
     * On fait une copie profonde pour ne pas modifier le template en mémoire.
     */
    private ObjectNode buildFinalPayload(String vpToken) {
        var root = templatePayload.deepCopy();
        root.put("vptoken", vpToken);
        return root;
    }
}


String token = requestWalletDataWorker.getVpToken(id, code).block();
Map<String, Object> parsed = requestWalletDataWorker.parseVpToken(token).block();

