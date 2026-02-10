springdoc:
  api-docs:
    path: /api/v1/openapi
  swagger-ui:
    path: /api/v1/docs
    urls:
      - name: access
        url: /api/v1/openapi/access
      - name: callback
        url: /api/v1/openapi/callback
    urlsPrimaryName: access


import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomizer;
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
        .addOpenApiCustomizer(titleAndVersion("WALL-E ACCESS API", "V0"))
        .build();
  }

  @Bean
  public GroupedOpenApi callbackApi() {
    return GroupedOpenApi.builder()
        .group("callback")
        .packagesToScan("com.tonprojet.callback.controller")
        .addOpenApiCustomizer(titleAndVersion("WALL-E CALLBACK API", "V0"))
        .build();
  }

  private OpenApiCustomizer titleAndVersion(String title, String version) {
    return openApi -> {
      if (openApi.getInfo() == null) openApi.setInfo(new Info());
      openApi.getInfo().setTitle(title);
      openApi.getInfo().setVersion(version);
    };
  }
}