import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

String response = notificationClient.getCallbackUrlWithCaseID(caseWithConfig.get().caseId());
ObjectMapper mapper = new ObjectMapper();

JsonNode node = mapper.readTree(response);
String data = node.get("data").asText();

System.out.println(data);