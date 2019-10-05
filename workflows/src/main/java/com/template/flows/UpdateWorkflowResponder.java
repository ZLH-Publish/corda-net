package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;


@InitiatedBy(UpdateWorkflow.class)
public class UpdateWorkflowResponder extends FlowLogic<Void> {

    private final FlowSession otherPartySession;

    public UpdateWorkflowResponder(FlowSession otherPartySession) {
        this.otherPartySession = otherPartySession;
    }

    @Suspendable
    @Override
    public Void call() throws FlowException {
        subFlow(new ReceiveFinalityFlow(otherPartySession));

        return null;
    }
}
