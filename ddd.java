package com.bnpp.pf.common.api.config.doc;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.SmartInitializingSingleton;

@Component
public class DynamicOpenApiGroupsRegistrar implements SmartInitializingSingleton {

  private final SpringdocProperties props;
  private final GenericApplicationContext context;
  private final OpenApiCustomizer globalHeadersAndErrors;
  private final OperationCustomizer dynamicTags;

  public DynamicOpenApiGroupsRegistrar(SpringdocProperties props,
                                       GenericApplicationContext context,
                                       OpenApiCustomizer globalHeadersAndErrors,
                                       OperationCustomizer dynamicTags) {
    this.props = props;
    this.context = context;
    this.globalHeadersAndErrors = globalHeadersAndErrors;
    this.dynamicTags = dynamicTags;
  }

  @Override
  public void afterSingletonsInstantiated() {
    if (props.getGroups() == null || !props.getGroups().isEnabled()) {
      return;
    }

    for (SpringdocProperties.Group g : props.getGroups().getItems()) {
      if (g.getName() == null || g.getName().isBlank()) continue;
      if ("public".equalsIgnoreCase(g.getName())) continue;

      context.registerBean("groupedOpenApi_" + g.getName(), GroupedOpenApi.class, () -> {
        GroupedOpenApi.Builder b = GroupedOpenApi.builder().group(g.getName());

        if (g.getPackagesToScan() != null && !g.getPackagesToScan().isEmpty()) {
          b.packagesToScan(g.getPackagesToScan().toArray(new String[0]));
        }
        if (g.getPathsToMatch() != null && !g.getPathsToMatch().isEmpty()) {
          b.pathsToMatch(g.getPathsToMatch().toArray(new String[0]));
        }

        b.addOpenApiCustomizer(globalHeadersAndErrors);
        b.addOperationCustomizer(dynamicTags);

        return b.build();
      });
    }
  }
}