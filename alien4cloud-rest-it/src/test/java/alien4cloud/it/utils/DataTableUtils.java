package alien4cloud.it.utils;

import java.util.HashMap;
import java.util.Map;

import cucumber.api.DataTable;

/**
 *
 */
public class DataTableUtils {

    /**
     * Allow to parse a table of key value to a map and it supports the key being an environment variable
     * @param table a data table
     * @return  a map
     */
    public static Map<String, Object> dataTableToMap(DataTable table) {
        Map<String, Object> configuration = new HashMap<>();
        table.getGherkinRows().forEach(dataTableRow -> {
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
