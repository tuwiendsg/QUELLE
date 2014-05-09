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
package at.ac.tuwien.dsg.quelle.csvOutputFormatters;

import at.ac.tuwien.dsg.quelle.elasticityQuantification.engines.CloudServiceUnitAnalysisEngine.AnalysisResult;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 *
 * @Author Daniel Moldovan
 * @E-mail: d.moldovan@dsg.tuwien.ac.at
 *
 */
public class AnalysisResultCSVWriter {

    public static void writeAnalysisResult(List<AnalysisResult> result, String file) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));

        //write columns
        {
            AnalysisResult analysisResult = result.get(0);
            String columnsLine = "Service Unit";
            for (String key : analysisResult.getResultFields()) {
                columnsLine += "," + key;
            }
            writer.write(columnsLine);
            writer.newLine();

        }

        for (AnalysisResult analysisResult : result) {
            String line = analysisResult.getUnit().getName();
            for (String key : analysisResult.getResultFields()) {
                line += "," + analysisResult.getValue(key);
            }
            writer.write(line);
            writer.newLine();
        }

        writer.flush();
        writer.close();
    }
}
