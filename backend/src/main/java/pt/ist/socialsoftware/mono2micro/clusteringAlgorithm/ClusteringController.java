package pt.ist.socialsoftware.mono2micro.clusteringAlgorithm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.ist.socialsoftware.mono2micro.codebase.CodebaseController;

import java.util.List;

@RestController
@RequestMapping(value = "/mono2micro/clustering")
public class ClusteringController {
    @Autowired
    ClusteringService clusteringService;

    private static final Logger logger = LoggerFactory.getLogger(ClusteringController.class);

    @GetMapping(value = "/{algorithmType}/getSupportedRepresentationInfoTypes")
    public ResponseEntity<List<String>> getSupportedRepresentationInfoTypes(
            @PathVariable String algorithmType
    ){
        logger.debug("getSupportedRepresentationInfoTypes");

        try {
            return new ResponseEntity<>(clusteringService.getSupportedRepresentationInfoTypes(algorithmType), HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
