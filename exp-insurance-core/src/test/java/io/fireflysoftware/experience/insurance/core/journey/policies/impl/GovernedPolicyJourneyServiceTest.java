package io.fireflysoftware.experience.insurance.core.journey.policies.impl;

import io.fireflysoftware.experience.insurance.core.journey.policies.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.*;

class GovernedPolicyJourneyServiceTest {
    static final String CALLER="verified-human",OWNER_DIGEST="c".repeat(64),ACCEPTED_DIGEST="a".repeat(64);
    GovernedPolicyBindRequest request(){return new GovernedPolicyBindRequest(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),1,1,UUID.randomUUID(),ACCEPTED_DIGEST,ACCEPTED_DIGEST,UUID.randomUUID(),"SANDBOX-1",LocalDate.of(2027,1,1),LocalDate.of(2028,1,1));}
    GovernedPolicyBindingResult receipt(GovernedPolicyBindRequest request){return new GovernedPolicyBindingResult(UUID.randomUUID(),request.policyNumber(),GovernedPolicyBindingResult.PolicyState.BOUND,1,ACCEPTED_DIGEST,request.acceptanceActionId(),UUID.randomUUID(),request.offerId(),request.offerVersion(),request.termsDigest());}
    GovernedPolicyCommandResponse response(GovernedPolicyBindRequest request,GovernedPolicyCommandResponse.Status status){return new GovernedPolicyCommandResponse(status,request.actionId(),OWNER_DIGEST,status==GovernedPolicyCommandResponse.Status.COMMITTED?receipt(request):null);}
    static final class DomainFixtureImpl implements GovernedPolicyDomainPort {
        final List<Action> actions=new CopyOnWriteArrayList<>();final List<String> callers=new CopyOnWriteArrayList<>(),commandCallers=new CopyOnWriteArrayList<>();
        final List<GovernedPolicyBindRequest> preparedBinds=new CopyOnWriteArrayList<>(),binds=new CopyOnWriteArrayList<>();
        final List<UUID> preparedPolicies=new CopyOnWriteArrayList<>(),issuedPolicies=new CopyOnWriteArrayList<>(),recoveries=new CopyOnWriteArrayList<>();
        final List<String> recoveryDigests=new CopyOnWriteArrayList<>();
        Function<GovernedPolicyBindRequest,Mono<GovernedPolicyPreparedIntent>> preparing=body->Mono.just(new GovernedPolicyPreparedIntent(GovernedPolicyPreparedIntent.Operation.BIND,body.actionId(),OWNER_DIGEST));
        Function<GovernedPolicyBindRequest,Mono<GovernedPolicyCommandResponse>> binding;
        Function<GovernedPolicyIssueRequest,Mono<GovernedPolicyCommandResponse>> issuing;
        Function<UUID,Mono<GovernedPolicyRecoveryResponse>> recovering;
        RuntimeException authorityFailure;int revokeAtValidation=Integer.MAX_VALUE;Mono<Void> prepareGate=Mono.empty();
        @Override public Mono<CallerBound> current(Action action){return Mono.deferContextual(context->{
            if(authorityFailure!=null)return Mono.error(authorityFailure);
            String caller=context.get(CALLER);actions.add(action);callers.add(caller);
            return Mono.just(new CallerBound(){int validations;
                @Override public Mono<Void> revalidate(){return Mono.deferContextual(current->{
                    if(!caller.equals(current.getOrDefault(CALLER,"")) || ++validations>=revokeAtValidation)return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN));return Mono.empty();
                });}
                @Override public Mono<GovernedPolicyPreparedIntent> prepareBind(GovernedPolicyBindRequest request){return Mono.defer(()->{preparedBinds.add(request);return prepareGate.then(Mono.defer(()->preparing.apply(request)));});}
                @Override public Mono<GovernedPolicyPreparedIntent> prepareIssue(UUID policy,GovernedPolicyIssueRequest request){return Mono.defer(()->{preparedPolicies.add(policy);return Mono.just(new GovernedPolicyPreparedIntent(GovernedPolicyPreparedIntent.Operation.ISSUE,request.actionId(),OWNER_DIGEST));});}
                @Override public Mono<GovernedPolicyCommandResponse> bind(GovernedPolicyBindRequest request){return Mono.defer(()->{binds.add(request);commandCallers.add(caller);return binding.apply(request);});}
                @Override public Mono<GovernedPolicyCommandResponse> issue(UUID policy,GovernedPolicyIssueRequest request){return Mono.defer(()->{issuedPolicies.add(policy);commandCallers.add(caller);return issuing.apply(request);});}
                @Override public Mono<GovernedPolicyRecoveryResponse> recover(UUID action,String digest){return Mono.defer(()->{recoveries.add(action);recoveryDigests.add(digest);return recovering.apply(action);});}
            });
        });}
    }
    <T> Mono<T> caller(Mono<T> work,String caller){return work.contextWrite(context->context.put(CALLER,caller));}
    @Test void ownerPreparationPrecedesExactlyOneBindUsingTheSameImmutableRequest(){
        var domain=new DomainFixtureImpl();var request=request();domain.binding=body->{assertEquals(List.of(request),domain.preparedBinds);assertSame(request,body);return Mono.just(response(body,GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN));};
        StepVerifier.create(caller(new GovernedPolicyJourneyServiceImpl(domain).bind(request),"human-A")).assertNext(value->{assertEquals(request.actionId(),value.actionId());assertEquals(OWNER_DIGEST,value.intentDigest());}).verifyComplete();assertEquals(List.of(request),domain.binds);assertSame(request,domain.preparedBinds.getFirst());
    }
    @Test void allFourDomainOutcomesKeepTheOwnersOpaqueDigest(){
        var domain=new DomainFixtureImpl();var request=request();var service=new GovernedPolicyJourneyServiceImpl(domain);
        for(var status:GovernedPolicyCommandResponse.Status.values()){domain.binding=body->Mono.just(response(body,status));StepVerifier.create(caller(service.bind(request),"human-A")).assertNext(value->{assertEquals(status,value.status());assertEquals(OWNER_DIGEST,value.intentDigest());assertEquals(request.actionId(),value.actionId());}).verifyComplete();}assertEquals(4,domain.binds.size());
    }
    @Test void currentAuthorityRefusalCannotPrepareOrMutate(){
        var domain=new DomainFixtureImpl();domain.authorityFailure=new ResponseStatusException(HttpStatus.FORBIDDEN);
        StepVerifier.create(caller(new GovernedPolicyJourneyServiceImpl(domain).bind(request()),"staff-cannot-be-human")).expectErrorSatisfies(error->assertSame(domain.authorityFailure,error)).verify();assertTrue(domain.preparedBinds.isEmpty());assertTrue(domain.binds.isEmpty());
    }
    @Test void missingCallerBoundDomainRefusesBeforeAnyPreparation(){StepVerifier.create(new GovernedPolicyJourneyServiceImpl(action->Mono.empty()).bind(request())).expectErrorSatisfies(error->assertEquals(HttpStatus.SERVICE_UNAVAILABLE,((ResponseStatusException)error).getStatusCode())).verify();}
    @Test void losingTheReadOnlyPreparationCannotBecomeAnUnknownMutation(){
        var domain=new DomainFixtureImpl();var failure=new IOException("controlled preparation transport loss");domain.preparing=body->Mono.error(failure);
        StepVerifier.create(caller(new GovernedPolicyJourneyServiceImpl(domain).bind(request()),"human-A")).expectErrorSatisfies(error->assertSame(failure,error)).verify();assertEquals(1,domain.preparedBinds.size());assertTrue(domain.binds.isEmpty());
    }
    @Test void absentPreparationCannotStartTheMutation(){
        var domain=new DomainFixtureImpl();domain.preparing=body->Mono.empty();StepVerifier.create(caller(new GovernedPolicyJourneyServiceImpl(domain).bind(request()),"human-A")).expectErrorSatisfies(error->assertEquals(HttpStatus.BAD_GATEWAY,((ResponseStatusException)error).getStatusCode())).verify();assertTrue(domain.binds.isEmpty());
    }
    @Test void anotherPreparedActionCannotStartTheMutation(){
        var domain=new DomainFixtureImpl();domain.preparing=body->Mono.just(new GovernedPolicyPreparedIntent(GovernedPolicyPreparedIntent.Operation.BIND,UUID.randomUUID(),OWNER_DIGEST));StepVerifier.create(caller(new GovernedPolicyJourneyServiceImpl(domain).bind(request()),"human-A")).expectError(ResponseStatusException.class).verify();assertTrue(domain.binds.isEmpty());
    }
    @Test void anotherPreparedOperationCannotStartTheMutation(){
        var domain=new DomainFixtureImpl();domain.preparing=body->Mono.just(new GovernedPolicyPreparedIntent(GovernedPolicyPreparedIntent.Operation.ISSUE,body.actionId(),OWNER_DIGEST));StepVerifier.create(caller(new GovernedPolicyJourneyServiceImpl(domain).bind(request()),"human-A")).expectError(ResponseStatusException.class).verify();assertTrue(domain.binds.isEmpty());
    }
    @Test void expiredAuthorityAfterPreparationCannotDispatchTheMutation(){
        var domain=new DomainFixtureImpl();domain.revokeAtValidation=2;StepVerifier.create(caller(new GovernedPolicyJourneyServiceImpl(domain).bind(request()),"human-A")).expectError(ResponseStatusException.class).verify();assertEquals(1,domain.preparedBinds.size());assertTrue(domain.binds.isEmpty());
    }
    @Test void uncertainDomainDispatchKeepsThePreparedIdentityWithoutRetry(){
        var domain=new DomainFixtureImpl();var request=request();domain.binding=body->Mono.error(new GovernedPolicyOutcomeUnknownException());
        StepVerifier.create(caller(new GovernedPolicyJourneyServiceImpl(domain).bind(request),"human-A")).assertNext(value->{assertEquals(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,value.status());assertEquals(request.actionId(),value.actionId());assertEquals(OWNER_DIGEST,value.intentDigest());assertNull(value.result());}).verifyComplete();assertEquals(List.of(request),domain.binds);assertTrue(domain.recoveries.isEmpty());
    }
    @Test void unrelatedCommandDigestCannotInventSuccessOrReplaceTheOwnerDigest(){
        var domain=new DomainFixtureImpl();var request=request();domain.binding=body->Mono.just(new GovernedPolicyCommandResponse(GovernedPolicyCommandResponse.Status.COMMITTED,body.actionId(),"d".repeat(64),receipt(body)));
        StepVerifier.create(caller(new GovernedPolicyJourneyServiceImpl(domain).bind(request),"human-A")).assertNext(value->{assertEquals(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,value.status());assertEquals(OWNER_DIGEST,value.intentDigest());assertNull(value.result());}).verifyComplete();assertEquals(1,domain.binds.size());
    }
    @Test void aCommittedReceiptIsWithheldWhenTheCapturedCallerExpires(){
        var domain=new DomainFixtureImpl();var request=request();domain.revokeAtValidation=3;domain.binding=body->Mono.just(response(body,GovernedPolicyCommandResponse.Status.COMMITTED));
        StepVerifier.create(caller(new GovernedPolicyJourneyServiceImpl(domain).bind(request),"human-A")).assertNext(value->{assertEquals(GovernedPolicyCommandResponse.Status.COMMITTED_RESPONSE_UNAVAILABLE,value.status());assertEquals(OWNER_DIGEST,value.intentDigest());assertNull(value.result());}).verifyComplete();assertEquals(1,domain.binds.size());
    }
    @Test void activationPreparesTheExistingPolicyBeforeExactlyOneActivation(){
        var domain=new DomainFixtureImpl();var policy=UUID.randomUUID();var request=new GovernedPolicyIssueRequest(UUID.randomUUID(),1,ACCEPTED_DIGEST);domain.issuing=body->Mono.just(new GovernedPolicyCommandResponse(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,body.actionId(),OWNER_DIGEST,null));
        StepVerifier.create(caller(new GovernedPolicyJourneyServiceImpl(domain).issue(policy,request),"human-A")).assertNext(value->{assertEquals(request.actionId(),value.actionId());assertEquals(OWNER_DIGEST,value.intentDigest());}).verifyComplete();assertEquals(List.of(policy),domain.preparedPolicies);assertEquals(List.of(policy),domain.issuedPolicies);assertTrue(domain.binds.isEmpty());
    }
    @Test void recoveryUsesTheOriginalActionAndOwnerDigestWithoutPreparationOrMutation(){
        var domain=new DomainFixtureImpl();var action=UUID.randomUUID();domain.recovering=id->Mono.just(new GovernedPolicyRecoveryResponse(GovernedPolicyRecoveryResponse.State.NOT_OBSERVED_AT_THIS_READ,id,OWNER_DIGEST,null));
        StepVerifier.create(caller(new GovernedPolicyJourneyServiceImpl(domain).recover(action,OWNER_DIGEST),"human-A")).assertNext(value->assertEquals(GovernedPolicyRecoveryResponse.State.NOT_OBSERVED_AT_THIS_READ,value.state())).verifyComplete();assertEquals(List.of(action),domain.recoveries);assertEquals(List.of(OWNER_DIGEST),domain.recoveryDigests);assertTrue(domain.preparedBinds.isEmpty());assertTrue(domain.preparedPolicies.isEmpty());assertTrue(domain.binds.isEmpty());assertTrue(domain.issuedPolicies.isEmpty());
    }
    @Test void recoveryFailureCannotBecomeAbsenceOrStartAnotherAction(){
        var domain=new DomainFixtureImpl();var failure=new ResponseStatusException(HttpStatus.FORBIDDEN);domain.recovering=id->Mono.error(failure);StepVerifier.create(caller(new GovernedPolicyJourneyServiceImpl(domain).recover(UUID.randomUUID(),OWNER_DIGEST),"human-A")).expectErrorSatisfies(error->assertSame(failure,error)).verify();assertEquals(1,domain.recoveries.size());assertTrue(domain.binds.isEmpty());
    }
    @Test void overlappingHumansKeepDistinctCallerBoundPreparationAndCommands(){
        var domain=new DomainFixtureImpl();var gate=Sinks.<Void>empty();domain.prepareGate=gate.asMono();domain.binding=body->Mono.just(response(body,GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN));var service=new GovernedPolicyJourneyServiceImpl(domain);var left=request();var right=request();
        StepVerifier.create(Mono.zip(caller(service.bind(left),"human-A"),caller(service.bind(right),"human-B"))).expectSubscription().then(()->{assertEquals(List.of("human-A","human-B"),domain.callers);assertTrue(domain.binds.isEmpty());gate.tryEmitEmpty();}).assertNext(values->{assertEquals(left.actionId(),values.getT1().actionId());assertEquals(right.actionId(),values.getT2().actionId());}).verifyComplete();assertEquals(Set.of("human-A","human-B"),Set.copyOf(domain.commandCallers));assertEquals(2,domain.binds.size());
    }
}
