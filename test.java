@Autowired
ZeebeClient zeebe;

public void startProcess() {
    zeebe.newCreateInstanceCommand()
        .bpmnProcessId("create_request")   // ton ID BPMN
        .latestVersion()
        .variables(Map.of(
            "client_code", "12345"
        ))
        .send()
        .join();
}