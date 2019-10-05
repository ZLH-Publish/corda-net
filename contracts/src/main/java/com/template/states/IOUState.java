package com.template.states;


import com.template.contracts.TemplateContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

// *********
// * State *
// *********
@BelongsToContract(TemplateContract.class)
public class IOUState implements LinearState {


    private final String value;
    private final Party lender;
    private final Party borrower;
    private final UniqueIdentifier linearId;

    public IOUState(String value, Party lender, Party borrower,UniqueIdentifier linearId) {
        this.value = value;
        this.lender = lender;
        this.borrower = borrower;
        this.linearId = linearId;
    }

    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    private void sendGet() throws Exception {

        String url = "http://localhost:8080/alive";

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        //add request header
        //con.setRequestProperty("User-Agent", USER_AGENT);

        int responseCode = con.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        //print result
        System.out.println(response.toString());

    }


    public String getValue() {
        return value;
    }

    public Party getLender() {
        return lender;
    }

    public Party getBorrower() {
        return borrower;
    }


    @Override public List<AbstractParty> getParticipants() {
        return Arrays.asList(lender,borrower);
    }


}