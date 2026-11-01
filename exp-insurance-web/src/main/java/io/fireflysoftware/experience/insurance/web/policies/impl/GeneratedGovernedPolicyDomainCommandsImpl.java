package io.fireflysoftware.experience.insurance.web.policies.impl;

import io.fireflysoftware.domain.insurance.policies.sdk.api.GovernedPoliciesApi;
import io.fireflysoftware.domain.insurance.policies.sdk.invoker.ApiClient;
import io.fireflysoftware.experience.insurance.core.journey.policies.*;
import io.fireflysoftware.experience.insurance.web.policies.GovernedPolicyDomainClientFactory;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/** Typed command mapping from the observed original Domain generator; the port must prepare its original intent first. */
public final class GeneratedGovernedPolicyDomainCommandsImpl {
    private final GovernedPolicyDomainClientFactory.Client client;
    private final GovernedPoliciesApi api;
    public GeneratedGovernedPolicyDomainCommandsImpl(GovernedPolicyDomainClientFactory.Client client){
        this.client=Objects.requireNonNull(client);
        var transport=ApiClient.buildWebClientBuilder().clientConnector(client.connector()).codecs(codecs->{codecs.defaultCodecs().maxInMemorySize(1024*1024);codecs.defaultCodecs().enableLoggingRequestDetails(false);}).build();
        var generated=new ApiClient(transport).setBasePath(client.endpoint().toASCIIString());generated.setBearerToken(client.bearerToken());api=new GovernedPoliciesApi(generated);
    }
    public Mono<GovernedPolicyPreparedIntent> prepareBind(GovernedPolicyBindRequest request){return Mono.defer(()->{
        var input=new io.fireflysoftware.domain.insurance.policies.sdk.model.GovernedPolicyIntentRequest()
                .operation(io.fireflysoftware.domain.insurance.policies.sdk.model.GovernedPolicyIntentRequest.OperationEnum.BIND).bindRequest(bindInput(request));
        return prepare(input);
    });}
    public Mono<GovernedPolicyPreparedIntent> prepareIssue(UUID policyId,GovernedPolicyIssueRequest request){return Mono.defer(()->{
        var input=new io.fireflysoftware.domain.insurance.policies.sdk.model.GovernedPolicyIntentRequest()
                .operation(io.fireflysoftware.domain.insurance.policies.sdk.model.GovernedPolicyIntentRequest.OperationEnum.ISSUE).policyId(Objects.requireNonNull(policyId)).issueRequest(issueInput(request));
        return prepare(input);
    });}
    public Mono<GovernedPolicyCommandResponse> bind(GovernedPolicyBindRequest request){return Mono.defer(()->{
        var input=bindInput(request);return write(()->api.bindGovernedPolicyWithHttpInfo(input));
    });}
    public Mono<GovernedPolicyCommandResponse> issue(UUID policyId,GovernedPolicyIssueRequest request){return Mono.defer(()->{
        Objects.requireNonNull(policyId);var input=issueInput(request);return write(()->api.issueGovernedPolicyWithHttpInfo(policyId,input));
    });}
    private Mono<GovernedPolicyPreparedIntent> prepare(io.fireflysoftware.domain.insurance.policies.sdk.model.GovernedPolicyIntentRequest input){
        return Mono.defer(()->client.revalidate().then(Mono.defer(()->
                Mono.defer(()->api.prepareGovernedPolicyIntentWithHttpInfo(input)).timeout(client.remaining())
                        .flatMap(reply->reply.getStatusCode().value()==200 && reply.getBody()!=null?Mono.just(reply.getBody()):Mono.error(unavailable()))
                        .map(value->new GovernedPolicyPreparedIntent(GovernedPolicyPreparedIntent.Operation.valueOf(value.getOperation().getValue()),value.getActionId(),value.getIntentDigest()))
                        .switchIfEmpty(Mono.error(unavailable())).onErrorMap(GeneratedGovernedPolicyDomainCommandsImpl::readFailure)))
                .flatMap(value->client.revalidate().thenReturn(value)));
    }
    private static io.fireflysoftware.domain.insurance.policies.sdk.model.GovernedPolicyBindRequest bindInput(GovernedPolicyBindRequest request){
        Objects.requireNonNull(request);
        return new io.fireflysoftware.domain.insurance.policies.sdk.model.GovernedPolicyBindRequest()
                .actionId(request.actionId()).submissionId(request.submissionId()).partyId(request.partyId()).productId(request.productId())
                .offerId(request.offerId()).offerVersion(request.offerVersion()).acceptedControlGeneration(request.acceptedControlGeneration())
                .acceptanceActionId(request.acceptanceActionId()).acceptanceIntentDigest(request.acceptanceIntentDigest()).termsDigest(request.termsDigest())
                .contractId(request.contractId()).policyNumber(request.policyNumber()).inceptionDate(request.inceptionDate()).expiryDate(request.expiryDate());
    }
    private static io.fireflysoftware.domain.insurance.policies.sdk.model.GovernedPolicyIssueRequest issueInput(GovernedPolicyIssueRequest request){
        Objects.requireNonNull(request);
        return new io.fireflysoftware.domain.insurance.policies.sdk.model.GovernedPolicyIssueRequest()
                .actionId(request.actionId()).expectedBindingVersion(request.expectedBindingVersion()).bindingReceiptDigest(request.bindingReceiptDigest());
    }
    public Mono<GovernedPolicyRecoveryResponse> recover(UUID actionId,String digest){return Mono.defer(()->client.revalidate().then(Mono.defer(()->
        Mono.defer(()->api.recoverGovernedPolicyBindingWithHttpInfo(actionId,digest,null)).timeout(client.remaining())
                .flatMap(reply->reply.getStatusCode().value()==200 && reply.getBody()!=null?Mono.just(reply.getBody()):Mono.error(unavailable()))
                .map(value->new GovernedPolicyRecoveryResponse(GovernedPolicyRecoveryResponse.State.valueOf(value.getState().getValue()),value.getActionId(),value.getIntentDigest(),result(value.getResult())))
                .switchIfEmpty(Mono.error(unavailable())).onErrorMap(GeneratedGovernedPolicyDomainCommandsImpl::readFailure)))
                .flatMap(value->client.revalidate().thenReturn(value)));}
    private Mono<GovernedPolicyCommandResponse> write(Supplier<Mono<ResponseEntity<io.fireflysoftware.domain.insurance.policies.sdk.model.GovernedPolicyCommandResponse>>> dispatch){
        return Mono.defer(()->client.revalidate().then(Mono.defer(()->{
            var remaining=client.remaining();
            return Mono.defer(dispatch).timeout(remaining)
                    .flatMap(reply->reply.getStatusCode().value()==200 && reply.getBody()!=null?Mono.just(reply.getBody()):Mono.error(new GovernedPolicyOutcomeUnknownException()))
                    .map(value->new GovernedPolicyCommandResponse(GovernedPolicyCommandResponse.Status.valueOf(value.getStatus().getValue()),value.getActionId(),value.getIntentDigest(),result(value.getResult())))
                    .switchIfEmpty(Mono.error(new GovernedPolicyOutcomeUnknownException())).onErrorMap(error->{
                        if(error instanceof WebClientResponseException response && response.getStatusCode().value()!=503 && knownRefusal(response))return new ResponseStatusException(response.getStatusCode(),"Governed policy Domain request refused");
                        return new GovernedPolicyOutcomeUnknownException();
                    });
        })).flatMap(value->client.revalidate().thenReturn(value).onErrorResume(error->value.status()==GovernedPolicyCommandResponse.Status.COMMITTED
                ?Mono.just(new GovernedPolicyCommandResponse(GovernedPolicyCommandResponse.Status.COMMITTED_RESPONSE_UNAVAILABLE,value.actionId(),value.intentDigest(),null)):Mono.error(error))));
    }
    private static GovernedPolicyBindingResult result(io.fireflysoftware.domain.insurance.policies.sdk.model.GovernedPolicyBindingResult value){
        if(value==null)return null;
        return new GovernedPolicyBindingResult(value.getPolicyId(),value.getPolicyNumber(),GovernedPolicyBindingResult.PolicyState.valueOf(value.getPolicyStatus().getValue()),
                value.getBindingVersion(),value.getBindingReceiptDigest(),value.getAcceptanceActionId(),value.getAcceptanceId(),value.getOfferId(),value.getOfferVersion(),value.getTermsDigest());
    }
    private static boolean knownRefusal(WebClientResponseException response){return response.getStatusCode().is4xxClientError() && response.getStatusCode().value()!=408 || response.getStatusCode().value()==503;}
    private static Throwable readFailure(Throwable error){
        if(error instanceof ResponseStatusException)return error;
        if(error instanceof WebClientResponseException response && knownRefusal(response))return new ResponseStatusException(response.getStatusCode(),"Governed policy Domain observation refused");
        return unavailable();
    }
    private static ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Governed policy Domain observation unavailable");}
    @Override public String toString(){return "GeneratedGovernedPolicyDomainCommandsImpl[redacted]";}
}
