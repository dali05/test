import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public GroupedOpenApi accessApi() {
    return GroupedOpenApi.builder()
        .group("access")
        .packagesToScan("com.tonprojet.access.controller")
        .build();
  }

  @Bean
  public GroupedOpenApi callbackApi() {
    return GroupedOpenApi.builder()
        .group("callback")
        .packagesToScan("com.tonprojet.callback.controller")
        .build();
  }
}