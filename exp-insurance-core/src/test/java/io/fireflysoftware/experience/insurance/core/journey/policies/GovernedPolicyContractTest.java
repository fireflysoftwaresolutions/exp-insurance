package io.fireflysoftware.experience.insurance.core.journey.policies;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GovernedPolicyContractTest {
    final UUID action=UUID.randomUUID(),submission=UUID.randomUUID(),party=UUID.randomUUID(),product=UUID.randomUUID(),offer=UUID.randomUUID(),acceptance=UUID.randomUUID(),contract=UUID.randomUUID();final String digest="a".repeat(64);
    GovernedPolicyBindRequest request(String number){return new GovernedPolicyBindRequest(action,submission,party,product,offer,1,1,acceptance,digest,digest,contract,number,LocalDate.of(2027,1,1),LocalDate.of(2028,1,1));}
    @Test void malformedSelectorsCannotReachTheDomainPort(){assertThrows(IllegalArgumentException.class,()->request("bad\nnumber"));assertThrows(IllegalArgumentException.class,()->request(new String(new char[]{'b','a','d',(char)0xd800})));assertThrows(IllegalArgumentException.class,()->new GovernedPolicyIssueRequest(new UUID(0,0),1,digest));assertThrows(IllegalArgumentException.class,()->new GovernedPolicyIssueRequest(action,0,digest));}
    @Test void uncertainAndAbsentObservationsCannotCarryInventedReceipts(){
        var result=new GovernedPolicyBindingResult(UUID.randomUUID(),"SANDBOX-1",GovernedPolicyBindingResult.PolicyState.BOUND,1,digest,acceptance,UUID.randomUUID(),offer,1,digest);
        assertThrows(IllegalArgumentException.class,()->new GovernedPolicyCommandResponse(GovernedPolicyCommandResponse.Status.OUTCOME_UNKNOWN,action,digest,result));
        assertThrows(IllegalArgumentException.class,()->new GovernedPolicyCommandResponse(GovernedPolicyCommandResponse.Status.COMMITTED,action,digest,null));
        assertThrows(IllegalArgumentException.class,()->new GovernedPolicyRecoveryResponse(GovernedPolicyRecoveryResponse.State.NOT_OBSERVED_AT_THIS_READ,action,digest,result));
    }
    @Test void ownerDigestRemainsOpaqueAndDiagnosticsHideSelectors(){
        var prepared=new GovernedPolicyPreparedIntent(GovernedPolicyPreparedIntent.Operation.BIND,action,"c".repeat(64));assertEquals("c".repeat(64),prepared.intentDigest());assertEquals("GovernedPolicyPreparedIntent[BIND]",prepared.toString());assertEquals("GovernedPolicyBindRequest[redacted]",request("PRIVATE-POLICY-SENTINEL").toString());
    }
}
