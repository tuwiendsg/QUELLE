/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.api;

import at.ac.tuwien.dsg.quelle.cloudServicesModel.requirements.MultiLevelRequirements;
import at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.control.SESConstructionController;
import com.wordnik.swagger.annotations.Api;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
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
@Service("sesConstructionService")
@Provider
@Path("/")
@Api(value = "/", description = "The SESConstructionService is the entry point for all SES construction")
public class SESConstructionService {

    @Autowired
    private SESConstructionController controller;

    @GET
    @Path("/xml/requirements")
    @Produces("application/xml")
    public MultiLevelRequirements getLatestRequirements() {
        return controller.getRequirements();
    }

    @GET
    @Path("/json/requirements")
    @Produces("application/json")
    public String getLatestRequirementsInJSON() {
        return controller.getRequirementsJSON();
    }

    @PUT
    @Path("/xml/requirements")
    public void setRequirements(MultiLevelRequirements levelRequirements) {
        controller.setRequirements(levelRequirements);
    }

    @GET
    @Path("/json/costmetrics")
    @Produces("application/json")
    public String getCostMetricsAsJSON() {
        return controller.getCostMetricsAsJSON();
    }

    @GET
    @Path("/json/qualitymetrics")
    @Produces("application/json")
    public String getQualityMetricsAsJSON() {
        return controller.getQualityMetricsAsJSON();
    }

    @GET
    @Path("/json/resourcemetrics")
    @Produces("application/json")
    public String getResourceMetricsAsJSON() {
        return controller.getResourceMetricsAsJSON();
    }

    @PUT
    @Path("/management/json/requirements")
    public void addToStructureRequirements(String jsonRepr) {
        controller.addToRequirements(jsonRepr);
    }

    @DELETE
    @Path("/management/json/requirements")
    public void removeFromStructureRequirements(String jsonRepr) {
        controller.removeFromRequirements(jsonRepr);
    }

}
