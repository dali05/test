@Bean
public ApplicationRunner runner(ApplicationContext ctx) {
    return args -> {
        System.out.println(">>> Beans ConfigurationProperties :");
        Arrays.stream(ctx.getBeansWithAnnotation(ConfigurationProperties.class).keySet().toArray())
              .forEach(System.out::println);
    };
}


@PostConstruct
public void checkYaml() {
    System.out.println(">>> YAML idacto.base-url = " + env.getProperty("idacto.base-url"));
}