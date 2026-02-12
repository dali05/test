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


@Getter @Setter
@ConfigurationProperties(prefix = "springdoc")
public class SpringdocProperties {

  private ApiDocs apiDocs = new ApiDocs();
  private SwaggerUi swaggerUi = new SwaggerUi();

  // ✅ nouveau
  private Groups groups = new Groups();

  @Getter @Setter
  public static class Groups {
    private boolean enabled = false;
    private List<Group> items = new ArrayList<>();
  }

  @Getter @Setter
  public static class Group {
    private String name;
    private List<String> packagesToScan = new ArrayList<>();
    private List<String> pathsToMatch = new ArrayList<>();
  }

  // ... tes classes ApiDocs / SwaggerUi existantes
}



import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DynamicOpenApiGroupsRegistrar implements BeanDefinitionRegistryPostProcessor {

  @Override
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
    // rien ici (on a besoin des properties, donc on fait ça dans postProcessBeanFactory)
  }

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    SpringdocProperties props = beanFactory.getBean(SpringdocProperties.class);

    if (props.getGroups() == null || !props.getGroups().isEnabled()) {
      return; // groupes désactivés => on ne crée rien ici
    }

    BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

    for (SpringdocProperties.Group g : props.getGroups().getItems()) {
      String beanName = "groupedOpenApi_" + g.getName();

      RootBeanDefinition def = new RootBeanDefinition(GroupedOpenApi.class, () -> {
        GroupedOpenApi.Builder builder = GroupedOpenApi.builder().group(g.getName());

        if (g.getPackagesToScan() != null && !g.getPackagesToScan().isEmpty()) {
          builder.packagesToScan(g.getPackagesToScan().toArray(new String[0]));
        }
        if (g.getPathsToMatch() != null && !g.getPathsToMatch().isEmpty()) {
          builder.pathsToMatch(g.getPathsToMatch().toArray(new String[0]));
        }

        return builder.build();
      });

      registry.registerBeanDefinition(beanName, def);
    }
  }
}


springdoc:
  api-docs:
    path: /api/v1/openapi
  swagger-ui:
    path: /api/v1/docs
    urls:
      - name: access
        url: /api/v1/openapi?group=access
      - name: callback
        url: /api/v1/openapi?group=callback
    urlsPrimaryName: access

  groups:
    enabled: true
    items:
      - name: access
        packagesToScan:
          - com.bnpp.pf.walle.access
        pathsToMatch:
          - /access/**
      - name: callback
        packagesToScan:
          - com.bnpp.pf.walle.callback
        pathsToMatch:
          - /callback/**



