    // 1) Enregistrement DB
    repo.save(...);

    // 2) Envoi du message Zeebe pour débloquer wait4RequestRetrieval
    zeebeClient
        .newPublishMessageCommand()
        .messageName("walletGetRequest4IdOK")      // nom défini dans le BPMN
        .correlationKey(requestId.toString())      // clé de corrélation
        .variables(Map.of(
            "requestId", requestId,
            "walletResponse", "OK"
        ))
        .send()
        .join();

    log.info("Message walletGetRequest4IdOK envoyé pour requestId {}", requestId);
}

        




<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="https://www.omg.org/spec/DMN/20191111/MODEL/"
             xmlns:feel="http://www.omg.org/spec/DMN/20191111/FEEL/"
             id="definitions_eligibility"
             name="EligibilityDecision"
             namespace="http://example.com/eligibility">

  <decision id="identificationDecision" name="identificationDecision">
    <variable name="eligibilityDecision" typeRef="feel:any"/>

    <decisionTable hitPolicy="COLLECT">

      <!-- INPUTS -->
      <input id="in_datasets" label="datasets">
        <inputExpression typeRef="feel:any">
          <text>datasets</text>
        </inputExpression>
      </input>

      <input id="in_caseName" label="caseName">
        <inputExpression typeRef="feel:string">
          <text>caseName</text>
        </inputExpression>
      </input>

      <!-- OUTPUT -->
      <output id="out_error" name="error" typeRef="feel:any" />

      <!-- RULE 1 : dataset non valide -->
      <rule>
        <inputEntry><text>some d in datasets satisfies not(d in [100,105,210])</text></inputEntry>
        <inputEntry><text>-</text></inputEntry>
        <outputEntry>
          <text>{
            "field": "datasets",
            "message": "Invalid dataset code",
            "value": string(datasets),
            "expected": "100,105,210"
          }</text>
        </outputEntry>
      </rule>

      <!-- RULE 2 : caseName vide -->
      <rule>
        <inputEntry><text>-</text></inputEntry>
        <inputEntry><text>caseName = null or caseName = ""</text></inputEntry>
        <outputEntry>
          <text>{
            "field": "caseName",
            "message": "caseName must not be empty",
            "value": caseName
          }</text>
        </outputEntry>
      </rule>

      <!-- RULE 3 : caseName incorrect -->
      <rule>
        <inputEntry><text>-</text></inputEntry>
        <inputEntry><text>not(caseName in ["FRB2C","SPB2C"])</text></inputEntry>
        <outputEntry>
          <text>{
            "field": "caseName",
            "message": "Invalid caseName value",
            "value": caseName,
            "expected": "FRB2C, SPB2C"
          }</text>
        </outputEntry>
      </rule>

      <!-- RULE 4 : OK -->
      <rule>
        <inputEntry><text>-</text></inputEntry>
        <inputEntry><text>-</text></inputEntry>
        <outputEntry><text>null</text></outputEntry>
      </rule>

    </decisionTable>
  </decision>

</definitions>