<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:zeebe="http://camunda.org/schema/zeebe/1.0"
                  id="Definitions_1"
                  targetNamespace="http://example.com/bpmn">

  <bpmn:process id="simpleProcess" name="Simple Process" isExecutable="true">

    <!-- Start event -->
    <bpmn:startEvent id="startEvent" name="Start">
      <bpmn:outgoing>Flow_Start_To_Task</bpmn:outgoing>
    </bpmn:startEvent>

    <!-- Service Task handled by a Spring Boot Worker -->
    <bpmn:serviceTask id="myServiceTask" name="Process Data">
      <bpmn:incoming>Flow_Start_To_Task</bpmn:incoming>
      <bpmn:outgoing>Flow_Task_To_End</bpmn:outgoing>
      <zeebe:taskDefinition type="process-data" retries="3"/>
    </bpmn:serviceTask>

    <!-- End event -->
    <bpmn:endEvent id="endEvent" name="End">
      <bpmn:incoming>Flow_Task_To_End</bpmn:incoming>
    </bpmn:endEvent>

    <!-- Flows -->
    <bpmn:sequenceFlow id="Flow_Start_To_Task" sourceRef="startEvent" targetRef="myServiceTask"/>
    <bpmn:sequenceFlow id="Flow_Task_To_End" sourceRef="myServiceTask" targetRef="endEvent"/>

  </bpmn:process>

</bpmn:definitions>

@Autowired
private ZeebeClient zeebeClient;

public void startProcess() {
    zeebeClient
        .newCreateInstanceCommand()
        .bpmnProcessId("simpleProcess")
        .latestVersion()
        .variables(Map.of(
            "inputValue", 42,
            "username", "Alice"
        ))
        .send();
}



@Component
public class ProcessDataWorker {

    @JobWorker(type = "process-data")
    public Map<String, Object> handle(final JobClient client, final ActivatedJob job) {

        Map<String, Object> vars = job.getVariablesAsMap();

        int inputValue = (int) vars.get("inputValue");
        String user = (String) vars.get("username");

        System.out.println("Received from process: " + inputValue + ", user=" + user);

        int result = inputValue * 2;

        // Variables renvoyées dans le workflow
        return Map.of(
            "resultValue", result,
            "message", "Hello " + user
        );
    }
}


<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:omgdc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:omgdi="http://www.omg.org/spec/DD/20100524/DI"
                  xmlns:zeebe="http://camunda.org/schema/zeebe/1.0"
                  id="definitions"
                  targetNamespace="http://example.com/bpmn">

  <bpmn:process id="simpleProcess" name="Simple Process Demo" isExecutable="true">

    <bpmn:startEvent id="startEvent" name="Start">
      <bpmn:outgoing>flow1</bpmn:outgoing>
    </bpmn:startEvent>

    <bpmn:serviceTask id="serviceTask" name="Process Input">
      <bpmn:incoming>flow1</bpmn:incoming>
      <bpmn:outgoing>flow2</bpmn:outgoing>
      <zeebe:taskDefinition type="process-data" />
    </bpmn:serviceTask>

    <bpmn:endEvent id="endEvent" name="Done">
      <bpmn:incoming>flow2</bpmn:incoming>
    </bpmn:endEvent>

    <bpmn:sequenceFlow id="flow1" sourceRef="startEvent" targetRef="serviceTask"/>
    <bpmn:sequenceFlow id="flow2" sourceRef="serviceTask" targetRef="endEvent"/>

  </bpmn:process>

  <bpmndi:BPMNDiagram id="BPMNDiagram_simpleProcess">
    <bpmndi:BPMNPlane id="BPMNPlane_simpleProcess" bpmnElement="simpleProcess">

      <bpmndi:BPMNShape id="Shape_Start" bpmnElement="startEvent">
        <omgdc:Bounds x="150" y="150" width="36" height="36"/>
      </bpmndi:BPMNShape>

      <bpmndi:BPMNShape id="Shape_ServiceTask" bpmnElement="serviceTask">
        <omgdc:Bounds x="260" y="135" width="120" height="70"/>
      </bpmndi:BPMNShape>

      <bpmndi:BPMNShape id="Shape_End" bpmnElement="endEvent">
        <omgdc:Bounds x="430" y="150" width="36" height="36"/>
      </bpmndi:BPMNShape>

      <bpmndi:BPMNEdge id="Edge_flow1" bpmnElement="flow1">
        <omgdi:waypoint x="186" y="168" />
        <omgdi:waypoint x="260" y="168" />
      </bpmndi:BPMNEdge>

      <bpmndi:BPMNEdge id="Edge_flow2" bpmnElement="flow2">
        <omgdi:waypoint x="380" y="168" />
        <omgdi:waypoint x="430" y="168" />
      </bpmndi:BPMNEdge>

    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>

</bpmn:definitions>
