/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.control;

 
import at.ac.tuwien.dsg.extensions.neo4jPersistenceAdapter.DataAccess;
import at.ac.tuwien.dsg.extensions.neo4jPersistenceAdapter.daos.CloudProviderDAO;
import at.ac.tuwien.dsg.extensions.neo4jPersistenceAdapter.daos.ServiceUnitDAO;
import at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.dtos.CloudServiceConfigurationRecommendation;
import at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.dtos.ServiceUnitServicesRecommendation;
import at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.util.ConfigurationUtil;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MonitoredElement;
import at.ac.tuwien.dsg.mela.common.requirements.Condition;
import at.ac.tuwien.dsg.mela.common.requirements.Requirement;
import at.ac.tuwien.dsg.mela.common.requirements.Requirements;
import at.ac.tuwien.dsg.quelle.cloudDescriptionParsers.CloudDescriptionParser;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostElement;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostFunction;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.ElasticityCapability;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Quality;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Resource;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.ServiceUnit;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.requirements.MultiLevelRequirements;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.requirements.Strategy;
import at.ac.tuwien.dsg.quelle.elasticityQuantification.engines.CloudServiceElasticityAnalysisEngine;
import at.ac.tuwien.dsg.quelle.elasticityQuantification.engines.CloudServiceUnitAnalysisEngine;
import at.ac.tuwien.dsg.quelle.elasticityQuantification.engines.RequirementsMatchingEngine;
import at.ac.tuwien.dsg.quelle.elasticityQuantification.engines.ServiceUnitComparators;
import at.ac.tuwien.dsg.quelle.elasticityQuantification.requirements.RequirementsResolutionResult;
import at.ac.tuwien.dsg.quelle.elasticityQuantification.requirements.ServiceUnitConfigurationSolution;
import at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.util.ConvertTOJSON;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Service("sesConstructionController")
public class SESConstructionController implements InitializingBean {

    static final Logger log = LoggerFactory.getLogger(SESConstructionController.class);

    @Value("#{dataAccess}")
    private DataAccess dataAccess;
    @Autowired
    private ApplicationContext context;

    @Autowired
    private CloudServiceElasticityAnalysisEngine cloudServiceElasticityAnalysisEngine;

    @Autowired
    private RequirementsMatchingEngine requirementsMatchingEngine;

    @Autowired
    private ServiceUnitComparators serviceUnitComparators;

    @Autowired
    private ConfigurationUtil configurationUtil;

    private MultiLevelRequirements requirements;

    /**
     * Potential metrics to put requirements on
     */
    private Map<Metric.MetricType, List<Metric>> cloudServicesMetrics;

    {
        cloudServicesMetrics = new LinkedHashMap<>();
        cloudServicesMetrics.put(Metric.MetricType.QUALITY, new ArrayList<Metric>());
        cloudServicesMetrics.put(Metric.MetricType.COST, new ArrayList<Metric>());
        cloudServicesMetrics.put(Metric.MetricType.RESOURCE, new ArrayList<Metric>());
    }

    public String getRequirementsJSON() {
        return ConvertTOJSON.convertTOJSON(requirements);
    }

    public MultiLevelRequirements getRequirements() {
        return requirements;
    }

    public void setRequirements(MultiLevelRequirements requirements) {
        this.requirements = requirements;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        requirements = configurationUtil.createDefaultRequirements();
        CloudProvider provider = configurationUtil.createAmazonDefaultCloudProvider();

        Transaction transaction = dataAccess.startTransaction();

        try {

            CloudProviderDAO.persistCloudProvider(provider, dataAccess.getGraphDatabaseService());

            transaction.success();
        } catch (Exception e) {
            e.printStackTrace();
            transaction.failure();
        }
        transaction.finish();

        //pull new list of cloud services metrics
        updateMetrics();

    }

    public String getCostMetricsAsJSON() {
        return ConvertTOJSON.convertTOJSON(cloudServicesMetrics.get(Metric.MetricType.COST));
    }

    public String getQualityMetricsAsJSON() {
        return ConvertTOJSON.convertTOJSON(cloudServicesMetrics.get(Metric.MetricType.QUALITY));
    }

