package io.fireflysoftware.experience.insurance.web.policies.impl;

import io.fireflysoftware.domain.insurance.policies.sdk.api.GovernedPoliciesApi;
import io.fireflysoftware.domain.insurance.policies.sdk.invoker.ApiClient;
import io.fireflysoftware.experience.insurance.core.journey.policies.*;
import io.fireflysoftware.experience.insurance.web.policies.GovernedPolicyDomainClientFactory;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.*;
import org.springframework.mock.http.client.reactive.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.*;

class GeneratedGovernedPolicyDomainCommandsTest {
    static final String DIGEST="a".repeat(64);
    GovernedPolicyBindRequest request(){return new GovernedPolicyBindRequest(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),1,1,UUID.randomUUID(),DIGEST,DIGEST,UUID.randomUUID(),"SANDBOX-1",LocalDate.of(2027,1,1),LocalDate.of(2028,1,1));}
    record Sent(String path,String query,List<String> authorization,String body,String retryKey){}
    static final class LeaseFixtureImpl implements GovernedPolicyDomainClientFactory.Client {
        final String caller;final List<Sent> sent=new CopyOnWriteArrayList<>();HttpStatus status=HttpStatus.OK;String response;boolean lost,refused,refuseAfterResponse,unavailable;
        LeaseFixtureImpl(String caller){this.caller=caller;}
        @Override public URI endpoint(){return URI.create("https://synthetic-domain-policy.invalid");}
        @Override public String bearerToken(){return caller;}
        @Override public Duration remaining(){return Duration.ofSeconds(2);}
        @Override public Mono<Void> revalidate(){return Mono.defer(()->unavailable?Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)):refused?Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN)):Mono.empty());}
        @Override public ClientHttpConnector connector(){return (method,uri,callback)->{
            var request=new MockClientHttpRequest(method,uri);return callback.apply(request).then(Mono.defer(request::getBodyAsString)).flatMap(body->{
                sent.add(new Sent(uri.getPath(),uri.getRawQuery(),List.copyOf(request.getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION)),body,request.getHeaders().getFirst("X-Idempotency-Key")));
                if(lost)return Mono.error(new IOException("controlled loss after possible dispatch"));
                var reply=new MockClientHttpResponse(status);reply.getHeaders().setContentType(MediaType.APPLICATION_JSON);reply.setBody(response);if(refuseAfterResponse)refused=true;return Mono.just((ClientHttpResponse)reply);
            });};}
    }
    String command(UUID action,String status){return "{\"status\":\""+status+"\",\"actionId\":\""+action+"\",\"intentDigest\":\""+DIGEST+"\"}";}
    @Test void genuineDomainApiAndModelsComeFromTheSelectedOriginalSourceReactor()throws Exception{
        var expected=java.nio.file.Path.of("..","..","domain-insurance-policies","domain-insurance-policies-sdk","target","classes").toRealPath();
        for(Class<?> type:List.of(GovernedPoliciesApi.class,io.fireflysoftware.domain.insurance.policies.sdk.model.GovernedPolicyCommandResponse.class,io.fireflysoftware.domain.insurance.policies.sdk.model.GovernedPolicyRecoveryResponse.class,io.fireflysoftware.domain.insurance.policies.sdk.model.GovernedPolicyIntentRequest.class,io.fireflysoftware.domain.insurance.policies.sdk.model.GovernedPolicyPreparedIntent.class))FreshSourceSdkFixture.require(type,expected);
    }
    @Test void actualGeneratedBindCarriesAllOriginalSelectorsAndOnlyTheExchangedBearer(){
        var lease=new LeaseFixtureImpl("exchanged-human-A");var request=request();lease.response=command(request.actionId(),"OUTCOME_UNKNOWN");
        StepVerifier.create(new GeneratedGovernedPolicyDomainCommandsImpl(lease).bind(request)).assertNext(value->{assertEquals(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,value.status());assertEquals(request.actionId(),value.actionId());assertEquals(DIGEST,value.intentDigest());}).verifyComplete();
        assertEquals(1,lease.sent.size());var sent=lease.sent.getFirst();assertEquals("/api/v1/policies/governed/bindings",sent.path());assertEquals(List.of("Bearer exchanged-human-A"),sent.authorization());assertNull(sent.retryKey());
        assertDoesNotThrow(()->{var json=ApiClient.createDefaultMapper(null).readTree(sent.body());assertEquals(14,json.size());assertEquals(request.actionId().toString(),json.path("actionId").asText());assertEquals(request.submissionId().toString(),json.path("submissionId").asText());assertEquals(request.partyId().toString(),json.path("partyId").asText());assertEquals(request.productId().toString(),json.path("productId").asText());assertEquals(request.offerId().toString(),json.path("offerId").asText());assertEquals(1,json.path("offerVersion").asLong());assertEquals(1,json.path("acceptedControlGeneration").asLong());assertEquals(request.acceptanceActionId().toString(),json.path("acceptanceActionId").asText());assertEquals(DIGEST,json.path("acceptanceIntentDigest").asText());assertEquals(DIGEST,json.path("termsDigest").asText());assertEquals(request.contractId().toString(),json.path("contractId").asText());assertEquals("SANDBOX-1",json.path("policyNumber").asText());assertEquals("2027-01-01",json.path("inceptionDate").asText());assertEquals("2028-01-01",json.path("expiryDate").asText());});
    }
    @Test void actualGeneratedActivationKeepsTheExistingPolicyAndReceipt(){var lease=new LeaseFixtureImpl("human-A");var policy=UUID.randomUUID();var request=new GovernedPolicyIssueRequest(UUID.randomUUID(),1,DIGEST);lease.response=command(request.actionId(),"COMMITTED_RESPONSE_UNAVAILABLE");StepVerifier.create(new GeneratedGovernedPolicyDomainCommandsImpl(lease).issue(policy,request)).assertNext(value->assertEquals(GovernedPolicyCommandResponse.Status.COMMITTED_RESPONSE_UNAVAILABLE,value.status())).verifyComplete();assertEquals(1,lease.sent.size());assertEquals("/api/v1/policies/"+policy+"/governed/issue",lease.sent.getFirst().path());assertDoesNotThrow(()->{var json=ApiClient.createDefaultMapper(null).readTree(lease.sent.getFirst().body());assertEquals(3,json.size());assertEquals(request.actionId().toString(),json.path("actionId").asText());assertEquals(1,json.path("expectedBindingVersion").asLong());assertEquals(DIGEST,json.path("bindingReceiptDigest").asText());});}
    @Test void actualGeneratedRecoveryObservesTheOriginalActionWithoutARetryKey(){var lease=new LeaseFixtureImpl("human-A");var action=UUID.randomUUID();lease.response="{\"state\":\"NOT_OBSERVED_AT_THIS_READ\",\"actionId\":\""+action+"\",\"intentDigest\":\""+DIGEST+"\"}";StepVerifier.create(new GeneratedGovernedPolicyDomainCommandsImpl(lease).recover(action,DIGEST)).assertNext(value->{assertEquals(GovernedPolicyRecoveryResponse.State.NOT_OBSERVED_AT_THIS_READ,value.state());assertEquals(action,value.actionId());}).verifyComplete();assertEquals(1,lease.sent.size());assertEquals("/api/v1/policies/governed/recovery/"+action,lease.sent.getFirst().path());assertEquals("intentDigest="+DIGEST,lease.sent.getFirst().query());assertNull(lease.sent.getFirst().retryKey());}
    @Test void possibleGeneratedDispatchLossCreatesOnlyTheUnknownMarkerWithoutRetry(){var lease=new LeaseFixtureImpl("human-A");lease.lost=true;StepVerifier.create(new GeneratedGovernedPolicyDomainCommandsImpl(lease).bind(request())).expectError(GovernedPolicyOutcomeUnknownException.class).verify();assertEquals(1,lease.sent.size());}
    @Test void generatedForbiddenAndPreworkUnavailableRemainKnownFailures(){
        var refused=new LeaseFixtureImpl("human-A");refused.status=HttpStatus.FORBIDDEN;refused.response="{}";
        StepVerifier.create(new GeneratedGovernedPolicyDomainCommandsImpl(refused).bind(request())).expectErrorSatisfies(error->assertEquals(HttpStatus.FORBIDDEN,((ResponseStatusException)error).getStatusCode())).verify();assertEquals(1,refused.sent.size());
        var unavailable=new LeaseFixtureImpl("human-A");unavailable.unavailable=true;
        StepVerifier.create(new GeneratedGovernedPolicyDomainCommandsImpl(unavailable).bind(request())).expectErrorSatisfies(error->assertEquals(HttpStatus.SERVICE_UNAVAILABLE,((ResponseStatusException)error).getStatusCode())).verify();assertTrue(unavailable.sent.isEmpty());
    }
    @Test void malformedSuccessAfterPossibleDispatchRequiresOriginalActionRecovery(){var lease=new LeaseFixtureImpl("human-A");lease.response="{\"status\":\"COMMITTED\"}";StepVerifier.create(new GeneratedGovernedPolicyDomainCommandsImpl(lease).bind(request())).expectError(GovernedPolicyOutcomeUnknownException.class).verify();assertEquals(1,lease.sent.size());}
    @Test void currentCallerRefusalPreventsAnyGeneratedRequest(){var lease=new LeaseFixtureImpl("human-A");lease.refused=true;StepVerifier.create(new GeneratedGovernedPolicyDomainCommandsImpl(lease).bind(request())).expectErrorSatisfies(error->assertEquals(HttpStatus.FORBIDDEN,((ResponseStatusException)error).getStatusCode())).verify();assertTrue(lease.sent.isEmpty());}
    @Test void lostCurrentAuthorityAfterDispatchWithholdsTheReceipt(){var lease=new LeaseFixtureImpl("human-A");lease.refuseAfterResponse=true;lease.response=command(UUID.randomUUID(),"OUTCOME_UNKNOWN");StepVerifier.create(new GeneratedGovernedPolicyDomainCommandsImpl(lease).bind(request())).expectErrorSatisfies(error->assertEquals(HttpStatus.FORBIDDEN,((ResponseStatusException)error).getStatusCode())).verify();assertEquals(1,lease.sent.size());}
    @Test void overlappingHumanLeasesUseDistinctGeneratedCredentials(){var a=new LeaseFixtureImpl("human-A");var b=new LeaseFixtureImpl("human-B");var action=UUID.randomUUID();a.response=b.response="{\"state\":\"NOT_OBSERVED_AT_THIS_READ\",\"actionId\":\""+action+"\",\"intentDigest\":\""+DIGEST+"\"}";StepVerifier.create(Mono.zip(new GeneratedGovernedPolicyDomainCommandsImpl(a).recover(action,DIGEST),new GeneratedGovernedPolicyDomainCommandsImpl(b).recover(action,DIGEST))).expectNextCount(1).verifyComplete();assertEquals(List.of("Bearer human-A"),a.sent.getFirst().authorization());assertEquals(List.of("Bearer human-B"),b.sent.getFirst().authorization());assertEquals(1,a.sent.size());assertEquals(1,b.sent.size());}
    @Test void actualGeneratedCommittedReceiptRetainsEveryTypedOwnerField(){var lease=new LeaseFixtureImpl("human-A");var request=request();var policy=UUID.randomUUID();var acceptance=UUID.randomUUID();lease.response="{\"status\":\"COMMITTED\",\"actionId\":\""+request.actionId()+"\",\"intentDigest\":\""+DIGEST+"\",\"result\":{\"policyId\":\""+policy+"\",\"policyNumber\":\"SANDBOX-1\",\"policyStatus\":\"BOUND\",\"bindingVersion\":1,\"bindingReceiptDigest\":\""+DIGEST+"\",\"acceptanceActionId\":\""+request.acceptanceActionId()+"\",\"acceptanceId\":\""+acceptance+"\",\"offerId\":\""+request.offerId()+"\",\"offerVersion\":1,\"termsDigest\":\""+DIGEST+"\"}}";StepVerifier.create(new GeneratedGovernedPolicyDomainCommandsImpl(lease).bind(request)).assertNext(value->{assertEquals(GovernedPolicyCommandResponse.Status.COMMITTED,value.status());assertEquals(new GovernedPolicyBindingResult(policy,"SANDBOX-1",GovernedPolicyBindingResult.PolicyState.BOUND,1,DIGEST,request.acceptanceActionId(),acceptance,request.offerId(),1,DIGEST),value.result());}).verifyComplete();assertEquals(1,lease.sent.size());}
    @Test void allThreeNonReceiptDomainOutcomesRemainDistinct(){for(var state:List.of("COMMITTED_RESPONSE_UNAVAILABLE","OUTCOME_UNKNOWN","REFUSED")){var lease=new LeaseFixtureImpl("human-A");var request=request();lease.response=command(request.actionId(),state);StepVerifier.create(new GeneratedGovernedPolicyDomainCommandsImpl(lease).bind(request)).assertNext(value->{assertEquals(state,value.status().name());assertEquals(request.actionId(),value.actionId());assertEquals(DIGEST,value.intentDigest());assertNull(value.result());}).verifyComplete();assertEquals(1,lease.sent.size());}}
    @Test void actualGeneratedBindPreparationReturnsOnlyTheOpaqueOwnerIntent(){var lease=new LeaseFixtureImpl("human-A");var request=request();lease.response="{\"operation\":\"BIND\",\"actionId\":\""+request.actionId()+"\",\"intentDigest\":\""+DIGEST+"\"}";StepVerifier.create(new GeneratedGovernedPolicyDomainCommandsImpl(lease).prepareBind(request)).assertNext(value->{assertEquals(GovernedPolicyPreparedIntent.Operation.BIND,value.operation());assertEquals(request.actionId(),value.actionId());assertEquals(DIGEST,value.intentDigest());}).verifyComplete();assertEquals(1,lease.sent.size());assertEquals("/api/v1/policies/governed/intents",lease.sent.getFirst().path());assertNull(lease.sent.getFirst().retryKey());}
    @Test void preparationTransportLossIsAnObservationFailureBeforeAnyMutation(){var lease=new LeaseFixtureImpl("human-A");lease.lost=true;StepVerifier.create(new GeneratedGovernedPolicyDomainCommandsImpl(lease).prepareBind(request())).expectErrorSatisfies(error->{assertInstanceOf(ResponseStatusException.class,error);assertFalse(error instanceof GovernedPolicyOutcomeUnknownException);}).verify();assertEquals(1,lease.sent.size());assertTrue(lease.sent.getFirst().path().endsWith("/intents"));}
}
