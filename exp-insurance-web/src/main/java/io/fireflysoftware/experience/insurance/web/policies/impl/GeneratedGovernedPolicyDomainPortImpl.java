package io.fireflysoftware.experience.insurance.web.policies.impl;

import io.fireflysoftware.experience.insurance.core.journey.policies.*;
import io.fireflysoftware.experience.insurance.web.policies.GovernedPolicyDomainClientFactory;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/** One caller-owned lease and immutable original selector; preparation grants no authority. */
public final class GeneratedGovernedPolicyDomainPortImpl implements GovernedPolicyDomainPort {
    private final GovernedPolicyDomainClientFactory clients;
    public GeneratedGovernedPolicyDomainPortImpl(GovernedPolicyDomainClientFactory clients){this.clients=Objects.requireNonNull(clients);}
    @Override public Mono<CallerBound> current(Action action){return Mono.defer(()->clients.current(Objects.requireNonNull(action)).map(client->new CallerBoundImpl(action,client)));}
    private record Selector(Action action,UUID actionId,UUID policyId,GovernedPolicyBindRequest bind,GovernedPolicyIssueRequest issue,String recoveryDigest){}
    private static final class CallerBoundImpl implements CallerBound {
        private final Action action;private final GovernedPolicyDomainClientFactory.Client client;private final GeneratedGovernedPolicyDomainCommandsImpl commands;
        private final AtomicReference<Selector> original=new AtomicReference<>();private final AtomicReference<GovernedPolicyPreparedIntent> prepared=new AtomicReference<>();
        private final AtomicBoolean dispatched=new AtomicBoolean();
        CallerBoundImpl(Action action,GovernedPolicyDomainClientFactory.Client client){this.action=action;this.client=client;commands=new GeneratedGovernedPolicyDomainCommandsImpl(client);}
        @Override public Mono<Void> revalidate(){return Mono.defer(client::revalidate);}
        @Override public Mono<GovernedPolicyPreparedIntent> prepareBind(GovernedPolicyBindRequest request){return Mono.defer(()->{
            Objects.requireNonNull(request);var selector=new Selector(Action.BIND,request.actionId(),null,request,null,null);capture(selector);
            return commands.prepareBind(request).map(value->accept(selector,value));
        });}
        @Override public Mono<GovernedPolicyPreparedIntent> prepareIssue(UUID policyId,GovernedPolicyIssueRequest request){return Mono.defer(()->{
            Objects.requireNonNull(policyId);Objects.requireNonNull(request);var selector=new Selector(Action.ISSUE,request.actionId(),policyId,null,request,null);capture(selector);
            return commands.prepareIssue(policyId,request).map(value->accept(selector,value));
        });}
        @Override public Mono<GovernedPolicyCommandResponse> bind(GovernedPolicyBindRequest request){return Mono.defer(()->{
            Objects.requireNonNull(request);dispatch(new Selector(Action.BIND,request.actionId(),null,request,null,null));return commands.bind(request);
        });}
        @Override public Mono<GovernedPolicyCommandResponse> issue(UUID policyId,GovernedPolicyIssueRequest request){return Mono.defer(()->{
            Objects.requireNonNull(policyId);Objects.requireNonNull(request);dispatch(new Selector(Action.ISSUE,request.actionId(),policyId,null,request,null));return commands.issue(policyId,request);
        });}
        @Override public Mono<GovernedPolicyRecoveryResponse> recover(UUID actionId,String digest){return Mono.defer(()->{
            if(actionId==null || actionId.equals(new UUID(0,0)) || digest==null || !digest.matches("[0-9a-f]{64}"))throw conflict();
            capture(new Selector(Action.RECOVER,actionId,null,null,null,digest));return commands.recover(actionId,digest);
        });}
        private void capture(Selector selector){
            if(action!=selector.action())throw conflict();original.compareAndSet(null,selector);if(!selector.equals(original.get()))throw conflict();
        }
        private GovernedPolicyPreparedIntent accept(Selector selector,GovernedPolicyPreparedIntent value){
            if(!selector.equals(original.get()) || !selector.actionId().equals(value.actionId()) || !selector.action().name().equals(value.operation().name()))throw conflict();
            prepared.compareAndSet(null,value);if(!value.equals(prepared.get()))throw conflict();return value;
        }
        private void dispatch(Selector selector){
            if(action!=selector.action() || !selector.equals(original.get()) || prepared.get()==null || !dispatched.compareAndSet(false,true))throw conflict();
        }
        @Override public String toString(){return "GeneratedGovernedPolicyCallerBound[redacted]";}
    }
    private static ResponseStatusException conflict(){return new ResponseStatusException(HttpStatus.CONFLICT,"Exact original prepared Domain policy action required");}
    @Override public String toString(){return "GeneratedGovernedPolicyDomainPortImpl[redacted]";}
}
