package io.fireflysoftware.experience.insurance.web.policies.impl;

import io.fireflysoftware.experience.insurance.core.journey.policies.GovernedPolicyCommandResponse;
import io.fireflysoftware.experience.insurance.core.journey.policies.GovernedPolicyIssueRequest;
import io.fireflysoftware.experience.insurance.core.journey.policies.GovernedPolicyRecoveryResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.*;

class GeneratedPolicyDomainWriteAmbiguityTest {
    private final GeneratedGovernedPolicyDomainPortTest fixtures=new GeneratedGovernedPolicyDomainPortTest();
    private static final String DIGEST=GeneratedGovernedPolicyDomainPortTest.DIGEST;
    @Test void received503AfterBindKeepsThePreparedOwnerDigestWithoutAnotherMutation(){
        var factory=new GeneratedGovernedPolicyDomainPortTest.FactoryFixtureImpl();var request=fixtures.request();
        factory.prepareResponse=fixtures.prepared(request.actionId(),"BIND");factory.commandStatus=HttpStatus.SERVICE_UNAVAILABLE;factory.commandResponse="{}";
        StepVerifier.create(fixtures.caller(fixtures.service(factory).bind(request),"human-A")).assertNext(value->{
            assertEquals(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,value.status());assertEquals(request.actionId(),value.actionId());assertEquals(DIGEST,value.intentDigest());assertNull(value.result());
        }).verifyComplete();
        assertEquals(2,factory.sent.size());assertEquals("/api/v1/policies/governed/intents",factory.sent.getFirst().path());assertEquals("/api/v1/policies/governed/bindings",factory.sent.getLast().path());
    }
    @Test void received503AfterIssueKeepsThePreparedOwnerDigestWithoutAnotherMutation(){
        var factory=new GeneratedGovernedPolicyDomainPortTest.FactoryFixtureImpl();var policy=UUID.randomUUID();var request=new GovernedPolicyIssueRequest(UUID.randomUUID(),1,DIGEST);
        factory.prepareResponse=fixtures.prepared(request.actionId(),"ISSUE");factory.commandStatus=HttpStatus.SERVICE_UNAVAILABLE;factory.commandResponse="{}";
        StepVerifier.create(fixtures.caller(fixtures.service(factory).issue(policy,request),"human-A")).assertNext(value->{
            assertEquals(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,value.status());assertEquals(request.actionId(),value.actionId());assertEquals(DIGEST,value.intentDigest());assertNull(value.result());
        }).verifyComplete();
        assertEquals(2,factory.sent.size());assertEquals("/api/v1/policies/"+policy+"/governed/issue",factory.sent.getLast().path());
    }
    @Test void aNewJourneyServiceRecoversOnlyTheRetainedIntentAfterAmbiguousWrite(){
        var first=new GeneratedGovernedPolicyDomainPortTest.FactoryFixtureImpl();var request=fixtures.request();
        first.prepareResponse=fixtures.prepared(request.actionId(),"BIND");first.commandStatus=HttpStatus.SERVICE_UNAVAILABLE;first.commandResponse="{}";
        var original=fixtures.caller(fixtures.service(first).bind(request),"human-A").block();assertNotNull(original);assertEquals(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,original.status());
        var restarted=new GeneratedGovernedPolicyDomainPortTest.FactoryFixtureImpl();
        restarted.recoveryResponse="{\"state\":\"NOT_OBSERVED_AT_THIS_READ\",\"actionId\":\""+original.actionId()+"\",\"intentDigest\":\""+original.intentDigest()+"\"}";
        StepVerifier.create(fixtures.caller(fixtures.service(restarted).recover(original.actionId(),original.intentDigest()),"human-A")).assertNext(value->{
            assertEquals(GovernedPolicyRecoveryResponse.State.NOT_OBSERVED_AT_THIS_READ,value.state());assertEquals(request.actionId(),value.actionId());assertEquals(DIGEST,value.intentDigest());assertNull(value.result());
        }).verifyComplete();
        assertEquals(1,restarted.sent.size());assertEquals("/api/v1/policies/governed/recovery/"+request.actionId(),restarted.sent.getFirst().path());assertEquals(2,first.sent.size());
    }
}