    public String getResourceMetricsAsJSON() {
        return ConvertTOJSON.convertTOJSON(cloudServicesMetrics.get(Metric.MetricType.RESOURCE));
    }

    public Map<Metric.MetricType, List<Metric>> getCloudServicesMetrics() {
        return cloudServicesMetrics;
    }

    public Map<Metric.MetricType, List<Metric>> updateCloudProvidersDescription() {

        Transaction transaction = dataAccess.startTransaction();

        try {

            List<CloudProvider> providers = new ArrayList<>();

            // list all MELA datasources from application context
            Map<String, CloudDescriptionParser> cloudParsers = context.getBeansOfType(CloudDescriptionParser.class);
            for (String name : cloudParsers.keySet()) {
                CloudDescriptionParser cloudDescriptionParser = cloudParsers.get(name);
                log.debug("Using CloudDescriptionParser '{}': {}  to update cloud description", name, cloudDescriptionParser);
                CloudProvider provider = cloudDescriptionParser.getCloudProviderDescription();
                providers.add(provider);
            }

            CloudProviderDAO.persistCloudProviders(providers, dataAccess.getGraphDatabaseService());

            transaction.success();
        } catch (Exception e) {
            e.printStackTrace();
            transaction.failure();
        }
        transaction.finish();

        //pull new list of cloud services metrics
        updateMetrics();

        return cloudServicesMetrics;

    }

