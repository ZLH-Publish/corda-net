package com.template.flows;


import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;

@InitiatedBy(CDMWorkflow.class)
public class CDMWorkflowResponder extends FlowLogic<Void> {


    private final FlowSession otherPartySession;

    public CDMWorkflowResponder(FlowSession otherPartySession) {
        this.otherPartySession = otherPartySession;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        subFlow(new ReceiveFinalityFlow(otherPartySession));

        return null;
    }
}
