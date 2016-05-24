package alien4cloud.tosca.normative;

import alien4cloud.utils.VersionUtil;
import alien4cloud.utils.version.InvalidVersionException;
import alien4cloud.utils.version.Version;

/**
 * @author Minh Khang VU
 */
public class VersionType implements IComparablePropertyType<Version> {

    public static final String NAME = "version";

    @Override
    public Version parse(String text) throws InvalidPropertyValueException {
        try {
            return VersionUtil.parseVersion(text);
        } catch (InvalidVersionException e) {
            throw new InvalidPropertyValueException("Could not parse version from value " + text, e);
        }
    }

    @Override
    public String print(Version value) {
        return value.toString();
    }

    @Override
    public String getTypeName() {
        return NAME;
    }
}
