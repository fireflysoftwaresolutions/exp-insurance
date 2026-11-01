package io.fireflysoftware.experience.insurance.web.openapi;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.fireflysoftware.experience.insurance.core.journey.policies.GovernedPolicyJourneyService;
import io.fireflysoftware.experience.insurance.web.policies.GovernedPolicyDomainClientFactory;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("openapi-gen")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(classes=OpenApiGenApplication.class,webEnvironment=SpringBootTest.WebEnvironment.MOCK,properties={"firefly.cache.enabled=false","firefly.cache.postgres.enabled=false","spring.main.web-application-type=reactive","springdoc.api-docs.enabled=true","springdoc.api-docs.version=OPENAPI_3_0"})
class GovernedPolicyJourneyProducerTest {
    @Autowired ApplicationContext context;
    static final Set<String> OPERATIONS=Set.of("readQuote","updateQuote","quoteStatus","readPolicy","updatePolicy","policyStatus","readClaim","updateClaim","claimStatus","createQuote","withdrawQuote","bind","cancelPolicy","registerClaim","closeClaim","offerInsuranceQuote","acceptInsuranceQuote","readInsuranceQuoteOffer","readInsuranceQuoteHistory","recoverInsuranceQuoteAction","bindGovernedInsurancePolicy","issueGovernedInsurancePolicy","recoverGovernedInsurancePolicyAction","prepareGovernedInsurancePolicyIntent");
    JsonNode document(String name)throws Exception{
        var client=WebTestClient.bindToApplicationContext(context).configureClient().responseTimeout(Duration.ofSeconds(3)).build();
        byte[] json=client.get().uri("/v3/api-docs").exchange().expectStatus().isOk().expectBody().returnResult().getResponseBody();
        byte[] yaml=client.get().uri("/v3/api-docs.yaml").exchange().expectStatus().isOk().expectBody().returnResult().getResponseBody();
        assertNotNull(json);assertNotNull(yaml);assertTrue(json.length<=4*1024*1024 && yaml.length<=4*1024*1024);
        Files.createDirectories(Path.of("target"));Files.write(Path.of("target",name+"-governed-experience-policy-openapi.json"),json);Files.write(Path.of("target",name+"-governed-experience-policy-openapi.yml"),yaml);
        var value=new ObjectMapper().readTree(json);assertEquals(value,new ObjectMapper(new YAMLFactory()).readTree(yaml));return value;
    }
    void routes(JsonNode doc){
        var observed=new HashSet<String>();doc.path("paths").forEach(path->path.fields().forEachRemaining(method->{if(Set.of("get","post","put","patch","delete","head","options","trace").contains(method.getKey()))assertTrue(observed.add(method.getValue().path("operationId").asText()));}));assertEquals(OPERATIONS,observed);
        for(var expected:List.of(new String[]{"/api/v1/experience/insurance/governed/policies/intents","post","prepareGovernedInsurancePolicyIntent","GovernedPolicyPreparedIntent"},new String[]{"/api/v1/experience/insurance/governed/policies/bindings","post","bindGovernedInsurancePolicy","GovernedPolicyCommandResponse"},new String[]{"/api/v1/experience/insurance/governed/policies/{policyId}/issue","post","issueGovernedInsurancePolicy","GovernedPolicyCommandResponse"},new String[]{"/api/v1/experience/insurance/governed/policies/recovery/{actionId}","get","recoverGovernedInsurancePolicyAction","GovernedPolicyRecoveryResponse"})){
            var operation=doc.path("paths").path(expected[0]).path(expected[1]);assertEquals(expected[2],operation.path("operationId").asText());assertEquals("#/components/schemas/"+expected[3],operation.path("responses").path("200").path("content").path("application/json").path("schema").path("$ref").asText());assertTrue(operation.path("security").toString().contains("insuranceBearer"));
        }
    }
    void outcomes(JsonNode doc){
        var schema=doc.path("components").path("schemas").path("GovernedPolicyCommandResponse");var required=new HashSet<String>();schema.path("required").forEach(value->required.add(value.asText()));assertTrue(required.containsAll(Set.of("status","actionId","intentDigest")));
        var status=schema.path("properties").path("status");if(status.has("$ref"))status=doc.at(status.path("$ref").asText().substring(1));var values=new HashSet<String>();status.path("enum").forEach(value->values.add(value.asText()));assertEquals(Set.of("COMMITTED","COMMITTED_RESPONSE_UNAVAILABLE","OUTCOME_UNKNOWN","REFUSED"),values);
        var properties=doc.path("components").path("schemas").path("GovernedPolicyBindRequest").path("properties");assertEquals(14,properties.size());for(String forbidden:List.of("tenantId","actor","issuer","clientId","premium","acceptedTerms","intentDigest","ownerIntent"))assertFalse(properties.has(forbidden));assertEquals(4,doc.path("components").path("schemas").path("GovernedPolicyIntentRequest").path("properties").size());
    }
    void noBusiness(){assertEquals(0,context.getBeanNamesForType(GovernedPolicyDomainClientFactory.class,false,false).length);assertEquals(0,context.getBeanNamesForType(GovernedPolicyJourneyService.class,false,false).length);}
    @Test @Order(1) void originalProducerKeepsAllTwentyThreeOperationsAndAddsReadOnlyPreparation()throws Exception{routes(document("typed"));}
    @Test @Order(2) void originalProducerKeepsTheOpaqueOwnerDigestAndAllFourCommandOutcomes()throws Exception{outcomes(document("outcome"));}
    @Test @Order(3) void originalProducerActivatesNoNativePolicyExchangeOrBusinessJourney()throws Exception{document("no-business");noBusiness();}
    @Test @Order(4) void ownedReactorBoundaryCopiesOnlyTheSameCompleteProducerAfterSemanticChecks()throws Exception{
        if(!Boolean.getBoolean("owned.insurance.sdk-boundary.enabled"))return;
        assertEquals("validated-hosted-manual-main-v1",System.getProperty("owned.insurance.sdk-boundary.hosted"));
        var root=Path.of(System.getProperty("owned.insurance.sdk-boundary.root")).normalize();assertTrue(root.isAbsolute());assertFalse(Files.isSymbolicLink(root));assertEquals(Path.of("..").toRealPath(),root.toRealPath());assertEquals(Path.of(System.getProperty("owned.insurance.sdk-boundary.workspace"),"qualification","experience-policy-chain","exp-insurance").toRealPath(),root.toRealPath());
        var canonical=root.resolve("exp-insurance-sdk/src/main/resources/openapi.yml");assertTrue(Files.isRegularFile(canonical,LinkOption.NOFOLLOW_LINKS));assertFalse(Files.isSymbolicLink(canonical));assertEquals(canonical,canonical.toRealPath());assertTrue(Files.size(canonical)<=4*1024*1024);var original=Files.readAllBytes(canonical);String expected=System.getProperty("owned.insurance.sdk-boundary.preimage-sha256");assertNotNull(expected);assertTrue(expected.matches("[0-9a-f]{64}"));assertEquals(expected,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(original)));
        byte[] json=null,yaml=null;for(String name:List.of("typed","outcome","no-business")){
            var jb=Files.readAllBytes(Path.of("target",name+"-governed-experience-policy-openapi.json"));var yb=Files.readAllBytes(Path.of("target",name+"-governed-experience-policy-openapi.yml"));assertTrue(jb.length>0 && jb.length<=4*1024*1024 && yb.length>0 && yb.length<=4*1024*1024);if(json!=null){assertArrayEquals(json,jb);assertArrayEquals(yaml,yb);}json=jb;yaml=yb;var doc=new ObjectMapper().readTree(jb);assertEquals(doc,new ObjectMapper(new YAMLFactory()).readTree(yb));routes(doc);outcomes(doc);
        }noBusiness();
        var quoteRoot=root.getParent().resolve("domain-insurance-underwriting");var currentQuote=new ObjectMapper().readTree(Files.readAllBytes(quoteRoot.resolve("domain-insurance-underwriting-web/target/governed-domain-openapi.json")));var selectedQuote=new ObjectMapper(new YAMLFactory()).readTree(Files.readAllBytes(quoteRoot.resolve("domain-insurance-underwriting-sdk/src/main/resources/openapi.yml")));for(String key:List.of("openapi","info","paths","components"))assertEquals(currentQuote.path(key),selectedQuote.path(key),"Actual complete quote producer schema differs: "+key);
        Files.write(Path.of("target","owned-sdk-canonical-preimage.yml"),original,StandardOpenOption.CREATE_NEW);Files.write(canonical,yaml,StandardOpenOption.WRITE,StandardOpenOption.TRUNCATE_EXISTING);assertArrayEquals(yaml,Files.readAllBytes(canonical));Files.writeString(Path.of("target","owned-sdk-boundary.json"),"{\"originalOperationsRetained\":23,\"operations\":24,\"exactOriginalProducerCopied\":true,\"authorityGranted\":false}\n",StandardOpenOption.CREATE_NEW);
    }
}
