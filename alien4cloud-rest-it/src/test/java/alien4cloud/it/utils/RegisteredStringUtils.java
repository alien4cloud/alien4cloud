package alien4cloud.it.utils;

import alien4cloud.it.Context;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xdegenne on 27/03/2017.
 */
public class RegisteredStringUtils {

    private static final Pattern REGISTERED_STRING_PATTERN = Pattern.compile("(.*)\\{\\{(.+)\\}\\}(.*)");

    public static final Object parseAndReplace(Object input) {
        Object result = input;
        Matcher matcher = REGISTERED_STRING_PATTERN.matcher(result.toString());
        while(matcher.matches()) {
            result = matcher.group(1) + Context.getInstance().getRegisteredStringContent(matcher.group(2)) + matcher.group(3);
            matcher = REGISTERED_STRING_PATTERN.matcher(result.toString());
        }
        return result;
    }

}
