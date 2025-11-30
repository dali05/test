<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:zeebe="http://camunda.org/schema/zeebe/1.0"
                  id="Definitions_1"
                  targetNamespace="http://bpmn.io/schema/bpmn">

  <bpmn:process id="simple_process" name="Simple Process" isExecutable="true">

    <bpmn:startEvent id="start">
      <bpmn:outgoing>flow_start_fetch</bpmn:outgoing>
    </bpmn:startEvent>

    <bpmn:serviceTask id="task_fetch" name="Fetch Data">
      <bpmn:extensionElements>
        <zeebe:taskDefinition type="fetch_data" />
      </bpmn:extensionElements>
      <bpmn:incoming>flow_start_fetch</bpmn:incoming>
      <bpmn:outgoing>flow_fetch_process</bpmn:outgoing>
    </bpmn:serviceTask>

    <bpmn:serviceTask id="task_process" name="Process Data">
      <bpmn:extensionElements>
        <zeebe:taskDefinition type="process_data" />
      </bpmn:extensionElements>
      <bpmn:incoming>flow_fetch_process</bpmn:incoming>
      <bpmn:outgoing>flow_process_end</bpmn:outgoing>
    </bpmn:serviceTask>

    <bpmn:endEvent id="end">
      <bpmn:incoming>flow_process_end</bpmn:incoming>
    </bpmn:endEvent>

    <bpmn:sequenceFlow id="flow_start_fetch" sourceRef="start" targetRef="task_fetch" />
    <bpmn:sequenceFlow id="flow_fetch_process" sourceRef="task_fetch" targetRef="task_process" />
    <bpmn:sequenceFlow id="flow_process_end" sourceRef="task_process" targetRef="end" />

  </bpmn:process>
</bpmn:definitions>


    @Component
public class FetchDataWorker {

    @JobWorker(type = "fetch_data")
    public Map<String, Object> handleFetchData(final ActivatedJob job) {

        System.out.println("➡️ Worker FETCH_DATA exécuté");

        // Exemple de données
        Map<String, Object> vars = new HashMap<>();
        vars.put("message", "Hello from worker!");
        vars.put("value", 42);

        // Renvoie les variables au process
        return vars;
    }
}


@Component
public class ProcessDataWorker {

    @JobWorker(type = "process_data")
    public void handleProcessData(final ActivatedJob job) {

        Map<String, Object> vars = job.getVariablesAsMap();

        System.out.println("➡️ Worker PROCESS_DATA exécuté");
        System.out.println("Variables reçues : " + vars);
    }
}


@RestController
@RequiredArgsConstructor
public class TestController {

    private final ZeebeClient zeebe;

    @PostMapping("/start")
    public String start() {
        zeebe
            .newCreateInstanceCommand()
            .bpmnProcessId("simple_process")
            .latestVersion()
            .variables(Map.of("input", "test"))
            .send()
            .join();

        return "Process started!";
    }
}

