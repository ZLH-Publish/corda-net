package com.template.webserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.regnosys.rosetta.common.serialisation.RosettaObjectMapper;
import com.template.flows.CDMWorkflow;
import com.template.flows.IOUFlow;
import com.template.flows.UpdateWorkflow;
import liquibase.util.BooleanParser;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.node.NodeInfo;
import org.isda.cdm.EconomicTerms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private final CordaRPCOps proxy;
    private final static Logger logger = LoggerFactory.getLogger(Controller.class);

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
    }
    @GetMapping(value = "/templateendpoint", produces = "text/plain")
    private String templateendpoint() {
        return "Define an endpoint here.";
    }


    @GetMapping(value = "/peers")
    protected ResponseEntity<?> retrievePeers(){


        Map<String,Object> map=new HashMap<>();

        final List<NodeInfo> nodes = proxy.networkMapSnapshot();

        for(NodeInfo info:nodes){
            info.getSerial();
            List<Party> parties=info.getLegalIdentities();
            List<String> mainList=new ArrayList<>();
            for(Party p:parties){
                mainList.add(p.toString());
            }
            map.put(info.getSerial()+"",mainList);
        }



        return ResponseEntity.ok(map);
    }

    @GetMapping(value = "/init")
    protected ResponseEntity<?> initWorkflow(@RequestParam(value = "party") String party){

        //final List<Party> notaries = proxy.notaryIdentities();
        //final Party notary = notaries.get(0);

        final Set<Party> identities = proxy.partiesFromName(party, false);
        final Party identity = identities.iterator().next();

        try{


            FlowHandle<Void> flowHandle = proxy.startFlowDynamic(IOUFlow.class,"INIT",identity);

            Void result=flowHandle.getReturnValue().get();

            return ResponseEntity.ok(identity.toString());
        }catch(Exception ex){

            return ResponseEntity.badRequest().build();
        }

    }

    @GetMapping(value = "/update")
    protected ResponseEntity<?> updateWorkflow(@RequestParam(value = "party") String party,
                                               @RequestParam(value = "linearId") String linearId,
                                               @RequestParam(value = "status") String status){

        //final List<Party> notaries = proxy.notaryIdentities();
        //final Party notary = notaries.get(0);

        final Set<Party> identities = proxy.partiesFromName(party, false);
        final Party identity = identities.iterator().next();

        try{


            FlowHandle<Void> flowHandle = proxy.startFlowDynamic(UpdateWorkflow.class,status,identity,linearId);

            Void result=flowHandle.getReturnValue().get();

            return ResponseEntity.ok(identity.toString());
        }catch(Exception ex){

            return ResponseEntity.badRequest().build();
        }

    }

    @PostMapping(value = "/cmd_workflow/post")
    public ResponseEntity<?> performCDMWorkflow(@RequestBody(required = true) Map obj){
        try{

            ObjectMapper rosettaObjectMapper = RosettaObjectMapper.getDefaultRosettaObjectMapper();

            String status = obj.get("status").toString();
            logger.info("status:"+status);
            String id = obj.get("id").toString();
            logger.info("id:"+id);
            if(id.isEmpty()){
                id=UUID.randomUUID().toString();
            }
            Object party = obj.get("party");
            logger.info("party:"+party);
            Boolean initFlag= Boolean.valueOf(obj.get("initFlag").toString());
            logger.info("initFlag:"+initFlag);
            Object cdmContent = obj.get("cdmContent");

            Object observers = obj.get("observers");
            if(observers!=null){

            }

            Class<EconomicTerms> clazz=EconomicTerms.class;
            String json = cdmContent != null ? JsonUtil.toJsonString(cdmContent):JsonUtil.toJsonString(obj);
            logger.info(json);
            EconomicTerms economicTerms = rosettaObjectMapper.readValue(json, clazz);

            final Set<Party> identities = proxy.partiesFromName(party.toString(), false);
            final Party identity = identities.iterator().next();

            FlowHandle<Void> flowHandle = proxy.startFlowDynamic(CDMWorkflow.class,status,json,identity,initFlag,id);

            Void result=flowHandle.getReturnValue().get();

            return ResponseEntity.ok(identity.toString());

        }catch(Exception ex){
            logger.info("Error - ", ex);

        }
        return ResponseEntity.badRequest().build();
    }
}