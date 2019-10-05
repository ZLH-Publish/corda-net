package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.template.contracts.TemplateContract;
import com.template.states.IOUState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.List;


@InitiatingFlow
@StartableByRPC
public class UpdateWorkflow extends FlowLogic<Void> {


    private final String iouValue;
    private final Party otherParty;
    private final String id;

    private final ProgressTracker progressTracker = new ProgressTracker();

    public UpdateWorkflow(String iouValue, Party otherParty, String id) {
        this.iouValue = iouValue;
        this.otherParty = otherParty;
        this.id = id;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    /**
     * The flow logic is encapsulated within the call() method.
     */
    @Suspendable
    @Override
    public Void call() throws FlowException {
        // We retrieve the notary identity from the network map.
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        final UniqueIdentifier linearId = UniqueIdentifier.Companion.fromString(id);
        // We create the transaction components.
        IOUState outputState = new IOUState(iouValue, getOurIdentity(), otherParty,linearId);
        Command command = new Command<>(new TemplateContract.Commands.Action(), getOurIdentity().getOwningKey());


        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(
                null,
                ImmutableList.of(linearId),
                Vault.StateStatus.UNCONSUMED,
                null);

        List<StateAndRef<IOUState>> iouStates = getServiceHub().getVaultService().queryBy(IOUState.class, queryCriteria).getStates();

        System.out.println("Total IOUStates with linearId '"+linearId+"': "+iouStates.size());
        StateAndRef<IOUState> oldState=null;
         for(StateAndRef<IOUState> ref:iouStates){
             System.out.println(ref.getState().getData().getValue());
             oldState=ref;
         }

        // We create a transaction builder and add the components.
        TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .addInputState(oldState)
                .addOutputState(outputState, TemplateContract.ID)
                .addCommand(command);

        // Signing the transaction.
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Creating a session with the other party.
        FlowSession otherPartySession = initiateFlow(otherParty);

        // We finalise the transaction and then send it to the counterparty.
        subFlow(new FinalityFlow(signedTx, otherPartySession));

        return null;
    }
}
