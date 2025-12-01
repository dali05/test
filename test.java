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