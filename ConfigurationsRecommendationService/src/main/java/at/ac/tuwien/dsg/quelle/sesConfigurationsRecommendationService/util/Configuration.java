/**
 * Copyright 2013 Technische Universitaet Wien (TUW), 
 * Distributed Systems Group E184
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

package at.ac.tuwien.dsg.quelle.sesConfigurationsRecommendationService.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @Author Daniel Moldovan
 * @E-mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
public class Configuration {

    static Logger logger;

    static {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            
            InputStream log4jStream = classLoader.getResourceAsStream("/config/Log4j.properties");
             if(log4jStream == null){
               log4jStream = new FileInputStream(new File("./config/Log4j.properties")); 
            }

            
            if (log4jStream != null) {
                PropertyConfigurator.configure(log4jStream);
                String date = new Date().toString();
                date = date.replace(" ", "_");
                date = date.replace(":", "_");
                System.getProperties().put("recording_date", date);

                logger = Logger.getLogger("rootLogger");
            } else {
               logger = Logger.getLogger("rootLogger");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            logger = Logger.getLogger("rootLogger");
        }

    }

    public static Logger getLogger() {
        return logger;
    }
    
    
 
}
