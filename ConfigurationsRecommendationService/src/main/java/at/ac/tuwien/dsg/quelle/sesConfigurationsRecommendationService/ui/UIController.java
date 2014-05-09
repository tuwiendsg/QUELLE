/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.ui;

import at.ac.tuwien.dsg.extensions.neo4jPersistenceAdapter.DataAccess;
import at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.control.SESConstructionController;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.requirements.MultiLevelRequirements;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.faces.bean.SessionScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Component("uiController")
//@ManagedBean(name = "uiController")
@SessionScoped
public class UIController implements Serializable {

    @Value(value = "#{sesConstructionController}")
    private SESConstructionController controller;

    static final Logger log = LoggerFactory.getLogger(UIController.class);

    @Value(value = "#{dataAccess}")
    private DataAccess dataAccess;

    private MultiLevelRequirements requirements;

    private Map<Metric.MetricType, List<Metric>> cloudServicesMetrics;

    private Metric selectedMetric;

    public SESConstructionController getController() {
        return controller;
    }

    public void setController(SESConstructionController controller) {
        this.controller = controller;
        cloudServicesMetrics = controller.updateCloudProvidersDescription();
    }

    public DataAccess getDataAccess() {
        return dataAccess;
    }

    public void setDataAccess(DataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public MultiLevelRequirements getRequirements() {
        return controller.getRequirements();
    }

    public void setRequirements(MultiLevelRequirements requirements) {
        this.controller.setRequirements(requirements);
    }

    public Metric getSelectedMetric() {
        return selectedMetric;
    }

    public void setSelectedMetric(Metric selectedMetric) {
        this.selectedMetric = selectedMetric;
    }

    public List<Metric> getCostMetrics() {
        return cloudServicesMetrics.get(Metric.MetricType.COST);
    }

    public List<Metric> getQualityMetrics() {
        return cloudServicesMetrics.get(Metric.MetricType.QUALITY);
    }

    public List<Metric> getResourceMetrics() {
        return cloudServicesMetrics.get(Metric.MetricType.RESOURCE);
    }

}
