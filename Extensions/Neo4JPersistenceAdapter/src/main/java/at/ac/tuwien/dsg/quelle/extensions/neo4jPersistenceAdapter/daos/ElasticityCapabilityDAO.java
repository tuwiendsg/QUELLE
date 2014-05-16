/**
 * Copyright 2013 Technische Universitaet Wien (TUW), Distributed Systems Group
 * E184
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.daos;

import at.ac.tuwien.dsg.quelle.extensions.neo4jPersistenceAdapter.daos.helper.ServiceUnitRelationship;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.Metric;
import at.ac.tuwien.dsg.mela.common.monitoringConcepts.MetricValue;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CostFunction;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.ElasticityCapability;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.ElasticityCapability.Dependency;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Entity;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Quality;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Resource;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.ServiceUnit;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.Volatility;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @Author Daniel Moldovan
 * @E-mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
public class ElasticityCapabilityDAO {
    
    static final Logger log = LoggerFactory.getLogger(ElasticityCapabilityDAO.class);

    public static final Label LABEL = new Label() {
        public String name() {
            return "ElasticityCapability";
        }
    };
    public static final String KEY = "name";
    public static final String TYPE = "type";
    public static final String PHASE = "phase";
    public static final String VOLATILITY_TIME_UNIT = "minimumLifetimeInHours";
    public static final String VOLATILITY_MAX_CHANGES = "maxNrOfChanges";

    public static final String PROPERTY_SEPARATOR = ":";
//    public static final String ELASTICITY_CHARACTERISTIC_VALUES_SEPARATOR = ",";

    private ElasticityCapabilityDAO() {
    }

    public static List<ElasticityCapability> getELasticityCapabilitiesForNode(Long id, EmbeddedGraphDatabase database) {
        List<ElasticityCapability> elasticityCapabilities = new ArrayList<ElasticityCapability>();

        try {
            Node parentNode = database.getNodeById(id);

            if (parentNode == null) {
                return elasticityCapabilities;
            }

            TraversalDescription description = Traversal.traversal()
                    .evaluator(Evaluators.excludeStartPosition())
                    .relationships(ServiceUnitRelationship.hasElasticityCapability, Direction.OUTGOING)
                    .uniqueness(Uniqueness.NODE_PATH);
            Traverser traverser = description.traverse(parentNode);
            for (Path path : traverser) {

                Node lastPathNode = path.endNode();

                List<Dependency> elasticityCapabilityTargets = null;
                //TODO: this might be an issue if I want diff target types per same elasticity capability
                //search target in Resources
                {
                    List<Dependency> result = ResourceDAO.getElasticityCapabilityTargetResourcesForNode(lastPathNode.getId(), database);
                    if (!result.isEmpty()) {
                        elasticityCapabilityTargets = new ArrayList<Dependency>(result);
                    }
                }
                if (elasticityCapabilityTargets == null) {
                    List<Dependency> result = QualityDAO.getElasticityCapabilityTargetsQualityForNode(lastPathNode.getId(), database);
                    if (!result.isEmpty()) {
                        elasticityCapabilityTargets = new ArrayList<Dependency>(result);
                    }
                }

                if (elasticityCapabilityTargets == null) {
                    List<Dependency> result = ServiceUnitDAO.getElasticityCapabilitiesTargetServiceUnitForNode(lastPathNode.getId(), database);
                    if (!result.isEmpty()) {
                        elasticityCapabilityTargets = new ArrayList<Dependency>(result);
                    }
                }

                if (elasticityCapabilityTargets == null) {
                    List<Dependency> result = CostFunctionDAO.getElasticityCapabilitiesTargetsCostFunctionNode(lastPathNode.getId(), database);
                    if (!result.isEmpty()) {
                        elasticityCapabilityTargets = new ArrayList<Dependency>(result);
                    }
                }

                //if still null, something bad happend
                if (elasticityCapabilityTargets == null) {
                    log.warn( "ElasticityCapability for " + lastPathNode.getProperty(KEY).toString() + " does not have target");
                    return elasticityCapabilities;
                }

                ElasticityCapability capability = new ElasticityCapability();
                capability.setCapabilityDependencies(elasticityCapabilityTargets);
                capability.setId(lastPathNode.getId());

                if (lastPathNode.hasProperty(KEY)) {
                    String name = lastPathNode.getProperty(KEY).toString();
                    capability.setName(name);
                } else {
                    log.warn( "Retrieved ElasticityCapability " + lastPathNode + " has no " + KEY);
                }

                //get type and phase from relationship
                Relationship relationship = path.lastRelationship();
//                if (relationship.hasProperty(TYPE)) {
//                    String type = relationship.getProperty(TYPE).toString();
//                    capability.setType(type);
//                } else {
//                    log.warn( "Retrieved ElasticityCapability " + lastPathNode + " has no " + TYPE);
//                }

                if (relationship.hasProperty(PHASE)) {
                    String type = relationship.getProperty(PHASE).toString();
                    capability.setPhase(type);
                } else {
                    log.warn( "Retrieved ElasticityCapability " + lastPathNode + " has no " + PHASE);
                }

//                if (relationship != null) {
//                    for (String propertyKey : relationship.getPropertyKeys()) {
//                        String[] metricInfo = propertyKey.split(ELASTICITY_CHARACTERISTIC_VALUES_SEPARATOR);
//                        for (String metricString : metricInfo) {
//                            MetricValue metricValue = new MetricValue(metricString);
//                            capability.addOption(metricValue);
//                        }
//
//                    }
//            } 
//
//    
//        else {
//                    log.warn( "No relationship found of type " + UtilityRelationship.HAS_RESOURCE + " starting from " + parentNode + " and ending at " + lastPathNode);
//    }
                if (capability != null) {
                    elasticityCapabilities.add(capability);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            e.printStackTrace();
        } finally {
        }

        return elasticityCapabilities;
    }

    /**
     * DOES NOT return also properties embedded on the resource relationships
     *
     * @param resourceToSearchFor
     * @param database
     * @return
     */
    public static ElasticityCapability searchForElasticityCapabilitiesUniqueResult(ElasticityCapability resourceToSearchFor, EmbeddedGraphDatabase database) {
        ElasticityCapability elasticityCapability = null;

        for (Node node : database.findNodesByLabelAndProperty(LABEL, KEY, resourceToSearchFor.getName())) {

            List<Dependency> elasticityCapabilityTargets = null;
            //TODO: this might be an issue if I want diff target types per same elasticity capability
            //search target in Resources
            {
                List<Dependency> result = ResourceDAO.getElasticityCapabilityTargetResourcesForNode(node.getId(), database);
                if (!result.isEmpty()) {
                    elasticityCapabilityTargets = new ArrayList<Dependency>(result);
                }
            }
            if (elasticityCapabilityTargets == null) {
                List<Dependency> result = QualityDAO.getElasticityCapabilityTargetsQualityForNode(node.getId(), database);
                if (!result.isEmpty()) {
                    elasticityCapabilityTargets = new ArrayList<Dependency>(result);
                }
            }

            if (elasticityCapabilityTargets == null) {
                List<Dependency> result = ServiceUnitDAO.getElasticityCapabilitiesTargetServiceUnitForNode(node.getId(), database);
                if (!result.isEmpty()) {
                    elasticityCapabilityTargets = new ArrayList<Dependency>(result);
                }
            }

            if (elasticityCapabilityTargets == null) {
                List<Dependency> result = CostFunctionDAO.getElasticityCapabilitiesTargetsCostFunctionNode(node.getId(), database);
                if (!result.isEmpty()) {
                    elasticityCapabilityTargets = new ArrayList<Dependency>(result);
                }
            }

            elasticityCapability = new ElasticityCapability();
            elasticityCapability.setCapabilityDependencies(elasticityCapabilityTargets);
            elasticityCapability.setId(node.getId());

            if (node.hasProperty(KEY)) {
                String name = node.getProperty(KEY).toString();
                elasticityCapability.setName(name);
            } else {
                log.warn( "Retrieved ElasticityCapability " + resourceToSearchFor + " has no " + KEY);
            }

//            if (node.hasProperty(TYPE)) {
//                String type = node.getProperty(TYPE).toString();
//                elasticityCapability.setType(type);
//            } else {
//                log.warn( "Retrieved CloudProvider " + resourceToSearchFor + " has no " + TYPE);
//            }
//
//            if (node.hasProperty(PHASE)) {
//                String type = node.getProperty(PHASE).toString();
//                elasticityCapability.setPhase(type);
//            } else {
//                log.warn( "Retrieved CloudProvider " + resourceToSearchFor + " has no " + PHASE);
//            }
            if (!resourceToSearchFor.equals(elasticityCapability)) {
                continue;
            }

//
//            for (String propertyKey : node.getPropertyKeys()) {
//
//                if (propertyKey.equals(KEY)) {
//                    elasticityCapability.setName(node.getProperty(KEY).toString());
//                } else if (propertyKey.equals(METRIC)) {
//                    String[] metricInfo = propertyKey.split(PROPERTY_SEPARATOR);
//                    if (metricInfo.length < 2) {
//                        log.warn( "Retrieved property " + propertyKey + " does not respect format metricName:metricUnit");
//                    } else {
//                        Metric metric = new Metric(metricInfo[0], metricInfo[1]);
//                        elasticityCapability.setCapabilityMetric(metric);
//                    }
//                }
//            }
            break;
        }

//        if (elasticityCapability == null) {
//            log.warn( "ElasticityCapability " + resourceToSearchFor + " was not found");
//        }
        return elasticityCapability;
    }

    /**
     * Actually persists only CloudProvider and Properties
     *
     * @param resourceToPersist
     * @param database connection to DB
     */
    public static Node persistElasticityCapability(ElasticityCapability resourceToPersist, EmbeddedGraphDatabase database) {

        Node elCharactNode = null;

        elCharactNode = database.createNode();
        elCharactNode.setProperty(KEY, resourceToPersist.getName());

        elCharactNode.addLabel(LABEL);
        List<Dependency> dependencys = resourceToPersist.getCapabilityDependencies();

        for (Dependency dependency : dependencys) {

            Entity capabilityTarget = dependency.getTarget();
            //persist target. 
            //the ElasticityCapability values are persisted per individual CloudServiceUnit relationship HAS_ELASTICITY_CHARACTERISTIC
            if (capabilityTarget instanceof Resource) {
                Resource resourceCapabilityTarget = (Resource) capabilityTarget;
                Resource targetFound = ResourceDAO.searchForResourcesUniqueResult(resourceCapabilityTarget, database);
                //costFunction does not exist need to persist it
                Node costElementNode = null;
                if (targetFound == null) {
                    costElementNode = ResourceDAO.persistResource(resourceCapabilityTarget, database);
                } else {
                    //retrieve the costFunction to have its ID
                    //add relationship from CostElement to CloudProvider
                    costElementNode = database.getNodeById(targetFound.getId());
                }

                Relationship relationship = elCharactNode.createRelationshipTo(costElementNode, ServiceUnitRelationship.elasticityCapabilityFor);
                relationship.setProperty(TYPE, dependency.getDependencyType());

                Volatility volatility = dependency.getVolatility();
                relationship.setProperty(VOLATILITY_TIME_UNIT, volatility.getMinimumLifetimeInHours());
                relationship.setProperty(VOLATILITY_MAX_CHANGES, volatility.getMaxNrOfChanges());

            } else if (capabilityTarget instanceof Quality) {
                Quality qualityCapabilityTarget = (Quality) capabilityTarget;
                Quality targetFound = QualityDAO.searchForQualityEntitiesUniqueResult(qualityCapabilityTarget, database);
                //costFunction does not exist need to persist it
                Node costElementNode = null;
                if (targetFound == null) {
                    costElementNode = QualityDAO.persistQualityEntity(qualityCapabilityTarget, database);
                } else {
                    //retrieve the costFunction to have its ID
                    //add relationship from CostElement to CloudProvider
                    costElementNode = database.getNodeById(targetFound.getId());
                }

                Relationship relationship = elCharactNode.createRelationshipTo(costElementNode, ServiceUnitRelationship.elasticityCapabilityFor);
                relationship.setProperty(TYPE, dependency.getDependencyType());

                Volatility volatility = dependency.getVolatility();
                relationship.setProperty(VOLATILITY_TIME_UNIT, volatility.getMinimumLifetimeInHours());
                relationship.setProperty(VOLATILITY_MAX_CHANGES, volatility.getMaxNrOfChanges());

                /**
                 * Aimed to have ServiceUnit -> ELasticityCapability ->[Quality
                 * 1, Quality 2]
                 */
                for (Map.Entry<Metric, MetricValue> entry : qualityCapabilityTarget.getProperties().entrySet()) {
                    Metric metric = entry.getKey();
                    String propertyKey = metric.getName() + PROPERTY_SEPARATOR + metric.getMeasurementUnit();
                    relationship.setProperty(propertyKey, entry.getValue().getValue());
                }

            } else if (capabilityTarget instanceof ServiceUnit) {
                ServiceUnit serviceUnit = (ServiceUnit) capabilityTarget;
                ServiceUnit targetFound = ServiceUnitDAO.searchForCloudServiceUnitsUniqueResult(serviceUnit, database);
                //costFunction does not exist need to persist it
                Node costElementNode = null;
                if (targetFound == null) {
                    costElementNode = ServiceUnitDAO.persistServiceUnit(serviceUnit, database);
                } else {
                    //retrieve the costFunction to have its ID
                    //add relationship from CostElement to CloudProvider
                    costElementNode = database.getNodeById(targetFound.getId());
                }

                Relationship relationship = elCharactNode.createRelationshipTo(costElementNode, ServiceUnitRelationship.elasticityCapabilityFor);
                relationship.setProperty(TYPE, dependency.getDependencyType());

                Volatility volatility = dependency.getVolatility();
                relationship.setProperty(VOLATILITY_TIME_UNIT, volatility.getMinimumLifetimeInHours());
                relationship.setProperty(VOLATILITY_MAX_CHANGES, volatility.getMaxNrOfChanges());

            } else if (capabilityTarget instanceof CostFunction) {
                CostFunction costFunction = (CostFunction) capabilityTarget;
                CostFunction targetFound = CostFunctionDAO.searchForCostFunctionsUniqueResult(costFunction, database);
                //costFunction does not exist need to persist it
                Node costElementNode = null;
                if (targetFound == null) {
                    costElementNode = CostFunctionDAO.persistCostFunction(costFunction, database);
                } else {
                    //retrieve the costFunction to have its ID
                    //add relationship from CostElement to CloudProvider
                    costElementNode = database.getNodeById(targetFound.getId());
                }

                Relationship relationship = elCharactNode.createRelationshipTo(costElementNode, ServiceUnitRelationship.elasticityCapabilityFor);
                relationship.setProperty(TYPE, dependency.getDependencyType());

                Volatility volatility = dependency.getVolatility();
                relationship.setProperty(VOLATILITY_TIME_UNIT, volatility.getMinimumLifetimeInHours());
                relationship.setProperty(VOLATILITY_MAX_CHANGES, volatility.getMaxNrOfChanges());

            } else {
                log.warn( "Elasticity capability target entity " + capabilityTarget.getName() + " not instanceof Resource or Quality");
            }
        }

        return elCharactNode;

    }

    /**
     * Actually persists only CloudProvider and Properties
     *
     * @param resourceToPersist
     * @param database connection to DB
     */
    public static void persistElasticityCapabilities(List<ElasticityCapability> resourcesToPersist, EmbeddedGraphDatabase database) {

        for (ElasticityCapability resourceToPersist : resourcesToPersist) {
            Node elCharactNode = null;

            elCharactNode = database.createNode();
            elCharactNode.setProperty(KEY, resourceToPersist.getName());
//            elCharactNode.setProperty(TYPE, resourceToPersist.getType());
//            elCharactNode.setProperty(PHASE, resourceToPersist.getPhase());
            elCharactNode.addLabel(LABEL);

            List<Dependency> dependencys = resourceToPersist.getCapabilityDependencies();

            for (Dependency dependency : dependencys) {

                Entity capabilityTarget = dependency.getTarget();
                //persist target. 
                //the ElasticityCapability values are persisted per individual CloudServiceUnit relationship HAS_ELASTICITY_CHARACTERISTIC
                if (capabilityTarget instanceof Resource) {
                    Resource resourceCapabilityTarget = (Resource) capabilityTarget;
                    Resource targetFound = ResourceDAO.searchForResourcesUniqueResult(resourceCapabilityTarget, database);
                    //costFunction does not exist need to persist it
                    Node costElementNode = null;
                    if (targetFound == null) {
                        costElementNode = ResourceDAO.persistResource(resourceCapabilityTarget, database);
                    } else {
                        //retrieve the costFunction to have its ID
                        //add relationship from CostElement to CloudProvider
                        costElementNode = database.getNodeById(targetFound.getId());
                    }

                    Relationship relationship = elCharactNode.createRelationshipTo(costElementNode, ServiceUnitRelationship.elasticityCapabilityFor);
                    relationship.setProperty(TYPE, dependency.getDependencyType());

                    Volatility volatility = dependency.getVolatility();
                    relationship.setProperty(VOLATILITY_TIME_UNIT, volatility.getMinimumLifetimeInHours());
                    relationship.setProperty(VOLATILITY_MAX_CHANGES, volatility.getMaxNrOfChanges());

                } else if (capabilityTarget instanceof Quality) {
                    Quality qualityCapabilityTarget = (Quality) capabilityTarget;
                    Quality targetFound = QualityDAO.searchForQualityEntitiesUniqueResult(qualityCapabilityTarget, database);
                    //costFunction does not exist need to persist it
                    Node costElementNode = null;
                    if (targetFound == null) {
                        costElementNode = QualityDAO.persistQualityEntity(qualityCapabilityTarget, database);
                    } else {
                        //retrieve the costFunction to have its ID
                        //add relationship from CostElement to CloudProvider
                        costElementNode = database.getNodeById(targetFound.getId());
                    }

                    Relationship relationship = elCharactNode.createRelationshipTo(costElementNode, ServiceUnitRelationship.elasticityCapabilityFor);
                    relationship.setProperty(TYPE, dependency.getDependencyType());

                    Volatility volatility = dependency.getVolatility();
                    relationship.setProperty(VOLATILITY_TIME_UNIT, volatility.getMinimumLifetimeInHours());
                    relationship.setProperty(VOLATILITY_MAX_CHANGES, volatility.getMaxNrOfChanges());
                } else if (capabilityTarget instanceof ServiceUnit) {
                    ServiceUnit serviceUnit = (ServiceUnit) capabilityTarget;
                    ServiceUnit targetFound = ServiceUnitDAO.searchForCloudServiceUnitsUniqueResult(serviceUnit, database);
                    //costFunction does not exist need to persist it
                    Node costElementNode = null;
                    if (targetFound == null) {
                        costElementNode = ServiceUnitDAO.persistServiceUnit(serviceUnit, database);
                    } else {
                        //retrieve the costFunction to have its ID
                        //add relationship from CostElement to CloudProvider
                        costElementNode = database.getNodeById(targetFound.getId());
                    }

                    Relationship relationship = elCharactNode.createRelationshipTo(costElementNode, ServiceUnitRelationship.elasticityCapabilityFor);
                    relationship.setProperty(TYPE, dependency.getDependencyType());

                    Volatility volatility = dependency.getVolatility();
                    relationship.setProperty(VOLATILITY_TIME_UNIT, volatility.getMinimumLifetimeInHours());
                    relationship.setProperty(VOLATILITY_MAX_CHANGES, volatility.getMaxNrOfChanges());

                } else if (capabilityTarget instanceof CostFunction) {
                    CostFunction costFunction = (CostFunction) capabilityTarget;
                    CostFunction targetFound = CostFunctionDAO.searchForCostFunctionsUniqueResult(costFunction, database);
                    //costFunction does not exist need to persist it
                    Node costElementNode = null;
                    if (targetFound == null) {
                        costElementNode = CostFunctionDAO.persistCostFunction(costFunction, database);
                    } else {
                        //retrieve the costFunction to have its ID
                        //add relationship from CostElement to CloudProvider
                        costElementNode = database.getNodeById(targetFound.getId());
                    }

                    Relationship relationship = elCharactNode.createRelationshipTo(costElementNode, ServiceUnitRelationship.elasticityCapabilityFor);
                    relationship.setProperty(TYPE, dependency.getDependencyType());

                    Volatility volatility = dependency.getVolatility();
                    relationship.setProperty(VOLATILITY_TIME_UNIT, volatility.getMinimumLifetimeInHours());
                    relationship.setProperty(VOLATILITY_MAX_CHANGES, volatility.getMaxNrOfChanges());
                } else {
                    log.warn( "Elasticity capability target entity " + capabilityTarget.getName() + " not instanceof Resource or Quality");
                }
            }
        }

    }
}