package io.fireflysoftware.experience.insurance.web.controllers.impl;

import io.fireflysoftware.experience.insurance.core.journey.policies.*;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GovernedPolicyJourneyControllerTest {
    final UUID action=UUID.randomUUID();final String digest="c".repeat(64);
    GovernedPolicyBindRequest request(){return new GovernedPolicyBindRequest(action,UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),1,1,UUID.randomUUID(),"a".repeat(64),"a".repeat(64),UUID.randomUUID(),"SANDBOX-1",LocalDate.of(2027,1,1),LocalDate.of(2028,1,1));}
    WebTestClient client(GovernedPolicyJourneyService service){var beans=new DefaultListableBeanFactory();if(service!=null)beans.registerSingleton("governed",service);return WebTestClient.bindToController(new GovernedPolicyJourneyControllerImpl(beans.getBeanProvider(GovernedPolicyJourneyService.class))).build();}
    @Test void missingTrustedJourneyRefusesBindingBeforeBusiness(){client(null).post().uri("/api/v1/experience/insurance/governed/policies/bindings").bodyValue(request()).exchange().expectStatus().isEqualTo(503);}
    @Test void missingTrustedJourneyRefusesActivationBeforeBusiness(){client(null).post().uri("/api/v1/experience/insurance/governed/policies/"+UUID.randomUUID()+"/issue").bodyValue(new GovernedPolicyIssueRequest(action,1,digest)).exchange().expectStatus().isEqualTo(503);}
    @Test void missingTrustedJourneyRefusesRecoveryBeforeBusiness(){client(null).get().uri("/api/v1/experience/insurance/governed/policies/recovery/"+action+"?intentDigest="+digest).exchange().expectStatus().isEqualTo(503);}
    @Test void unknownJourneyRetainsOriginalActionAndOpaqueOwnerDigest(){var service=mock(GovernedPolicyJourneyService.class);var request=request();when(service.bind(request)).thenReturn(Mono.just(new GovernedPolicyCommandResponse(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,action,digest,null)));
        client(service).post().uri("/api/v1/experience/insurance/governed/policies/bindings").bodyValue(request).exchange().expectStatus().isOk().expectBody().jsonPath("$.status").isEqualTo("OUTCOME_UNKNOWN").jsonPath("$.actionId").isEqualTo(action.toString()).jsonPath("$.intentDigest").isEqualTo(digest).jsonPath("$.result").doesNotExist();verify(service).bind(request);verifyNoMoreInteractions(service);}
    @Test void recoveryDoesNotStartAnotherJourneyMutation(){var service=mock(GovernedPolicyJourneyService.class);when(service.recover(action,digest)).thenReturn(Mono.just(new GovernedPolicyRecoveryResponse(GovernedPolicyRecoveryResponse.State.NOT_OBSERVED_AT_THIS_READ,action,digest,null)));client(service).get().uri("/api/v1/experience/insurance/governed/policies/recovery/"+action+"?intentDigest="+digest).exchange().expectStatus().isOk().expectBody().jsonPath("$.state").isEqualTo("NOT_OBSERVED_AT_THIS_READ");verify(service).recover(action,digest);verifyNoMoreInteractions(service);}
    @Test void currentJourneyAuthorityRefusalCannotBecomeSuccess(){var service=mock(GovernedPolicyJourneyService.class);when(service.bind(any())).thenReturn(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN)));client(service).post().uri("/api/v1/experience/insurance/governed/policies/bindings").bodyValue(request()).exchange().expectStatus().isForbidden();verify(service).bind(any());verifyNoMoreInteractions(service);}
}
