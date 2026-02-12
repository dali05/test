package com.bnpp.pf.common.api.config.doc;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DynamicOpenApiGroupsRegistrar implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

  private Environment environment;

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
    // ✅ lire les props TÔT via Binder (pas via SpringdocProperties bean)
    SpringdocProperties.Groups groups = Binder.get(environment)
        .bind("springdoc.groups", SpringdocProperties.Groups.class)
        .orElseGet(SpringdocProperties.Groups::new);

    if (!groups.isEnabled() || groups.getItems() == null) return;

    for (SpringdocProperties.Group g : groups.getItems()) {
      if (g.getName() == null || g.getName().isBlank()) continue;
      if ("public".equalsIgnoreCase(g.getName())) continue;

      String beanName = "groupedOpenApi_" + g.getName();

      RootBeanDefinition def = new RootBeanDefinition(GroupedOpenApi.class);
      def.setInstanceSupplier(() -> {
        GroupedOpenApi.Builder b = GroupedOpenApi.builder().group(g.getName());

        if (g.getPackagesToScan() != null && !g.getPackagesToScan().isEmpty()) {
          b.packagesToScan(g.getPackagesToScan().toArray(new String[0]));
        }
        if (g.getPathsToMatch() != null && !g.getPathsToMatch().isEmpty()) {
          b.pathsToMatch(g.getPathsToMatch().toArray(new String[0]));
        }

        return b.build();
      });

      registry.registerBeanDefinition(beanName, def);
    }
  }

  @Override
  public void postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory)
      throws BeansException {
    // rien
  }
}