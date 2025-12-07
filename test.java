N
.onpp.pr.walle.access.process.adapter.i
package com.bnpp.pf.walle.access.process.adapter.in.workers;
import com.bnpp-pf.walle.access.domain.entity.AccessRequest;
import com.bnpp.pf.walle.access.process.app-port.in.NotifySyncCompletionUseCase;
com.bnpp.pf.walle.access.process.app.port.out.AccessRequestPersistencePort;
com.bnpp.pf.walle.access.process.config.idacto.IdactoProperties;
import com.fasterxml.jackson.databind.ObjectMapper; import com.fasterxml.jackson.databind.node.ObjectNode;
10. camunda. zeebe.client.api.ZeebeFuture;
io. camunda.zeebe.client.api.command. CompleteJobCommandStep1; io. camunda.zeebe.client.api.command. Fail.JobCommandStep1;
import
io. camunda.zeebe.client.api.command.FinalCommandStep:
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
class RequestWalletDataworkerTest {
WebClient webClient;
WebClient.RequestHeadersUriSpec uriSpec;
WebCLient.RequestHeadersSpec headersSpec;
WebCLient.RequestBodySpec bodySpec;
WebCLient.ResponseSpecresponseSpec;
IdactoProperties props;
AccessRequestPersistencePort persistence;
NotifySyncCompletionUseCase notifyUseCase;
ActivatedJob job;
JobClient jobClient;
FinalCommandStep completeStep;
FinalCommandStep failstep;
RequestWalletDataworker worker;
ObjectNode fakeTemplate;
@Mock
private CompleteJobCommandStep1 completeCommandStep1;
@Mock
private FinalCommandStep<Void> finalCompleteCommandStep;
private FailJobCommandStep1 failCommandStep1;
@Mock
private FinalCommandStep<Void> finalFailCommandStep;
@BeforeEach
void setup() throws Exception (
webCLient = mock(WebClient.class);
uriSpec = mock (WebClient.RequestHeadersUriSpec.class);
headersSpec= mock(WebClient.RequestHeadersSpec.class):
bodySpec = mock(WebClient.RequestBodySpec.class) ;
responseSpec = mock(WebClient.ResponseSpec.class) ;
props = mock(IdactoProperties.class);
persistence - mock(AccessRequestPersistencePort.class);
notifyUseCase = mock(NotifySyncCompletionUseCase.class);
job =
mock ActivatedJob.class):
jobClient = mock(JobClient.class) ;
completeStep = mock(FinalCommandStep.class);
failStep = mock(FinalCommandStep-class);
fakeTemplate = new ObjectMapper(). createObjectNode() ;
fakeTemplate-put ("field",
"value");
worker = Mockito.spy(new RequestWalletDataworker webClient,props, persistence, notifyUseCase, new ObjectMapper()
)) ;
doReturn(fakeTemplate). when (worker). LoadTemp lateJson();
when(props.authorizationPath()). thenReturn("/auth"');
when (props. parseTokenPath()). thenReturn("/parse");
@Test
void testSuccess) throws Exception (
UUID id = UUID. randomUUID() ;
when (job.getVariablesAsMap()). thenReturn(Map.of ("requestId", id. toString()));
AccessRequest ar = mock(AccessRequest.class) ;
when(ar-getResponseCode()). thenReturn('OK"*);
when(persistence. findById(id)). thenReturn(java.util.Optional.of(ar));
when (webCLient.get()). thenReturn(uriSpec) ;
when(uriSpec.uri((URI) any())). thenReturn(headersSpec);
when(headersSpec. retrieve)). thenReturn(responseSpec);
when(responseSpec. bodyToMono (String.class)). thenReturn(reactor.core.publisher.Mono. just("TOKEN123")) ;
when(webCLient.post()). thenReturn( (WebCLient.RequestBodyUriSpec) bodySpec);
when ( (WebCLient.RequestBodyUriSpec) bodySpec). uri("/parse")) . thenReturn(bodySpec);
when (bodySpec. contentType(MediaType. APPLICATION_JSON)) . thenReturn(bodySpec);
when(bodySpec. bodyValue(any))) . thenReturn(headersSpec);
when (bodySpec. retrieve()). thenReturn(responseSpec);
when(responseSpec.bodyToMono (Map. class))
•thenReturn(reactor.core. publisher.Mono. just (Map.of ("K", "y"))):
when(jobCLient.newCompLeteCommand(job.getKey())). thenReturn( (CompleteJobCommandStep1) CompleteStep);
when(completeCommandStep1.variables((InputStream) any ())). thenReturn (CompleteJobCommandStep1) responseSpec);
when(completeStep.send()). thenReturn( (ZeebeFuture) CompletableFuture.completedFuture(nutl));
worker.handleRequestWalletData(job, jobClient);
verify(completeCommandStep1). variables(Map.of ("k", "v")) ;
verify(jobClient) ,newCompleteCommand(job.getKey());
@Test
void testNotFound() {
UUID id = UUID, randomUUID();
when(job. getVariablesAsMap()), thenReturn(Map.of (requestId", id. toString());
when(persistence. findById(id)).thenReturn(java.util.Optional.empty());
when(jobClient.newFailCommand(job.getKey))), thenReturn( (FailJobCommandStep1) failStep);
when(failCommandStep1, retries(8)), thenReturn (Fai1JobCommandStep1.Fa1lJobCommandStep2) failStep);
when(failStep.send()).thenReturn((ZeebeFuture) CompletableFuture.completedFuture(nutl));
worker, handleRequestwalletData(job, jobClient); verify (notIfyUseCase) .notifyError(eq(id), any()):
@Test
void testGetVpTokenError() (
when(webClient.get)). thenReturn(uriSpec) ;
when(uriSpec.uri((URI) any())). thenReturn(headersSpec) ;
when(headersSpec.retrieve)).thenThrow(new Runt.imeExcept._on("FAIL"):
assertThrows (Runt ImeException. class, () - worker.getVpToken("a",
@Test
void testParseVpTokenError() throws Exception
when (webClient.post)). thenReturn( (WebClient.RequestBodyUriSpec) bodySpec) ;
when ( (WebClient.RequestBodyUr1Spec) bodySpec).uri("/parse")).thenReturn(bodySpec);
when(bodySpec.contentType(MediaType.APPLICATION_JSON)). thenReturn(bodySpec) ;
when(bodySpec.bodyValue(any))). thenReturn(headersSpec) ;
when(bodySpec. retrieve()). thenThrow(new RuntimeException("POST_FAIL"));
assertThrows (RuntimeException.class, () - worker.parseVpToken("XYZ")):
@Test
void testBuildFinalPayload(){
ObjectNode out = worker.buildFinalPayload("AAA");
assertEquals ("AAA",
out.get("vptoken").asText()):
assertEquals("value", out.get("field").asText)) :