/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.util;

import at.ac.tuwien.dsg.quelle.cloudServicesModel.concepts.CloudProvider;
import at.ac.tuwien.dsg.quelle.cloudServicesModel.requirements.MultiLevelRequirements;
import java.io.InputStream;
import javax.xml.bind.JAXBContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 *
 * @author Daniel Moldovan E-Mail: d.moldovan@dsg.tuwien.ac.at
 */
@Component
public class ConfigurationUtil {

    static final Logger log = LoggerFactory.getLogger(ConfigurationUtil.class);

    @Autowired
    private ApplicationContext context;

    @Value("${CONFIG_DIR}")
    private String configDir;

    public MultiLevelRequirements createDefaultRequirements() {

        log.debug("Loading file://" + configDir + "/default/requirements.xml");
//        MultiLevelRequirements requirements = unmarshalFragment(MultiLevelRequirements.class, "file://" + configDir + "/default/requirements.xml");
        MultiLevelRequirements requirements = unmarshalFragment(MultiLevelRequirements.class, "file:///home/daniel-tuwien/Documents/DSG_SVN/software/MELA/MELA-SESConstruction/CloudServicesSelection/config/default/requirements.xml");
        log.debug("Loaded " + requirements + " as requirements");
        return requirements;
    }

    public CloudProvider createAmazonDefaultCloudProvider() {

        log.debug("Loading file://" + configDir + "/default/amazonDescription.xml");
//        MultiLevelRequirements requirements = unmarshalFragment(MultiLevelRequirements.class, "file://" + configDir + "/default/amazonDescription.xml");
        CloudProvider cloudProvider = unmarshalFragment(CloudProvider.class, "file:///home/daniel-tuwien/Documents/DSG_SVN/software/MELA/MELA-SESConstruction/CloudServicesSelection/config/default/amazonDescription.xml");
        log.debug("Loaded " + cloudProvider + " as provider");
        return cloudProvider;
    }

    @SuppressWarnings("unchecked")
    private <T> T unmarshalFragment(Class<T> fragmentType, String filename) {
        try {
            JAXBContext jAXBContext = JAXBContext.newInstance(fragmentType);
            InputStream fileStream = context.getResource(filename).getInputStream();
            return (T) jAXBContext.createUnmarshaller().unmarshal(fileStream);
        } catch (Exception ex) {
            log.error("Cannot unmarshall : {}", ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
            return null;
        }

    }
}
