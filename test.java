<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                  xmlns:zeebe="http://camunda.org/schema/zeebe/1.0"
                  id="Definitions_123"
                  targetNamespace="http://bpmn.io/schema/bpmn">
                  
  <bpmn:process id="simple_process" name="Simple Process" isExecutable="true">

    <bpmn:startEvent id="StartEvent_1" name="Start">
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>

    <bpmn:serviceTask id="Task_FetchData" name="Fetch Data">
      <bpmn:extensionElements>
        <zeebe:taskDefinition type="fetch_data" />
      </bpmn:extensionElements>
      <bpmn:incoming>Flow_1</bpmn:incoming>
      <bpmn:outgoing>Flow_2</bpmn:outgoing>
    </bpmn:serviceTask>

    <bpmn:serviceTask id="Task_ProcessData" name="Process Data">
      <bpmn:extensionElements>
        <zeebe:taskDefinition type="process_data" />
      </bpmn:extensionElements>
      <bpmn:incoming>Flow_2</bpmn:incoming>
      <bpmn:outgoing>Flow_3</bpmn:outgoing>
    </bpmn:serviceTask>

    <bpmn:endEvent id="EndEvent_1" name="End">
      <bpmn:incoming>Flow_3</bpmn:incoming>
    </bpmn:endEvent>

    <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="Task_FetchData"/>
    <bpmn:sequenceFlow id="Flow_2" sourceRef="Task_FetchData" targetRef="Task_ProcessData"/>
    <bpmn:sequenceFlow id="Flow_3" sourceRef="Task_ProcessData" targetRef="EndEvent_1"/>

  </bpmn:process>

  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="simple_process">

      <bpmndi:BPMNShape id="StartEvent_1_di" bpmnElement="StartEvent_1">
        <dc:Bounds x="150" y="200" width="36" height="36"/>
      </bpmndi:BPMNShape>

      <bpmndi:BPMNShape id="Task_FetchData_di" bpmnElement="Task_FetchData">
        <dc:Bounds x="250" y="180" width="120" height="80"/>
      </bpmndi:BPMNShape>

      <bpmndi:BPMNShape id="Task_ProcessData_di" bpmnElement="Task_ProcessData">
        <dc:Bounds x="420" y="180" width="120" height="80"/>
      </bpmndi:BPMNShape>

      <bpmndi:BPMNShape id="EndEvent_1_di" bpmnElement="EndEvent_1">
        <dc:Bounds x="590" y="200" width="36" height="36"/>
      </bpmndi:BPMNShape>

      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
        <di:waypoint x="186" y="218"/>
        <di:waypoint x="250" y="220"/>
      </bpmndi:BPMNEdge>

      <bpmndi:BPMNEdge id="Flow_2_di" bpmnElement="Flow_2">
        <di:waypoint x="370" y="220"/>
        <di:waypoint x="420" y="220"/>
      </bpmndi:BPMNEdge>

      <bpmndi:BPMNEdge id="Flow_3_di" bpmnElement="Flow_3">
        <di:waypoint x="540" y="220"/>
        <di:waypoint x="590" y="220"/>
      </bpmndi:BPMNEdge>

    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>

</bpmn:definitions>
@JobWorker(type = "fetch_data")
public void fetchData(final ActivatedJob job) {
    System.out.println("→ Fetch Data exécuté");
}
@JobWorker(type = "process_data")
public void processData(final ActivatedJob job) {
    System.out.println("→ Process Data exécuté");
}
{
  "bpmnProcessId": "simple_process",
  "variables": { "x": 1 }
}
