package alien4cloud.it.utils;

import java.util.HashMap;
import java.util.Map;

import cucumber.api.DataTable;

/**
 *
 */
public class ConfigurationStringUtils {

    /**
     * Allow to parse a table of key value to a map
     * @param table
     * @return  A map
     */
    public static Map<String, Object> dataTableToMap(DataTable table) {
        Map<String, Object> configuration = new HashMap<>();
        table.getGherkinRows().stream().forEach(dataTableRow -> {
            String key = dataTableRow.getCells().get(0);
            String value = dataTableRow.getCells().get(1);
            Object processedValue = System.getenv(value);
            if (processedValue == null || ((String) processedValue).isEmpty()) {
                processedValue = value;
            }
            // Convert to raw boolean or integer if possible.
            if (processedValue.equals("true")) {
                processedValue = true;
            } else if (processedValue.equals("false")) {
                processedValue = false;
            } else {
                try {
                    processedValue = Integer.valueOf((String) processedValue);
                } catch (NumberFormatException e) {
                }
            }
            configuration.put(key, processedValue);
        });
        return configuration;
    }
}
