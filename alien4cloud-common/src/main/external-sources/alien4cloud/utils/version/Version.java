package alien4cloud.utils.version;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.elasticsearch.annotation.NumberField;
import org.elasticsearch.annotation.StringField;
import org.elasticsearch.mapping.IndexType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Default implementation of artifact versioning.
 * 
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Version implements Comparable<Version> {
    @NumberField(index = IndexType.not_analyzed)
    private Integer majorVersion;

    @NumberField(index = IndexType.not_analyzed)
    private Integer minorVersion;

    @NumberField(index = IndexType.not_analyzed)
    private Integer incrementalVersion;

    @NumberField(index = IndexType.not_analyzed)
    private Integer buildNumber;

    @StringField(indexType = IndexType.not_analyzed)
    private String qualifier;

    @JsonIgnore
    private ComparableVersion comparable;

    public Version(String version) {
        parseVersion(version);
    }

    @Override
    public int hashCode() {
        return 11 + comparable.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Version)) {
            return false;
        }

        return compareTo((Version) other) == 0;
    }

    public int compareTo(Version otherVersion) {
        if (otherVersion instanceof Version) {
            return this.comparable.compareTo(((Version) otherVersion).comparable);
        } else {
            return compareTo(new Version(otherVersion.toString()));
        }
    }

    public Integer getMajorVersion() {
        return majorVersion != null ? majorVersion : Integer.valueOf(0);
    }

    public Integer getMinorVersion() {
        return minorVersion != null ? minorVersion : Integer.valueOf(0);
    }

    public Integer getIncrementalVersion() {
        return incrementalVersion != null ? incrementalVersion : Integer.valueOf(0);
    }

    public Integer getBuildNumber() {
        return buildNumber != null ? buildNumber : Integer.valueOf(0);
    }

    public String getQualifier() {
        return qualifier;
    }

    public final void parseVersion(String version) {
        comparable = new ComparableVersion(version);

        int index = version.indexOf("-");

        String part1;
        String part2 = null;

        if (index < 0) {
            part1 = version;
        } else {
            part1 = version.substring(0, index);
            part2 = version.substring(index + 1);
        }

        if (part2 != null) {
            try {
                if ((part2.length() == 1) || !part2.startsWith("0")) {
                    buildNumber = Integer.valueOf(part2);
                } else {
                    qualifier = part2;
                }
            } catch (NumberFormatException e) {
                qualifier = part2;
            }
        }

        if ((!part1.contains(".")) && !part1.startsWith("0")) {
            try {
                majorVersion = Integer.valueOf(part1);
            } catch (NumberFormatException e) {
                // qualifier is the whole version, including "-"
                qualifier = version;
                buildNumber = null;
            }
        } else {
            boolean fallback = false;

            StringTokenizer tok = new StringTokenizer(part1, ".");
            try {
                majorVersion = getNextIntegerToken(tok);
                if (tok.hasMoreTokens()) {
                    minorVersion = getNextIntegerToken(tok);
                }
                if (tok.hasMoreTokens()) {
                    incrementalVersion = getNextIntegerToken(tok);
                }
                if (tok.hasMoreTokens()) {
                    qualifier = tok.nextToken();
                    fallback = Pattern.compile("\\d+").matcher(qualifier).matches();
                }

                // string tokenzier won't detect these and ignores them
                if (part1.contains("..") || part1.startsWith(".") || part1.endsWith(".")) {
                    fallback = true;
                }
            } catch (NumberFormatException e) {
                fallback = true;
            }

            if (fallback) {
                // qualifier is the whole version, including "-"
                qualifier = version;
                majorVersion = null;
                minorVersion = null;
                incrementalVersion = null;
                buildNumber = null;
            }
        }
    }

    private static Integer getNextIntegerToken(StringTokenizer tok) {
        String s = tok.nextToken();
        if ((s.length() > 1) && s.startsWith("0")) {
            throw new NumberFormatException("Number part has a leading 0: '" + s + "'");
        }
        return Integer.valueOf(s);
    }

    @Override
    public String toString() {
        if (qualifier == null) {
            return getMajorVersion() + "." + getMinorVersion() + "." + getIncrementalVersion() + "-" + getBuildNumber();
        }
        return getMajorVersion() + "." + getMinorVersion() + "." + getIncrementalVersion() + "-" + getBuildNumber() + "-" + getQualifier();
    }
}