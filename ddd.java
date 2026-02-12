b.addOpenApiCustomizer(openApi -> {
  if (openApi.getInfo() == null) {
    openApi.setInfo(new io.swagger.v3.oas.models.info.Info());
  }
  if (g.getTitle() != null && !g.getTitle().isBlank()) {
    openApi.getInfo().setTitle(g.getTitle());
  }
  if (g.getVersion() != null && !g.getVersion().isBlank()) {
    openApi.getInfo().setVersion(g.getVersion());
  }
});