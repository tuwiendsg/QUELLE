/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.api;

import at.ac.tuwien.dsg.quelle.elasticityQuantification.requirements.ServiceUnitConfigurationSolution;
import at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.control.SESRecommendationOutputController;
import com.wordnik.swagger.annotations.Api;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Service("solutionExportService")
@Provider
@Path("/export")
@Api(value = "/", description = "The SolutionExportService is the entry point for all methods for outputing found configuration recommendations")
public class SolutionExportService {

    @Autowired
    private SESRecommendationOutputController recommendationOutputController;

    //todo: add value for configuring these with Spring
    private String nodeTypesOutputPath = "./OpenToscaOutput/nodeTypes";
    private String serviceTemplatesOutputPath = "./OpenToscaOutput/serviceTemplates";

    @POST
    @Path("cloudServicesToWinery")
    public void outputCloudServicesAsWineryNodes() {
        recommendationOutputController.outputCloudServicesAsWineryNodes(nodeTypesOutputPath);
    }

    @POST
    @Path("/xml/requirements")
    @Consumes("application/xml")
    public void outputConfigurationSolutionToWinery(List<List<ServiceUnitConfigurationSolution>> configurationsList, String serviceTemplateName) {
        recommendationOutputController.outputConfigurationSolutionToWinery(configurationsList, serviceTemplateName, serviceTemplatesOutputPath);
    }

}
