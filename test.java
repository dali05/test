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

identificationDecision.dmn

<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="https://www.omg.org/spec/DMN/20191111/MODEL/"
             xmlns:feel="http://www.omg.org/spec/DMN/20191111/FEEL/"
             id="definitions_eligibility"
             name="EligibilityDecision"
             namespace="http://example.com/eligibility">

  <!-- ===== DECISION PRINCIPALE ===== -->
  <decision id="identificationDecision" name="identificationDecision">
    <variable id="eligibilityDecisionVar" name="eligibilityDecision" typeRef="feel:object"/>

    <decisionTable id="eligibilityTable" hitPolicy="COLLECT">

      <!-- === INPUTS === -->
      <input id="input_dataset" label="datasets">
        <inputExpression id="inputExpr_dataset" typeRef="feel:any">
          <text>datasets</text>
        </inputExpression>
      </input>

      <input id="input_caseName" label="caseName">
        <inputExpression id="inputExpr_caseName" typeRef="feel:string">
          <text>caseName</text>
        </inputExpression>
      </input>

      <!-- === OUTPUT (1 erreur par règle) === -->
      <output id="out_error" name="error" typeRef="feel:any"/>

      <!-- ====================== -->
      <!-- ===== RULES LIST ===== -->
      <!-- ====================== -->

      <!-- RULE 1 : datasets invalid codes -->
      <rule id="rule_invalid_dataset">
        <inputEntry><text>not(datasets all item in [100,105,210])</text></inputEntry>
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

      <!-- RULE 2 : caseName obligatoire -->
      <rule id="rule_caseName_empty">
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

      <!-- RULE 3 : caseName doit être FRB2C ou SPB2C -->
      <rule id="rule_caseName_invalid_value">
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

      <!-- RULE 4 : OK si tout est valide -->
      <rule id="rule_ok">
        <inputEntry><text>-</text></inputEntry>
        <inputEntry><text>-</text></inputEntry>
        <outputEntry><text>null</text></outputEntry>
      </rule>

    </decisionTable>
  </decision>

  <!-- ======================================== -->
  <!-- DECISION 2 : Agrégation des erreurs     -->
  <!-- ======================================== -->

  <decision id="finalEligibility" name="finalEligibility">
    <variable name="eligibilityDecision" typeRef="feel:object"/>

    <informationRequirement>
      <requiredDecision href="#identificationDecision"/>
    </informationRequirement>

    <literalExpression id="expr_aggregate">
      <text>
        let 
            rawErrors = identificationDecision.error,
            errorList = rawErrors[error != null]
        in 
          if count(errorList) = 0 then
            {"finalDecision": "OK", "errors": []}
          else
            {"finalDecision": "KO", "errors": errorList}
      </text>
    </literalExpression>
  </decision>

</definitions>
