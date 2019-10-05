package com.template.flows;


import co.paralleluniverse.fibers.Suspendable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.qualify.QualifyResultsExtractor;
import com.template.contracts.CDMTemplateContract;

import com.template.states.CDMState;
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
import org.isda.cdm.EconomicTerms;
import org.isda.cdm.meta.EconomicTermsMeta;


import java.util.List;
import java.util.function.Function;

@InitiatingFlow
@StartableByRPC
public class CDMWorkflow extends FlowLogic<Void> {




    private final String status;
    private final String cdmContent;
    private final Party otherParty;
    private final Boolean initFlag;
    private final String id;


    private final ProgressTracker progressTracker = new ProgressTracker();

    public CDMWorkflow(String status, String cdmContent, Party otherParty, Boolean initFlag, String id) {
        this.status = status;
        this.cdmContent = cdmContent;
        this.otherParty = otherParty;
        this.initFlag = initFlag;
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



        try {
            Class<EconomicTerms> clazz = EconomicTerms.class;
            //String json = cdmContent != null ? JsonUtil.toJsonString(cdmContent) : JsonUtil.toJsonString(obj);
            ObjectMapper rosettaObjectMapper = RosettaObjectMapper.getDefaultRosettaObjectMapper();

            EconomicTerms economicTerms = rosettaObjectMapper.readValue(cdmContent, clazz);

            List<Function<? super EconomicTerms, QualifyResult>> qualifyFunctions = new EconomicTermsMeta().getQualifyFunctions();


            // Use the QualifyResultsExtractor helper to easily make use of qualification results
            //

            String result = new QualifyResultsExtractor<>(qualifyFunctions, economicTerms)
                    .getOnlySuccessResult()
                    .map(QualifyResult::getName)
                    .orElse("Failed to qualify");


            System.out.println(result);

        }catch(Exception Ex){

            throw new FlowException("Problem reading CDM.");
        }



        // We retrieve the notary identity from the network map.
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        UniqueIdentifier linearId = null;
        if(initFlag) {
            linearId = new UniqueIdentifier();
        }else{
            linearId = UniqueIdentifier.Companion.fromString(id);
        }


        // We create the transaction components.
        CDMState outputState = new CDMState(status,cdmContent,getOurIdentity(),otherParty,linearId);
        Command command = new Command<>(new CDMTemplateContract.Commands.Action(), getOurIdentity().getOwningKey());


        // We create a transaction builder and add the components.
        TransactionBuilder txBuilder = new TransactionBuilder(notary);

        if(!initFlag) {
            StateAndRef<CDMState> previousState = retrievePreviousState(linearId);
            if(previousState!=null) {
                txBuilder = txBuilder.addInputState(previousState);
            }
        }

        txBuilder = txBuilder.addOutputState(outputState, CDMTemplateContract.ID)
                .addCommand(command);

        // Signing the transaction.
        SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

        // Creating a session with the other party.
        FlowSession otherPartySession = initiateFlow(otherParty);

        // We finalise the transaction and then send it to the counterparty.
        subFlow(new FinalityFlow(signedTx, otherPartySession));

        return null;
    }

    protected StateAndRef<CDMState> retrievePreviousState(UniqueIdentifier linearId){

        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(
                null,
                ImmutableList.of(linearId),
                Vault.StateStatus.UNCONSUMED,
                null);

        List<StateAndRef<CDMState>> states = getServiceHub().getVaultService().queryBy(CDMState.class, queryCriteria).getStates();

        //System.out.println("Total CDMStates with linearId '"+linearId+"': "+states.size());
        StateAndRef<CDMState> oldState=null;
        for(StateAndRef<CDMState> ref:states){
            oldState=ref;
        }
        return oldState;
    }

}
