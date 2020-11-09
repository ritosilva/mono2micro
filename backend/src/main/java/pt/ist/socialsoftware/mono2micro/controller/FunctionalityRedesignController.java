package pt.ist.socialsoftware.mono2micro.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.ist.socialsoftware.mono2micro.domain.*;
import pt.ist.socialsoftware.mono2micro.dto.AccessDto;
import pt.ist.socialsoftware.mono2micro.manager.CodebaseManager;
import pt.ist.socialsoftware.mono2micro.utils.Constants;
import pt.ist.socialsoftware.mono2micro.utils.Metrics;

@RestController
@RequestMapping(value = "/mono2micro/codebase/{codebaseName}/dendrogram/{dendrogramName}/decomposition/{decompositionName}")
public class FunctionalityRedesignController {

    private static Logger logger = LoggerFactory.getLogger(FunctionalityRedesignController.class);

    private CodebaseManager codebaseManager = CodebaseManager.getInstance();


    @RequestMapping(value = "/controller/{controllerName}/initRedesign", method = RequestMethod.GET)
    public ResponseEntity<Controller> initRedesign(
            @PathVariable String codebaseName,
            @PathVariable String dendrogramName,
            @PathVariable String decompositionName,
            @PathVariable String controllerName
    ) {

        logger.debug("initRedesign");

        try {
            Codebase codebase = codebaseManager.getCodebase(codebaseName);
            Dendrogram dendrogram = codebase.getDendrogram(dendrogramName);
            Decomposition decomposition = dendrogram.getDecomposition(decompositionName);
            Controller controller = decomposition.getController(controllerName);
            controller.createFunctionalityRedesign(
                    Constants.DEFAULT_REDESIGN_NAME,
                    true,
                    decomposition.getControllerLocalTransactionsGraph(
                            codebase,
                            controllerName,
                            dendrogram.getTraceType(),
                            dendrogram.getTracesMaxLimit()
                    )
            );

            codebaseManager.writeCodebase(codebase);
            return new ResponseEntity<>(controller, HttpStatus.OK);

        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/controller/{controllerName}/redesign/{redesignName}/addCompensating", method = RequestMethod.POST)
    public ResponseEntity<Controller> addCompensating(
        @PathVariable String codebaseName,
        @PathVariable String dendrogramName,
        @PathVariable String decompositionName,
        @PathVariable String controllerName,
        @PathVariable String redesignName,
        @RequestBody HashMap<String, Object> data
    ) {
        logger.debug("addCompensating");

        try {
            String fromID = (String) data.get("fromID");
            String clusterName = (String) data.get("cluster");
            Set<Short> accesses = (Set<Short>) data.get("accesses");

            Codebase codebase = codebaseManager.getCodebase(codebaseName);
            Decomposition decomposition = codebase.getDendrogram(dendrogramName).getDecomposition(decompositionName);
            Controller controller = decomposition.getController(controllerName);

            controller.getFunctionalityRedesign(redesignName).addCompensating(clusterName, accesses, fromID);
            Metrics.calculateRedesignComplexities(controller, redesignName, decomposition);
            codebaseManager.writeCodebase(codebase);

            return new ResponseEntity<>(controller, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/controller/{controllerName}/redesign/{redesignName}/sequenceChange", method = RequestMethod.POST)
    public ResponseEntity<Controller> sequenceChange(@PathVariable String codebaseName,
                                                     @PathVariable String dendrogramName,
                                                     @PathVariable String decompositionName,
                                                     @PathVariable String controllerName,
                                                     @PathVariable String redesignName,
                                                     @RequestBody HashMap<String, String> data) {
        logger.debug("sequenceChange");
        try {
            String localTransactionID = data.get("localTransactionID");
            String newCaller = data.get("newCaller");

            Codebase codebase = codebaseManager.getCodebase(codebaseName);
            Decomposition decomposition = codebase.getDendrogram(dendrogramName).getDecomposition(decompositionName);
            Controller controller = codebase.getDendrogram(dendrogramName).getDecomposition(decompositionName).getController(controllerName);
            controller.getFunctionalityRedesign(redesignName).sequenceChange(localTransactionID, newCaller);

            Metrics.calculateRedesignComplexities(controller, redesignName, decomposition);
            codebaseManager.writeCodebase(codebase);
            return new ResponseEntity<>(controller, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        }
    }

    @RequestMapping(value = "/controller/{controllerName}/redesign/{redesignName}/dcgi", method = RequestMethod.POST)
    public ResponseEntity<Controller> dcgi(
        @PathVariable String codebaseName,
        @PathVariable String dendrogramName,
        @PathVariable String decompositionName,
        @PathVariable String controllerName,
        @PathVariable String redesignName,
        @RequestBody HashMap<String, String> data
    ) {
        logger.debug("dcgi");
        try {
            String fromCluster = data.get("fromCluster");
            String toCluster = data.get("toCluster");
            String localTransactions = data.get("localTransactions");

            Codebase codebase = codebaseManager.getCodebase(codebaseName);
            Decomposition decomposition = codebase.getDendrogram(dendrogramName).getDecomposition(decompositionName);
            Controller controller = decomposition.getController(controllerName);

            controller.getFunctionalityRedesign(redesignName).dcgi(fromCluster, toCluster, localTransactions);

            Metrics.calculateRedesignComplexities(controller, redesignName, decomposition);
            codebaseManager.writeCodebase(codebase);

            return new ResponseEntity<>(controller, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        }

    }

    @RequestMapping(value = "/controller/{controllerName}/redesign/{redesignName}/pivotTransaction", method = RequestMethod.POST)
    public ResponseEntity<Object> pivotTransaction(
        @PathVariable String codebaseName,
        @PathVariable String dendrogramName,
        @PathVariable String decompositionName,
        @PathVariable String controllerName,
        @PathVariable String redesignName,
        @RequestParam String transactionID,
        @RequestParam Optional<String> newRedesignName
    ) {
        logger.debug("pivotTransaction");
        try {
            Codebase codebase = codebaseManager.getCodebase(codebaseName);
            Dendrogram dendrogram = codebase.getDendrogram(dendrogramName);
            Decomposition decomposition = dendrogram.getDecomposition(decompositionName);
            Controller controller = decomposition.getController(controllerName);

            if(newRedesignName.isPresent())
                if(!controller.checkNameValidity(newRedesignName.get()))
                    return new ResponseEntity<>("Name is already selected",HttpStatus.FORBIDDEN);


            controller.getFunctionalityRedesign(redesignName).definePivotTransaction(Integer.parseInt(transactionID));
            Metrics.calculateRedesignComplexities(controller, redesignName, decomposition);

            if(newRedesignName.isPresent()) {
                controller.changeFunctionalityRedesignName(redesignName, newRedesignName.get());

                DirectedAcyclicGraph<LocalTransaction, DefaultEdge> controllerLocalTransactionsGraph = decomposition.getControllerLocalTransactionsGraph(
                    codebase,
                    controllerName,
                    dendrogram.getTraceType(),
                    dendrogram.getTracesMaxLimit()
                );

                controller.createFunctionalityRedesign(
                    Constants.DEFAULT_REDESIGN_NAME,
                    false,
                    controllerLocalTransactionsGraph
                );
            }

            Metrics.calculateRedesignComplexities(controller, Constants.DEFAULT_REDESIGN_NAME, decomposition);
            codebaseManager.writeCodebase(codebase);

            return new ResponseEntity<>(controller, HttpStatus.OK);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

    }

    @RequestMapping(value="/controller/{controllerName}/redesign/{redesignName}/changeLTName", method = RequestMethod.POST)
    public ResponseEntity<Controller> changeLTName(
        @PathVariable String codebaseName,
        @PathVariable String dendrogramName,
        @PathVariable String decompositionName,
        @PathVariable String controllerName,
        @PathVariable String redesignName,
        @RequestParam String transactionID,
        @RequestParam String newName
    ){
        logger.debug("changeLTName");
        try {
            Codebase codebase = codebaseManager.getCodebase(codebaseName);
            Decomposition decomposition = codebase.getDendrogram(dendrogramName).getDecomposition(decompositionName);
            Controller controller = decomposition.getController(controllerName);
            controller.getFunctionalityRedesign(redesignName).changeLTName(transactionID, newName);
            codebaseManager.writeCodebase(codebase);

            return new ResponseEntity<>(controller, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value="/controller/{controllerName}/redesign/{redesignName}/deleteRedesign", method = RequestMethod.DELETE)
    public ResponseEntity<Controller> deleteRedesign(
        @PathVariable String codebaseName,
        @PathVariable String dendrogramName,
        @PathVariable String decompositionName,
        @PathVariable String controllerName,
        @PathVariable String redesignName
    ) {
        logger.debug("deleteRedesign");
        try {
            Codebase codebase = codebaseManager.getCodebase(codebaseName);
            Decomposition decomposition = codebase.getDendrogram(dendrogramName).getDecomposition(decompositionName);
            Controller controller = decomposition.getController(controllerName);
            controller.deleteRedesign(redesignName);
            codebaseManager.writeCodebase(codebase);

            return new ResponseEntity<>(controller, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @RequestMapping(value="/controller/{controllerName}/redesign/{redesignName}/useForMetrics", method = RequestMethod.POST)
    public ResponseEntity<Controller> useForMetrics(
        @PathVariable String codebaseName,
        @PathVariable String dendrogramName,
        @PathVariable String decompositionName,
        @PathVariable String controllerName,
        @PathVariable String redesignName
    ) {
        logger.debug("useForMetrics");
        try {
            Codebase codebase = codebaseManager.getCodebase(codebaseName);
            Decomposition decomposition = codebase.getDendrogram(dendrogramName).getDecomposition(decompositionName);
            Controller controller = decomposition.getController(controllerName);
            controller.changeFRUsedForMetrics(redesignName);
            codebaseManager.writeCodebase(codebase);

            return new ResponseEntity<>(controller, HttpStatus.OK);
        } catch (IOException e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

}
