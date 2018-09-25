package alien4cloud.metaproperty;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MetaPropertySubstitutionHelper {

    static <T> void subst(Map<String,T> values, Map<String,String> substitutions,String prefix) {
        Map<String,T> temp = new HashMap<>();

        if (values != null) {
            Iterator<String> i = values.keySet().iterator();

            prefix = prefix + ".";

            while (i.hasNext()) {
                String oKey = i.next();
                if (oKey.startsWith(prefix)) {
                    String sKey = oKey.substring(prefix.length());
                    if (substitutions.containsKey(sKey)) {
                        String nKey = prefix + substitutions.get(sKey);
                        temp.put(nKey, values.get(oKey));
                        i.remove();
                    }
                }
            }
            values.putAll(temp);
        }
    }
}
