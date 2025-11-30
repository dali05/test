@SpringBootApplication
public class DecentraBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(DecentraBackApplication.class, args);
    }

    @Bean
    CommandLineRunner deployWorkflow(ZeebeClient client) {
        return args -> {
            client.newDeployResourceCommand()
                    .addResourceFile("processes/create_request.bpmn")
                    .send()
                    .join();

            System.out.println("🚀 BPMN déployé avec succès !");
        };
    }
}