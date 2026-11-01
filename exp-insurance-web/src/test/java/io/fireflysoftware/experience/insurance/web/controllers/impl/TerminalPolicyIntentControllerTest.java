package io.fireflysoftware.experience.insurance.web.controllers.impl;

import io.fireflysoftware.experience.insurance.core.journey.policies.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TerminalPolicyIntentControllerTest {
    final GovernedPolicyJourneyControllerTest fixture=new GovernedPolicyJourneyControllerTest();
    GovernedPolicyIntentRequest request(){return new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.BIND,fixture.request(),null,null);}
    @Test void missingTrustedJourneyRefusesReadOnlyPreparation(){fixture.client(null).post().uri("/api/v1/experience/insurance/governed/policies/intents").bodyValue(request()).exchange().expectStatus().isEqualTo(503);}
    @Test void prepareReturnsOnlyOriginalActionAndOpaqueOwnerDigest(){var service=mock(GovernedPolicyJourneyService.class);var request=request();when(service.prepare(request)).thenReturn(Mono.just(new GovernedPolicyPreparedIntent(request.operation(),request.actionId(),fixture.digest)));fixture.client(service).post().uri("/api/v1/experience/insurance/governed/policies/intents").bodyValue(request).exchange().expectStatus().isOk().expectBody().jsonPath("$.operation").isEqualTo("BIND").jsonPath("$.actionId").isEqualTo(fixture.action.toString()).jsonPath("$.intentDigest").isEqualTo(fixture.digest).jsonPath("$.result").doesNotExist();verify(service).prepare(request);verifyNoMoreInteractions(service);}
    @Test void authorityRefusalCannotBecomeAPreparedGrant(){var service=mock(GovernedPolicyJourneyService.class);when(service.prepare(any())).thenReturn(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN)));fixture.client(service).post().uri("/api/v1/experience/insurance/governed/policies/intents").bodyValue(request()).exchange().expectStatus().isForbidden();verify(service).prepare(any());verifyNoMoreInteractions(service);}
}
