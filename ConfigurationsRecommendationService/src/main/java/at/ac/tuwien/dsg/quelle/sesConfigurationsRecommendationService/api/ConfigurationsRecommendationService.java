/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.api;

import at.ac.tuwien.dsg.quelle.cloudServicesModel.requirements.MultiLevelRequirements;
import at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.control.SESConstructionController;
import at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.dtos.ServiceUnitServicesRecommendation;
import com.wordnik.swagger.annotations.Api;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Service("configurationsRecommendationService")
@Provider
@Path("/recommendation")
@Api(value = "/recommendation", description = "The RequirementsManagementService is the entry point for all methods for managing multi-level requirements")
public class ConfigurationsRecommendationService {

    @Autowired
    private SESConstructionController sesConstructionController;

    @POST
    @Path("/xml/recommendationGeneration")
    @Consumes("application/xml")
    @Produces("application/xml")
    /**
     * Unfortunately, the result is structured a bit weird. So, each ServiceUnitServicesRecommendation
     * has the MultiLevelRequirements associated, the Requirements block, and list of possible configurations
     * The reason is that one Unit might have mapped more Requirements blocks, and each of those blocks must be
     * mapped to a single cloud service, thus we can have multiple configuration options.
     * So, 1 unit might require 1 IaaS and 1 PaaS, and each can have multiple options
     */
    public List<ServiceUnitServicesRecommendation> analyzeRequirements(MultiLevelRequirements multiLevelRequirements) {
        return sesConstructionController.analyzeRequirements(multiLevelRequirements);
    }

}
