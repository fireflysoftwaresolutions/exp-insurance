package io.fireflysoftware.experience.insurance.web.controllers.impl;

import io.fireflysoftware.experience.insurance.core.journey.policies.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.fireflyframework.web.idempotency.annotation.DisableIdempotency;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/experience/insurance/governed/policies")
@Tag(name="GovernedInsurancePolicies")
@SecurityScheme(name="insuranceBearer",type=SecuritySchemeType.HTTP,scheme="bearer",bearerFormat="JWT")
@SecurityRequirement(name="insuranceBearer")
public final class GovernedPolicyJourneyControllerImpl {
    private final ObjectProvider<GovernedPolicyJourneyService> journeys;
    public GovernedPolicyJourneyControllerImpl(ObjectProvider<GovernedPolicyJourneyService> journeys){this.journeys=journeys;}
    @Operation(operationId="prepareGovernedInsurancePolicyIntent",summary="Observe the original Domain intent before its first mutation without reserving or granting authority")
    @ApiResponse(responseCode="200",content=@Content(mediaType=MediaType.APPLICATION_JSON_VALUE,schema=@Schema(implementation=GovernedPolicyPreparedIntent.class)))
    @DisableIdempotency
    @PostMapping(value="/intents",consumes=MediaType.APPLICATION_JSON_VALUE,produces=MediaType.APPLICATION_JSON_VALUE)
    public Mono<GovernedPolicyPreparedIntent> prepare(@RequestBody GovernedPolicyIntentRequest request){return Mono.defer(()->journey().prepare(request));}
    @Operation(operationId="bindGovernedInsurancePolicy",summary="Prepare the original policy intent then bind through its Domain")
    @ApiResponse(responseCode="200",content=@Content(mediaType=MediaType.APPLICATION_JSON_VALUE,schema=@Schema(implementation=GovernedPolicyCommandResponse.class)))
    @DisableIdempotency
    @PostMapping(value="/bindings",consumes=MediaType.APPLICATION_JSON_VALUE,produces=MediaType.APPLICATION_JSON_VALUE)
    public Mono<GovernedPolicyCommandResponse> bind(@RequestBody GovernedPolicyBindRequest request){return Mono.defer(()->journey().bind(request));}
    @Operation(operationId="issueGovernedInsurancePolicy",summary="Prepare then activate an existing bound sandbox policy through its Domain")
    @ApiResponse(responseCode="200",content=@Content(mediaType=MediaType.APPLICATION_JSON_VALUE,schema=@Schema(implementation=GovernedPolicyCommandResponse.class)))
    @DisableIdempotency
    @PostMapping(value="/{policyId}/issue",consumes=MediaType.APPLICATION_JSON_VALUE,produces=MediaType.APPLICATION_JSON_VALUE)
    public Mono<GovernedPolicyCommandResponse> issue(@PathVariable UUID policyId,@RequestBody GovernedPolicyIssueRequest request){return Mono.defer(()->journey().issue(policyId,request));}
    @Operation(operationId="recoverGovernedInsurancePolicyAction",summary="Observe the original Domain policy action without another mutation")
    @ApiResponse(responseCode="200",content=@Content(mediaType=MediaType.APPLICATION_JSON_VALUE,schema=@Schema(implementation=GovernedPolicyRecoveryResponse.class)))
    @DisableIdempotency
    @GetMapping(value="/recovery/{actionId}",produces=MediaType.APPLICATION_JSON_VALUE)
    public Mono<GovernedPolicyRecoveryResponse> recover(@PathVariable UUID actionId,@RequestParam String intentDigest){return Mono.defer(()->journey().recover(actionId,intentDigest));}
    private GovernedPolicyJourneyService journey(){var service=journeys.getIfAvailable();if(service==null)throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Trusted governed policy Domain journey required");return service;}
}