    public List<ServiceUnitServicesRecommendation> analyzeRequirements(MultiLevelRequirements multiLevelRequirements) {

        Transaction transaction = dataAccess.startTransaction();

        List<CloudProvider> cloudProviders = CloudProviderDAO.getAllCloudProviders(dataAccess.getGraphDatabaseService());

        List<MultiLevelRequirements> individualServiceUnitRequirements = multiLevelRequirements.flatten();

        List<ServiceUnitServicesRecommendation> recommendations = new ArrayList<>();

        for (MultiLevelRequirements reqs : individualServiceUnitRequirements) {

            RequirementsResolutionResult result = requirementsMatchingEngine.analyzeMultiLevelRequirements(cloudProviders, reqs);
            Map<MultiLevelRequirements, Map<Requirements, List<ServiceUnitConfigurationSolution>>> bestElasticity = result.getConcreteConfigurations(serviceUnitComparators);

            {
                for (MultiLevelRequirements levelRequirements : bestElasticity.keySet()) {

                    Map<Requirements, List<ServiceUnitConfigurationSolution>> solutions = bestElasticity.get(levelRequirements);

//                    String strategies = "";
//                    for (Strategy s : levelRequirements.getOptimizationStrategies()) {
//                        strategies += "_" + s.getStrategyCategory();
//                    }
                    for (Requirements requirements : solutions.keySet()) {

                        List<CloudServiceConfigurationRecommendation> recommendedConfigurations = new ArrayList<>();

//                        String solutionsNames = "";
//
//                        int solutionsCount = solutions.get(requirements).size();
//
//                        // compute average elasticities
//                        double averageCostElasticity = 0d;
//                        double averageSUElasticity = 0d;
//                        double averageResourceElasticity = 0d;
//                        double averageQualityElasticity = 0d;
//
//                        double minCostElasticity = Double.POSITIVE_INFINITY;
//                        double minSUElasticity = Double.POSITIVE_INFINITY;
//                        double minResourceElasticity = Double.POSITIVE_INFINITY;
//                        double minQualityElasticity = Double.POSITIVE_INFINITY;
//
//                        double maxCostElasticity = Double.NEGATIVE_INFINITY;
//                        double maxSUElasticity = Double.NEGATIVE_INFINITY;
//                        double maxResourceElasticity = Double.NEGATIVE_INFINITY;
//                        double maxQualityElasticity = Double.NEGATIVE_INFINITY;
                        for (ServiceUnitConfigurationSolution solutionConfiguration : solutions.get(requirements)) {

                            //
                            // // System.out.println("Matched " +
                            // solutionConfiguration.getOverallMatched());
                            // // System.out.println("Unmatched " +
                            // solutionConfiguration.getOverallUnMatched());
                            //
                            // String configurationJSONDescription =
                            // solutionConfiguration.toJSON().toJSONString();
                            // System.out.println(configurationJSONDescription);
                            CloudServiceUnitAnalysisEngine.AnalysisResult analysisResult = cloudServiceElasticityAnalysisEngine.analyzeElasticity(solutionConfiguration.getServiceUnit());
//                            solutionsNames += " " + solutionConfiguration.getServiceUnit().getName();

                            double costElasticity = (double) analysisResult.getValue(CloudServiceElasticityAnalysisEngine.COST_ELASTICITY);
                            double sUElasticity = (double) analysisResult
                                    .getValue(CloudServiceElasticityAnalysisEngine.SERVICE_UNIT_ASSOCIATION_ELASTICITY);
                            double resourceElasticity = (double) analysisResult.getValue(CloudServiceElasticityAnalysisEngine.RESOURCE_ELASTICITY);
                            double qualityElasticity = (double) analysisResult.getValue(CloudServiceElasticityAnalysisEngine.QUALITY_ELASTICITY);

                            recommendedConfigurations.add(new CloudServiceConfigurationRecommendation().withServiceUnitConfigurationSolution(requirements.getName(), solutionConfiguration, costElasticity, sUElasticity, resourceElasticity, qualityElasticity));

//                            averageCostElasticity += costElasticity;
//                            averageSUElasticity += sUElasticity;
//                            averageResourceElasticity += resourceElasticity;
//                            averageQualityElasticity += qualityElasticity;
//
//                            if (minCostElasticity > costElasticity) {
//                                minCostElasticity = costElasticity;
//                            }
//
//                            if (minSUElasticity > sUElasticity) {
//                                minSUElasticity = sUElasticity;
//                            }
//
//                            if (minResourceElasticity > resourceElasticity) {
//                                minResourceElasticity = resourceElasticity;
//                            }
//
//                            if (minQualityElasticity > qualityElasticity) {
//                                minQualityElasticity = qualityElasticity;
//                            }
//
//                            if (maxCostElasticity < costElasticity) {
//                                maxCostElasticity = costElasticity;
//                            }
//
//                            if (maxSUElasticity < sUElasticity) {
//                                maxSUElasticity = sUElasticity;
//                            }
//
//                            if (maxResourceElasticity < resourceElasticity) {
//                                maxResourceElasticity = resourceElasticity;
//                            }
//
//                            if (maxQualityElasticity < qualityElasticity) {
//                                maxQualityElasticity = qualityElasticity;
//                            }
                        }

                        recommendations.add(new ServiceUnitServicesRecommendation().withSolutionRecommendation(requirements, recommendedConfigurations));

                        // write cfg sol as dot
                        // DOTWriter.writeServiceUnitConfigurationSolutions(solutions.get(requirements),
                        // new
                        // FileWriter("./experiments/scenario2/solutions_" +
                        // requirements.getName() + strategies + ".dot"));
//                        averageCostElasticity /= solutionsCount;
//                        averageSUElasticity /= solutionsCount;
//                        averageResourceElasticity /= solutionsCount;
//                        averageQualityElasticity /= solutionsCount;
//                        writer.write(requirements.getName() + "," + strategies + "," + solutionsNames + "," + solutionsCount + "," + averageCostElasticity + ","
//                                + minCostElasticity + "," + maxCostElasticity + "," + averageSUElasticity + "," + minSUElasticity + "," + maxSUElasticity
//                                + "," + averageResourceElasticity + "," + minResourceElasticity + "," + maxResourceElasticity + ","
//                                + averageQualityElasticity + "," + minQualityElasticity + "," + maxQualityElasticity);
//                        writer.write("\n");
                    }

                }
            }

        }

        transaction.success();

        transaction.finish();

        return recommendations;
    }

    private void updateMetrics() {
        cloudServicesMetrics = new LinkedHashMap<>();
        cloudServicesMetrics.put(Metric.MetricType.QUALITY, new ArrayList<Metric>());
        cloudServicesMetrics.put(Metric.MetricType.COST, new ArrayList<Metric>());
        cloudServicesMetrics.put(Metric.MetricType.RESOURCE, new ArrayList<Metric>());

        Transaction transaction = dataAccess.startTransaction();
        for (CloudProvider p : CloudProviderDAO.getAllCloudProviders(dataAccess.getGraphDatabaseService())) {
            for (ServiceUnit unit : ServiceUnitDAO.getCloudServiceUnitsForCloudProviderNode(p.getId(), dataAccess.getGraphDatabaseService())) {
                updateServiceUnitMetrics(unit);
            }
        }

        transaction.success();
        transaction.finish();

    }

