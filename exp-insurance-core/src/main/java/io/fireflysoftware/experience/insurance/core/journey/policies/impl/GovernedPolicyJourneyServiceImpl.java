package io.fireflysoftware.experience.insurance.core.journey.policies.impl;

import io.fireflysoftware.experience.insurance.core.journey.policies.*;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/** Read-only Domain preparation, then one Domain mutation; no Core dependency or business store. */
public final class GovernedPolicyJourneyServiceImpl implements GovernedPolicyJourneyService {
    private final GovernedPolicyDomainPort domain;
    public GovernedPolicyJourneyServiceImpl(GovernedPolicyDomainPort domain){this.domain=Objects.requireNonNull(domain);}
    @Override public Mono<GovernedPolicyPreparedIntent> prepare(GovernedPolicyIntentRequest request){return Mono.defer(()->{
        Objects.requireNonNull(request);var action=request.operation()==GovernedPolicyPreparedIntent.Operation.BIND?GovernedPolicyDomainPort.Action.BIND:GovernedPolicyDomainPort.Action.ISSUE;
        return current(action).flatMap(client->prepare(client,request.operation(),request.actionId(),()->request.operation()==GovernedPolicyPreparedIntent.Operation.BIND
                ?client.prepareBind(request.bindRequest()):client.prepareIssue(request.policyId(),request.issueRequest())));
    });}
    @Override public Mono<GovernedPolicyCommandResponse> bind(GovernedPolicyBindRequest request){return Mono.defer(()->{
        Objects.requireNonNull(request);
        return current(GovernedPolicyDomainPort.Action.BIND).flatMap(client->prepare(client,GovernedPolicyPreparedIntent.Operation.BIND,request.actionId(),()->client.prepareBind(request))
                .flatMap(intent->command(client,intent,()->client.bind(request),result->request.policyNumber().equals(result.policyNumber()) && request.offerId().equals(result.offerId())
                        && request.offerVersion()==result.offerVersion() && request.termsDigest().equals(result.termsDigest()) && request.acceptanceActionId().equals(result.acceptanceActionId()))));
    });}
    @Override public Mono<GovernedPolicyCommandResponse> issue(UUID policyId,GovernedPolicyIssueRequest request){return Mono.defer(()->{
        GovernedPolicyValues.id(policyId);Objects.requireNonNull(request);
        return current(GovernedPolicyDomainPort.Action.ISSUE).flatMap(client->prepare(client,GovernedPolicyPreparedIntent.Operation.ISSUE,request.actionId(),()->client.prepareIssue(policyId,request))
                .flatMap(intent->command(client,intent,()->client.issue(policyId,request),result->policyId.equals(result.policyId()))));
    });}
    @Override public Mono<GovernedPolicyRecoveryResponse> recover(UUID actionId,String digest){return Mono.defer(()->{
        GovernedPolicyValues.id(actionId);GovernedPolicyValues.digest(digest);
        return current(GovernedPolicyDomainPort.Action.RECOVER).flatMap(client->Mono.defer(client::revalidate).then(Mono.defer(()->client.recover(actionId,digest)))
                .switchIfEmpty(Mono.error(invalid())).flatMap(value->{
                    if(!actionId.equals(value.actionId()) || !digest.equals(value.intentDigest()))return Mono.error(invalid());
                    return Mono.defer(client::revalidate).thenReturn(value);
                }));
    });}
    private Mono<GovernedPolicyDomainPort.CallerBound> current(GovernedPolicyDomainPort.Action action){return Mono.defer(()->domain.current(action)).switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Trusted governed Domain policy client required")));}
    private Mono<GovernedPolicyPreparedIntent> prepare(GovernedPolicyDomainPort.CallerBound client,GovernedPolicyPreparedIntent.Operation operation,UUID actionId,
            Supplier<Mono<GovernedPolicyPreparedIntent>> preparation){
        return Mono.defer(client::revalidate).then(Mono.defer(preparation)).switchIfEmpty(Mono.error(invalid())).flatMap(value->{
            if(value.operation()!=operation || !actionId.equals(value.actionId()))return Mono.error(invalid());
            return Mono.defer(client::revalidate).thenReturn(value);
        });
    }
    private Mono<GovernedPolicyCommandResponse> command(GovernedPolicyDomainPort.CallerBound client,GovernedPolicyPreparedIntent intent,
            Supplier<Mono<GovernedPolicyCommandResponse>> dispatch,Predicate<GovernedPolicyBindingResult> matches){
        var unknown=new GovernedPolicyCommandResponse(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,intent.actionId(),intent.intentDigest(),null);
        return Mono.defer(dispatch).switchIfEmpty(Mono.just(unknown)).map(value->{
            if(!intent.actionId().equals(value.actionId()) || !intent.intentDigest().equals(value.intentDigest()) || value.result()!=null && !matches.test(value.result()))return unknown;
            return value;
        }).onErrorResume(GovernedPolicyOutcomeUnknownException.class,error->Mono.just(unknown)).flatMap(value->{
            if(value.status()!=GovernedPolicyCommandResponse.Status.COMMITTED)return Mono.just(value);
            return Mono.defer(client::revalidate).thenReturn(value).onErrorReturn(new GovernedPolicyCommandResponse(GovernedPolicyCommandResponse.Status.COMMITTED_RESPONSE_UNAVAILABLE,intent.actionId(),intent.intentDigest(),null));
        });
    }
    private static ResponseStatusException invalid(){return new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Exact original Domain policy intent observation required");}
}
