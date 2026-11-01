package io.fireflysoftware.experience.insurance.core.journey.policies;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TerminalPolicyIntentContractTest {
    GovernedPolicyBindRequest bind(){return new GovernedPolicyBindRequest(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),1,1,UUID.randomUUID(),"a".repeat(64),"b".repeat(64),UUID.randomUUID(),"SANDBOX-1",LocalDate.of(2027,1,1),LocalDate.of(2028,1,1));}
    @Test void bindAndIssuePreparationSelectorsAreStrictlyExclusive(){var b=bind();var i=new GovernedPolicyIssueRequest(UUID.randomUUID(),1,"a".repeat(64));assertThrows(IllegalArgumentException.class,()->new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.BIND,b,UUID.randomUUID(),i));assertThrows(IllegalArgumentException.class,()->new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.ISSUE,b,UUID.randomUUID(),i));assertThrows(IllegalArgumentException.class,()->new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.BIND,null,null,null));assertThrows(IllegalArgumentException.class,()->new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.ISSUE,null,null,i));}
    @Test void terminalIntentDiagnosticsExposeNoOriginalSelectors(){var b=bind();var value=new GovernedPolicyIntentRequest(GovernedPolicyPreparedIntent.Operation.BIND,b,null,null);assertEquals(b.actionId(),value.actionId());assertFalse(value.toString().contains(b.actionId().toString()));assertFalse(value.toString().contains(b.policyNumber()));}
}
