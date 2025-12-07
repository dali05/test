package com.bnpp.pf.walle.access.process.adapter.in.workers;
import com.bnpp.pf.walle.access.domain.entity.AccessRequest;
import com.bnpp.pf.walle.access.process.app.port.in.NotifySyncCompletionUseCase;
import com.bnpp.pf.walle.access.process.app.port.out.AccessRequestPersistencePort;
import com.bnpp.pf.walte.access.process.config.idacto.IdactoProperties;
import com.fasterxml.jackson.databind.ObjectMapper:
import com. fasterml. jackson-databind.node.ObjectNode;
import io. camunda.zeebe.client.api.ZeebeFuture;
import io.camunda.zeebe.client.api.command. CompleteJobCommandStep1; import io.camunda.zeebe.client.api.command.FailJobCommandStep1;
import 1o. camunda.zeebe.client.api. command. FinalCommandStep; import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobCtient;
class RequestWalletDataWorkerTest (
WebClient webClient;
WebCLient.RequestHeadersUriSpec uriSpec;
WebClient.RequestHeadersSpecheadersSpec;
WebCLlent.RequestBodySpec bodySpec;
WebCLient.ResponseSpecresponseSpec;
IdactoProperties props;
AccessRequestPersistencePort persistence;
NotifySyncCompletionUseCase notifyUseCase;
ActivatedJob job;
JobClient JobClient;
FinalCommandStep completeStep;
FinalCommandStep failStep;
Reques tWalletDataworker worker;
ObjectNode fakeTemplate;
@Mock
private CompleteJobCommandStep1 completeCommandStep1;
@Mock
private FinalCommandStep<Void> finalCompleteCommandStep;
@Mock
private FailJobCommandStep1 failCommandStep1;
@Mock
private FinalCommandStep<Void> finalFailCommandStep;
@BeforeEach
void setup() throws Exception ( webClient -mock(WebCLient.class);
uriSpec - mock(WebClient.RequestHeadersUriSpec.class) ;
headersSpec= mock (WebClient.RequestHeadersSpec.class) ;
bodySpec = mock(WebCLient.RequestBodySpec.class);
responseSpec = mock(WebClient.ResponseSpec.class);
props = mock(IdactoProperties.class);
persistence = mock(AccessRequestPersistencePort.class);
notifyUseCase = mock(NotifySyncCompletionUseCase.class);
job = mock(ActivatedJob.class) ;
jobClient = mock(JobClient.class);
completeStep = mock(FinalCommandStep-class) ;
failStep = mock(FinalCommandStep.class);
fakeTemplate = new ObjectMapper). createObjectNode();
fakeTemp late. put ("field",
"value");
worker = Mockito.spy(new RequestWalletDataworker(
webClient, props, persistence, notifyUseCase, new ObjectMapper()
));
doReturn(fakeTemp late) when (worker). loadTemplateJson();
when(props.authorizationPath()). thenReturn("/auth");
when (props. parseTokenPath()). thenReturn("/parse");
void testSuccess()
throws Exception (
UUID id = UUID. randomUUID() ;
when(job. getVariablesAsMap()). thenReturn(Map.of ("requestId", id. toString()));
AccessRequest ar = mock(AccessRequest. class) :
when (ar.getResponseCode()). thenReturn("K");
when(persistence. findById(id)). thenReturn(java.util.Optional.of(ar));
when (webCLient.get()). thenReturn(uriSpec);
when(uriSpec.uri((URI) any())). thenReturn(headersSpec);
when(headersSpec. retrieve)). thenReturn(responseSpec) :
when(responseSpec.bodyToMono (String.class)). thenReturn(reactor.core.publisher.Mono.just"TOKEN123")) ;
when (webCLient. post)). thenReturn( (WebCLient.RequestBodyUriSpec) bodySpec);
when ((WebCLient.RequestBodyUriSpec) bodySpec). uri("/parse"')). thenReturn(bodySpec);
when(bodySpec. contentType(MediaType. APPLICATION_JSON) ). thenReturn(bodySpec);
when(bodySpec. bodyValue(any))). thenReturn (headersSpec);
when (bodySpec. retrieve()). thenReturn(responseSpec);
when(responseSpec.bodyToMono (Map. class))
thenReturn( reactor. core.publisher.Mono. just (Map.of ("k", "y")):
hen(jobCLient.newCompLeteCommand(job.getKey())). thenReturn((CompleteJobConmandStep1) completeStep);
when(completeCommandStep1.variables (InputStream) any))). thenReturn (CompleteJobCommandStep1) responseSpec);
when(completeStep.send()). theReturn((ZeebeFuture) CompletableFuture.completedFuture(nutL)):
worker.handleRequestwalletData(job, jobClient);
verify( completeCommandStep1). variables (Map.of("k", "v"));
erify(JobClient),newCompleteCommand(job.getKey());
@Test
void testNotFound() (
UUID 1d = UUID, randomUUID();
hen(job.getVariablesAsMap()). thenReturn(Map.of("requestId", id. toString())):
hen(persistence. findById(1d)). thenReturn(java.uti1.Optional.empty));
hen(jobClient.newFa1lCommand(job.getKey())), thenReturn((FaitJobCommandStep1) failStep);
when(failCommandStep1, retries(0)). thenReturn( (FailJobCommandStep1.FailJobCommandStep2) failStep);
when(failStep. send)). thenReturn( (ZeebeFuture) CompletableFuture. completedFuture(nutt));
worker. handleRequestwalletData(job, jobClient);
verify(notifyUseCase), notifyError(eq(1d), any()) ;
@Test
void testGetVpTokenError) /
when(webClient.get)). thenReturn(uriSpec) :
when(uriSpec.uri (URI) any())), thenReturn(headersSpec) ;
when(headersSpec. retrieve()). thenThrow(new RuntimeException("FAIL")) ;
assertThrows (RuntimeException.class, () - worker.getVpToken("a",
Guest
void testParseVpTokenError() throws Exception (
when(webCLient.post)). thenReturn( (WebClient.RequestBodyUriSpec) bodySpec);
when ((WebClient.RequestBodyUrSpec) bodySpec).uri("/parse")). thenReturn(bodySpec);
when(bodySpec. contentType (MediaType. APPLICATION_JSON)) . thenReturn(bodySpec);
when(bodySpec.bodyValue(any))) . thenReturn(headersSpec);
when (bodySpec, retrieve()). thenThrow(new RuntimeException ("POST_FAIL")) ; assertThrows (Runt imeException. class, () → worker.parseVpToken("XYZ")) :
GecT
void testBuildFinalPayload()(
ObjectNode out = worker. buildFinalPayLoad ("AAA");
assertEquals ("AAA", out.get("vptoken").asText());
assertEquals("value", out.get"field"). asText)