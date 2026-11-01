package io.fireflysoftware.experience.insurance.core.journey.policies.impl;

import io.fireflysoftware.experience.insurance.core.journey.policies.*;
import java.io.IOException;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.*;

class TerminalPolicyIntentPreparationTest {
    final GovernedPolicyJourneyServiceTest fixture=new GovernedPolicyJourneyServiceTest();
    <T> Mono<T> caller(Mono<T> work){return fixture.caller(work,"human-A");}
    @Test void terminalCanRetainOwnerBindIntentBeforeAnyMutation(){var d=new GovernedPolicyJourneyServiceTest.DomainFixtureImpl();var r=fixture.request();StepVerifier.create(caller(new GovernedPolicyJourneyServiceImpl(d).prepare(new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.BIND,r,null,null)))).assertNext(value->{assertEquals(r.actionId(),value.actionId());assertEquals(GovernedPolicyJourneyServiceTest.OWNER_DIGEST,value.intentDigest());assertEquals(GovernedPolicyPreparedIntent.Operation.BIND,value.operation());}).verifyComplete();assertEquals(List.of(r),d.preparedBinds);assertTrue(d.binds.isEmpty());assertTrue(d.issuedPolicies.isEmpty());assertTrue(d.recoveries.isEmpty());}
    @Test void terminalIssuePreparationKeepsTheExistingPolicyWithoutActivating(){var d=new GovernedPolicyJourneyServiceTest.DomainFixtureImpl();var policy=UUID.randomUUID();var r=new GovernedPolicyIssueRequest(UUID.randomUUID(),1,GovernedPolicyJourneyServiceTest.ACCEPTED_DIGEST);StepVerifier.create(caller(new GovernedPolicyJourneyServiceImpl(d).prepare(new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.ISSUE,null,policy,r)))).assertNext(value->{assertEquals(r.actionId(),value.actionId());assertEquals(GovernedPolicyPreparedIntent.Operation.ISSUE,value.operation());}).verifyComplete();assertEquals(List.of(policy),d.preparedPolicies);assertTrue(d.issuedPolicies.isEmpty());assertTrue(d.binds.isEmpty());}
    @Test void currentAuthorityRefusalCannotPrepareOrMutate(){var d=new GovernedPolicyJourneyServiceTest.DomainFixtureImpl();d.authorityFailure=new ResponseStatusException(HttpStatus.FORBIDDEN);StepVerifier.create(caller(new GovernedPolicyJourneyServiceImpl(d).prepare(new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.BIND,fixture.request(),null,null)))).expectErrorSatisfies(error->assertSame(d.authorityFailure,error)).verify();assertTrue(d.preparedBinds.isEmpty());assertTrue(d.binds.isEmpty());}
    @Test void preparationFailureCannotBecomeAnUnknownMutation(){var d=new GovernedPolicyJourneyServiceTest.DomainFixtureImpl();var failure=new IOException("controlled read-only preparation loss");d.preparing=r->Mono.error(failure);StepVerifier.create(caller(new GovernedPolicyJourneyServiceImpl(d).prepare(new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.BIND,fixture.request(),null,null)))).expectErrorSatisfies(error->assertSame(failure,error)).verify();assertTrue(d.binds.isEmpty());assertTrue(d.issuedPolicies.isEmpty());}
    @Test void anotherPreparedActionCannotBeReleasedToTheTerminal(){var d=new GovernedPolicyJourneyServiceTest.DomainFixtureImpl();d.preparing=r->Mono.just(new GovernedPolicyPreparedIntent(GovernedPolicyPreparedIntent.Operation.BIND,UUID.randomUUID(),GovernedPolicyJourneyServiceTest.OWNER_DIGEST));StepVerifier.create(caller(new GovernedPolicyJourneyServiceImpl(d).prepare(new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.BIND,fixture.request(),null,null)))).expectError(ResponseStatusException.class).verify();assertTrue(d.binds.isEmpty());}
    @Test void expiredCurrentCallerCannotReceiveAnIntentPreparation(){var d=new GovernedPolicyJourneyServiceTest.DomainFixtureImpl();d.revokeAtValidation=2;StepVerifier.create(caller(new GovernedPolicyJourneyServiceImpl(d).prepare(new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.BIND,fixture.request(),null,null)))).expectErrorSatisfies(error->assertEquals(HttpStatus.FORBIDDEN,((ResponseStatusException)error).getStatusCode())).verify();assertEquals(1,d.preparedBinds.size());assertTrue(d.binds.isEmpty());}
}