    private void updateServiceUnitMetrics(ServiceUnit unit) {
        for (Resource r : unit.getResourceProperties()) {
            for (Metric m : r.getProperties().keySet()) {
                if (!cloudServicesMetrics.get(m.getType()).contains(m)) {
                    cloudServicesMetrics.get(m.getType()).add(m);
                }
            }
        }

        for (Quality q : unit.getQualityProperties()) {
            for (Metric m : q.getProperties().keySet()) {
                if (!cloudServicesMetrics.get(m.getType()).contains(m)) {
                    cloudServicesMetrics.get(m.getType()).add(m);
                }
            }
        }
        for (CostFunction cf : unit.getCostFunctions()) {
            for (CostElement ce : cf.getCostElements()) {
                Metric m = ce.getCostMetric();
                if (!cloudServicesMetrics.get(m.getType()).contains(m)) {
                    cloudServicesMetrics.get(m.getType()).add(m);
                }
            }
        }

        for (ElasticityCapability capability : unit.getElasticityCapabilities(Quality.class)) {
            for (ElasticityCapability.Dependency dependency : capability.getCapabilityDependencies()) {
                Quality r = (Quality) dependency.getTarget();
                for (Metric m : r.getProperties().keySet()) {
                    if (!cloudServicesMetrics.get(m.getType()).contains(m)) {
                        cloudServicesMetrics.get(m.getType()).add(m);
                    }
                }

            }
        }
        for (ElasticityCapability capability : unit.getElasticityCapabilities(CostFunction.class)) {
            for (ElasticityCapability.Dependency dependency : capability.getCapabilityDependencies()) {
                CostFunction cf = (CostFunction) dependency.getTarget();
                for (CostElement ce : cf.getCostElements()) {
                    Metric m = ce.getCostMetric();
                    if (!cloudServicesMetrics.get(m.getType()).contains(m)) {
                        cloudServicesMetrics.get(m.getType()).add(m);
                    }
                }

            }
        }

        for (ElasticityCapability capability : unit.getElasticityCapabilities(ServiceUnit.class)) {
            for (ElasticityCapability.Dependency dependency : capability.getCapabilityDependencies()) {
                ServiceUnit serviceUnit = (ServiceUnit) dependency.getTarget();
                updateServiceUnitMetrics(serviceUnit);
            }
        }
    }

    public void addToRequirements(String jsonRepr) {
        processAddToRequirementsJSONCommand(jsonRepr);

        log.debug("Adding " + jsonRepr);
    }

    public void removeFromRequirements(String jsonRepr) {
        processRemoveRequirementsJSONCommand(jsonRepr);
        log.debug("Removing " + jsonRepr);
    }

