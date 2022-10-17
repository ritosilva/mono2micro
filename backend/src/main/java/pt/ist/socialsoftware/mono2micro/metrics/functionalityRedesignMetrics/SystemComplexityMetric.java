package pt.ist.socialsoftware.mono2micro.metrics.functionalityRedesignMetrics;

import pt.ist.socialsoftware.mono2micro.cluster.Cluster;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.property.AccessesDecomposition;
import pt.ist.socialsoftware.mono2micro.functionality.FunctionalityType;
import pt.ist.socialsoftware.mono2micro.functionality.LocalTransactionTypes;
import pt.ist.socialsoftware.mono2micro.functionality.domain.Functionality;
import pt.ist.socialsoftware.mono2micro.functionality.domain.FunctionalityRedesign;
import pt.ist.socialsoftware.mono2micro.functionality.domain.LocalTransaction;
import pt.ist.socialsoftware.mono2micro.functionality.dto.AccessDto;
import pt.ist.socialsoftware.mono2micro.utils.Utils;

import java.util.Map;
import java.util.Set;

public class SystemComplexityMetric extends FunctionalityRedesignMetric {
    public static final String SYSTEM_COMPLEXITY = "System Complexity";

    public String getType() {
        return SYSTEM_COMPLEXITY;
    }

    public Integer calculateMetric(
            AccessesDecomposition decomposition,
            Functionality functionality,
            FunctionalityRedesign functionalityRedesign
    ){
        int value = 0;

        if(functionality.getType() != FunctionalityType.SAGA)
            return value;

        Map<String, Set<Cluster>> functionalitiesClusters = Utils.getFunctionalitiesClusters(
                decomposition.getEntityIDToClusterName(),
                decomposition.getClusters(),
                decomposition.getFunctionalities().values());

        for (int i = 0; i < functionalityRedesign.getRedesign().size(); i++) {
            LocalTransaction lt = functionalityRedesign.getRedesign().get(i);

            if(lt.getId() != 0){
                for(AccessDto accessDto : lt.getClusterAccesses()) {
                    short entity = accessDto.getEntityID();
                    byte mode = accessDto.getMode();

                    // Functionality complexity cost of write
                    if(mode >= 2 && lt.getType() == LocalTransactionTypes.COMPENSATABLE) // 2 -> W, 3 -> RW
                        for (Functionality otherFunctionality : decomposition.getFunctionalities().values())
                            if (!otherFunctionality.getName().equals(functionality.getName()) &&
                                    otherFunctionality.containsEntity(entity) &&
                                    otherFunctionality.getEntities().get(entity) != 2 &&
                                    functionalitiesClusters.get(otherFunctionality.getName()).size() > 1)
                                value++;
                }
            }
        }
        return value;
    }
}