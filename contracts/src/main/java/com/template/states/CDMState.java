package com.template.states;


import com.template.contracts.CDMTemplateContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.CommonSchemaV1;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(CDMTemplateContract.class)
public class CDMState implements LinearState {

    private final String status;
    private final String content;
    private final Party sender;
    private final Party receiver;
    private final UniqueIdentifier linearId;

    public CDMState(String status, String content, Party sender, Party receiver, UniqueIdentifier linearId) {
        this.status = status;
        this.content = content;
        this.sender = sender;
        this.receiver = receiver;
        this.linearId = linearId;
    }

    public String getStatus() {
        return status;
    }

    public String getContent() {
        return content;
    }

    public Party getSender() {
        return sender;
    }

    public Party getReceiver() {
        return receiver;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override public List<AbstractParty> getParticipants() {
        return Arrays.asList(sender,receiver);
    }
}