    private void processAddToRequirementsJSONCommand(String json) {

        //TODO: To actually  get the names from somwehere and conditions and etc. I need to do a bit more management
        //JSON looks like { "command" : "add", "type": "' + type+ '" "trace" : [{"name" : "ServiceReqs_overall_elasticity_multi", "type" : "SERVICE"}]};
        Object command = JSONValue.parse(json);
        JSONObject jSONcommandObject = (JSONObject) command;
        JSONObject metaInfo = (JSONObject) jSONcommandObject.get("meta");

        String whatToAdd = jSONcommandObject.get("type").toString();
        JSONArray trace = (JSONArray) jSONcommandObject.get("trace");

        JSONObject jSONRootObject = (JSONObject) trace.remove(0);
        String namejSONRootObject = jSONRootObject.get("name").toString();
        String typejSONRootObject = jSONRootObject.get("type").toString();

        //in order we traverse from root
        if (!requirements.getName().equals(namejSONRootObject) || !requirements.getLevel().toString().equals(typejSONRootObject)) {
            throw new RuntimeException("Something bad, as The requirements root does not match");
        }

        //we go one by one with JSON and get corect requirements children
        //first we get through the multi level requirements
        MultiLevelRequirements currentReqs = requirements;

        while (!currentReqs.getContainedElements().isEmpty() && !trace.isEmpty()) {
            {
                Object obj = trace.get(0);
                boolean discovered = false;
                JSONObject jSONObject = (JSONObject) obj;
                String name = jSONObject.get("name").toString();
                String type = jSONObject.get("type").toString();
                for (MultiLevelRequirements r : currentReqs.getContainedElements()) {
                    if (r.getName().equals(name) && r.getLevel().toString().equals(type)) {
                        currentReqs = r;
                        discovered = true;
                        break;
                    }
                }
                //so If for example I want to add Requirement on Requirements from Service, it will not be found in the MultiLevelRequirements (topology)
                //so, i need to keep it
                if (discovered) {
                    trace.remove(0);
                } else {
                    //if not discovered after a run, break while loop
                    break;
                }
            }
        }
        //here we are at a point in which we need to start looking in individual requirements blocks, then individual requirements, then conditions
        //only if we add Requirements or Conditions. Otherwise all work on multi level reqs
        switch (whatToAdd) {
            case "Strategy": {

                String strategyType = metaInfo.get("strategy").toString();
                Strategy s = new Strategy().withCategoryString(strategyType);

                if (!currentReqs.getOptimizationStrategies().contains(s)) {
                    currentReqs.addStrategy(s);
                }
                break;
            }
            case "Topology": {
                MultiLevelRequirements levelRequirements = new MultiLevelRequirements(MonitoredElement.MonitoredElementLevel.SERVICE_TOPOLOGY);
                String topologyName = metaInfo.get("name").toString();
                levelRequirements.setName(topologyName);
                if (!currentReqs.getContainedElements().contains(levelRequirements)) {
                    currentReqs.addMultiLevelRequirements(levelRequirements);
                }
            }
            break;
            case "Unit": {
                MultiLevelRequirements levelRequirements = new MultiLevelRequirements(MonitoredElement.MonitoredElementLevel.SERVICE_UNIT);
                String unitName = metaInfo.get("name").toString();
                levelRequirements.setName(unitName);
                if (!currentReqs.getContainedElements().contains(levelRequirements)) {
                    currentReqs.addMultiLevelRequirements(levelRequirements);
                }
            }
            break;
            case "Requirements": {

                Requirements requirements = new Requirements();
                String name = metaInfo.get("name").toString();

                requirements.setName(name);
                if (!currentReqs.getUnitRequirements().contains(requirements)) {
                    currentReqs.addRequirements(requirements);
                }
            }
            break;
            case "Requirement": //here we need to continue to get the targeted Requirements block
            {

                Object obj = trace.remove(0);
                JSONObject jSONObject = (JSONObject) obj;
                String name = jSONObject.get("name").toString();
                String type = jSONObject.get("type").toString();
                for (Requirements r : currentReqs.getUnitRequirements()) {
                    if (r.getName().equals(name)) {
                        Requirement requirement = new Requirement();
                        JSONObject metricInfo = (JSONObject) metaInfo.get("metric");
                        Metric metric = new Metric(metricInfo.get("name").toString(), metricInfo.get("unit").toString());
                        switch (metricInfo.get("type").toString()) {
                            case "COST":
                                metric.setType(Metric.MetricType.COST);
                                break;
                            case "RESOURCE":
                                metric.setType(Metric.MetricType.RESOURCE);
                                break;
                            case "QUALITY":
                                metric.setType(Metric.MetricType.QUALITY);
                                break;

                        }

//                                                + ', "unit": "' + selectedMetric.unit + '", "type":"' + selectedMetric.type + '"}';
                        requirement.setName(metricInfo.get("name").toString());
                        requirement.setMetric(metric);

                        r.addRequirement(requirement);
                        break;
                    }
                }
            }
            break;
            case "Condition": //here we also need to get the requirement
            {
                Object obj = trace.remove(0);
                JSONObject jSONObject = (JSONObject) obj;
                String name = jSONObject.get("name").toString();
                String type = jSONObject.get("type").toString();
                for (Requirements r : currentReqs.getUnitRequirements()) {
                    if (r.getName().equals(name)) {
                        Object reqObj = trace.remove(0);
                        JSONObject reqjSONObject = (JSONObject) reqObj;
                        String reqname = reqjSONObject.get("name").toString();
                        String reqtype = reqjSONObject.get("type").toString();

                        for (Requirement req : r.getRequirements()) {
                            if (req.getName().equals(reqname)) {

                                Condition condition = new Condition();

//                                var selectedConditionType = conditionTypeSelect.options[conditionTypeSelect.selectedIndex];
//                                        var conditionData = '{ "type":"' + selectedConditionType.text + '"'
//                                                + ', "value": "' + f.conditionValue.text + '"}';
//                                        data = '{' + data + ', "meta" : { "condition":' + conditionData + ' } ' + '}';
                                JSONObject conditionJSON = (JSONObject) metaInfo.get("condition");

                                switch (conditionJSON.get("type").toString()) {
                                    case "LESS_THAN":
                                        condition.setType(Condition.Type.LESS_THAN);
                                        break;
                                    case "LESS_EQUAL":
                                        condition.setType(Condition.Type.LESS_EQUAL);
                                        break;
                                    case "GREATER_THAN":
                                        condition.setType(Condition.Type.GREATER_THAN);
                                        break;
                                    case "GREATER_EQUAL":
                                        condition.setType(Condition.Type.GREATER_EQUAL);
                                        break;
                                    case "EQUAL":
                                        condition.setType(Condition.Type.EQUAL);
                                        break;
                                    case "RANGE":
                                        condition.setType(Condition.Type.RANGE);
                                        break;
                                    case "ENUMERATION":
                                        condition.setType(Condition.Type.ENUMERATION);
                                        break;
                                }

                                List<MetricValue> metricValues = new ArrayList<>();
                                metricValues.add(new MetricValue(conditionJSON.get("value").toString()));
                                condition.setValue(metricValues);

                                req.addCondition(condition);

                                break;
                            }
                        }
                        break;
                    }
                }

            }

        }
    }

