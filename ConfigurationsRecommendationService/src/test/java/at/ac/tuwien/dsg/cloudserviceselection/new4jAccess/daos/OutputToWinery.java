///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package at.ac.tuwien.dsg.cloudserviceselection.new4jAccess.daos;
//
//import at.ac.tuwien.dsg.cloudofferedservices.concepts.CloudProvider;
//import at.ac.tuwien.dsg.cloudofferedservices.new4jAccess.DataAccess;
//import at.ac.tuwien.dsg.cloudofferedservices.new4jAccess.daos.CloudProviderDAO;
//import at.ac.tuwien.dsg.cloudserviceselection.util.writers.CloudServicesToOpenTosca;
//
//import java.io.File;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import javax.transaction.TransactionManager;
//
//import javax.xml.bind.JAXBContext;
//import javax.xml.bind.Unmarshaller;
//
//import junit.framework.TestCase;
//import org.neo4j.graphdb.Transaction;
//import org.springframework.beans.factory.annotation.Value;
//
///**
// *
// * @author daniel-tuwien
// */
//public class OutputToWinery extends TestCase {
//
//    @Value("${dataAccess}")
//    private DataAccess access;
//    private Transaction transaction;
//
//    public OutputToWinery(String testName) {
//        super(testName);
//    }
//
//    @Override
//    protected void setUp() throws Exception {
//        access = new DataAccess("/tmp/neo4j");
//        access.clear();
//
//        transaction = access.startTransaction();
//
//        super.setUp();
//    }
//
//    @Override
//    protected void tearDown() throws Exception {
//        super.tearDown();
//        access.clear();
//        transaction.success();
//        transaction.finish();
//        access.getGraphDatabaseService().shutdown();
//    }
//
//    /**
//     * Test of matchServiceUnit method, of class RequirementsMatchingEngine.
//     */
//    public void testEcosystemDescription() throws IOException {
//
//        List<CloudProvider> cloudProviders = new ArrayList<CloudProvider>();
//
//        //
//        // ==========================================================================================
//        // amazon cloud description
//        try {
//            JAXBContext context = JAXBContext.newInstance(CloudProvider.class);
//            Unmarshaller unmarshaller = context.createUnmarshaller();
//            CloudProvider provider = (CloudProvider) unmarshaller.unmarshal(new File("./experiments/amazonDescription.xml"));
//            cloudProviders.add(provider);
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//
//        CloudProviderDAO.persistCloudProviders(cloudProviders, access.getGraphDatabaseService());
//
//        CloudServicesToOpenTosca cloudServicesToOpenTosca = new CloudServicesToOpenTosca();
//
//        //I need to write for all services new types (except VM) , for which I can just instantiate the existing type
//        cloudServicesToOpenTosca.createServiceTypes(cloudProviders.get(0), "./OpenToscaOutput/nodeTypes");
//
//    }
//
//}