    private void processRemoveRequirementsJSONCommand(String json) {

        //use META DATA FIELDs for rest of details for adding/removing shit
        //TODO: To actually  get the names from somwehere and conditions and etc. I need to do a bit more management
        //JSON looks like { "command" : "add", "type": "' + type+ '" "trace" : [{"name" : "ServiceReqs_overall_elasticity_multi", "type" : "SERVICE"}]};
        Object command = JSONValue.parse(json);
        JSONObject jSONcommandObject = (JSONObject) command;

        String whatToRemove = jSONcommandObject.get("type").toString();
        JSONArray trace = (JSONArray) jSONcommandObject.get("trace");

        JSONObject jSONRootObject = (JSONObject) trace.remove(0);
        String namejSONRootObject = jSONRootObject.get("name").toString();
        String typejSONRootObject = jSONRootObject.get("type").toString();

        //in order we traverse from root
        if (!requirements.getName().equals(namejSONRootObject) || !requirements.getLevel().toString().equals(typejSONRootObject)) {
            throw new RuntimeException("Something bad, as The requirements root does not match");
        }

        //we go one by one with JSON and get corect requirements children
        //first we get through the multi level requirements
        MultiLevelRequirements previousReqs = null;
        MultiLevelRequirements currentReqs = requirements;

        while (!currentReqs.getContainedElements().isEmpty() && !trace.isEmpty()) {
            {
                Object obj = trace.get(0);
                JSONObject jSONObject = (JSONObject) obj;
                String name = jSONObject.get("name").toString();
                String type = jSONObject.get("type").toString();
                boolean somethingMatched = false;
                for (MultiLevelRequirements r : currentReqs.getContainedElements()) {
                    if (r.getName().equals(name) && r.getLevel().toString().equals(type)) {
                        previousReqs = currentReqs;
                        currentReqs = r;

                        //if we matched, remove it from trace, else leave it for future matching
                        trace.remove(0);
                        somethingMatched = true;
                        break;
                    }
                }
                //need to break, as I might add strategies/requirements to any level (i.e. service)
                // so no need to traverse everything
                if (!somethingMatched) {
                    break;
                }

            }
        }
        //here we are at a point in which we need to start looking in individual requirements blocks, then individual requirements, then conditions
        //only if we add Requirements or Conditions. Otherwise all work on multi level reqs
        switch (whatToRemove) {

            case "Strategy": {
                Object obj = trace.remove(0);
                JSONObject jSONObject = (JSONObject) obj;
                String name = jSONObject.get("name").toString();
                String type = jSONObject.get("type").toString();

                Strategy s = new Strategy().withCategoryString(name);

                for (Strategy strategy : currentReqs.getOptimizationStrategies()) {
                    if (strategy.getStrategyCategory().equals(s.getStrategyCategory())) {
                        currentReqs.removeStrategy(s);
                        break;
                    }

                }
            }
            break;

            case "Topology": {

                if (previousReqs.getContainedElements().contains(currentReqs)) {
                    previousReqs.removeMultiLevelRequirements(currentReqs);
                }
            }
            break;
            case "Unit": {
                if (previousReqs.getContainedElements().contains(currentReqs)) {
                    previousReqs.removeMultiLevelRequirements(currentReqs);
                }
            }
            break;
            case "Requirements": {
                Object obj = trace.remove(0);
                JSONObject jSONObject = (JSONObject) obj;
                String name = jSONObject.get("name").toString();

                Requirements requirements = new Requirements();
                requirements.setName(name);
                if (currentReqs.getUnitRequirements().contains(requirements)) {
                    currentReqs.removeRequirements(requirements);
                }
            }
            break;
            case "Requirement": //here we need to continue to get the targeted Requirements block
            {
                Object obj = trace.remove(0);
                JSONObject jSONObject = (JSONObject) obj;
                String name = jSONObject.get("name").toString();
                String type = jSONObject.get("type").toString();
                for (Requirements r : currentReqs.getUnitRequirements()) {
                    if (r.getName().equals(name)) {

                        Object reqO = trace.remove(0);
                        JSONObject jSONObjectReqo = (JSONObject) reqO;
                        String nameReqo = jSONObjectReqo.get("name").toString();

                        Requirement requirement = new Requirement();
                        requirement.setName(nameReqo);

                        for (Requirement req : r.getRequirements()) {
                            if (req.getName().equals(nameReqo)) {
                                r.getRequirements().remove(req);
                                break;
                            }
                        }

                        break;
                    }
                }
            }
            break;

            case "Condition": //here we also need to get the requirement
            {
                Object obj = trace.remove(0);
                JSONObject jSONObject = (JSONObject) obj;
                String name = jSONObject.get("name").toString();
                String type = jSONObject.get("type").toString();
                for (Requirements r : currentReqs.getUnitRequirements()) {
                    if (r.getName().equals(name)) {
                        Object reqObj = trace.remove(0);
                        JSONObject reqjSONObject = (JSONObject) reqObj;
                        String reqname = reqjSONObject.get("name").toString();
                        String reqtype = reqjSONObject.get("type").toString();

                        for (Requirement req : r.getRequirements()) {
                            if (req.getName().equals(reqname)) {
                                Object rcondO = trace.remove(0);
                                JSONObject jSONObjectCondo = (JSONObject) rcondO;
                                String nameCondo = jSONObjectCondo.get("name").toString();

                                Condition condition = new Condition();
                                switch (nameCondo) {

                                    case "ENUMERATION":
                                        condition.setType(Condition.Type.ENUMERATION);
                                        break;
                                    case "EQUAL":
                                        condition.setType(Condition.Type.EQUAL);
                                        break;
                                    case "GREATER_EQUAL":
                                        condition.setType(Condition.Type.GREATER_EQUAL);
                                        break;
                                    case "LESS_EQUAL":
                                        condition.setType(Condition.Type.LESS_EQUAL);
                                        break;
                                    case "LESS_THAN":
                                        condition.setType(Condition.Type.LESS_THAN);
                                        break;
                                    case "RANGE":
                                        condition.setType(Condition.Type.RANGE);
                                        break;
                                }

                                for (Condition c : req.getConditions()) {
                                    if (c.getType().equals(condition.getType())) {
                                        req.getConditions().remove(c);
                                        break;
                                    }
                                }

                                break;
                            }
                        }
                        break;
                    }
                }

            }

        }
    }

}
