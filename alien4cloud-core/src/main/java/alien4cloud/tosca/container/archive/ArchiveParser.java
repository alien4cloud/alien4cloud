package alien4cloud.tosca.container.archive;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;

import org.springframework.stereotype.Component;

import alien4cloud.tosca.container.exception.CSARIOException;
import alien4cloud.tosca.container.exception.CSARParsingException;
import alien4cloud.tosca.container.model.CSARMeta;
import alien4cloud.tosca.container.model.CloudServiceArchive;
import alien4cloud.tosca.container.model.Definitions;
import alien4cloud.tosca.container.validation.CSARErrorCode;
import alien4cloud.utils.YamlParserUtil;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.YAMLException;

/**
 * Component responsible for parsing alien archives and definitions.
 */
@Component
public class ArchiveParser {

    public static final String META_FILE_LOCATION = FileSystems.getDefault().getSeparator() + "TOSCA-Metadata" + FileSystems.getDefault().getSeparator()
            + "ALIEN-META.yaml";

    /**
     * Parse archive to construct the skeleton
     *
     * @param archiveFile path to the archive to parse
     * @return a {@link CloudServiceArchive}
     * @throws CSARParsingException when the content of the archive is invalid,
     *             for example bad yaml file format
     */
    public CloudServiceArchive parseArchive(Path archiveFile) throws CSARParsingException {
        CloudServiceArchive cloudServiceArchive = new CloudServiceArchive();
        FileSystem csarFS;
        try {
            csarFS = FileSystems.newFileSystem(archiveFile, null);
        } catch (IOException e) {
            throw new CSARIOException("Problem happened while accessing file [" + archiveFile + "]", e);
        } catch (ProviderNotFoundException e) {
            throw new CSARParsingException("Archive", CSARErrorCode.ERRONEOUS_ARCHIVE_FILE, "File is not in good format, only zip file is supported ", e);
        }
        // load the Cloud Service Archive Meta data.
        CSARMeta csarMeta = parseCSARMeta(csarFS.getPath(META_FILE_LOCATION));
        cloudServiceArchive.setMeta(csarMeta);

        // now load and validate the archive definitions
        for (String definition : csarMeta.getDefinitions()) {
            Definitions definitions = parseDefinitions(csarFS.getPath(definition));
            cloudServiceArchive.putDefinitions(definition, definitions);
        }
        return cloudServiceArchive;
    }

    /**
     * Parse a cloud service archive meta yaml file.
     *
     * @param metaPath The path to the meta-file.
     * @return A {@link CSARMeta} object matching the file content.
     * @throws CSARParsingException if the csar meta data file is in bad format
     */
    public CSARMeta parseCSARMeta(Path metaPath) throws CSARParsingException {
        try {
            return YamlParserUtil.parseFromUTF8File(metaPath, CSARMeta.class);
        } catch (YAMLException e) {
            throw new CSARParsingException(metaPath.toString(), CSARErrorCode.ERRONEOUS_METADATA_FILE, "CSAR Meta data yaml file's content is invalid ["
                    + metaPath + "]", e);
        } catch (UnrecognizedPropertyException e) {
            int lineNr = e.getLocation().getLineNr();
            int colNr = e.getLocation().getColumnNr();
            throw new CSARParsingException(metaPath.toString(), e.getPropertyName(), lineNr, colNr, CSARErrorCode.UNRECOGNIZED_PROP_ERROR_METADATA_FILE,
                    "Yaml is not valid [" + metaPath + "] property [" + e.getPropertyName() + "] at line [" + lineNr + "] column [" + colNr
                            + "] is not a valid property", e);
        } catch (JsonMappingException e) {
            int lineNr = e.getLocation().getLineNr();
            int colNr = e.getLocation().getColumnNr();
            throw new CSARParsingException(metaPath.toString(), lineNr, colNr, CSARErrorCode.MAPPING_ERROR_METADATA_FILE, "Yaml is not valid [" + metaPath
                    + "] at line [" + lineNr + "] column [" + colNr + "]", e);
        } catch (JsonParseException e) {
            int lineNr = e.getLocation().getLineNr();
            int colNr = e.getLocation().getColumnNr();
            throw new CSARParsingException(metaPath.toString(), lineNr, colNr, CSARErrorCode.ERRONEOUS_METADATA_FILE, "Yaml file's content is invalid ["
                    + metaPath + "] at line [" + lineNr + "] column [" + colNr + "]", e);
        } catch (NoSuchFileException e) {
            throw new CSARParsingException(metaPath.toString(), CSARErrorCode.MISSING_METADATA_FILE, "Yaml file [" + metaPath + "] not found in the archive", e);
        } catch (IOException e) {
            throw new CSARIOException("Unable to read CSAR Meta data yaml file [" + metaPath + "]", e);
        }
    }

    /**
     * Parse a single definition from a file path.
     *
     * @param definitionPath The path of the definition file.
     * @return parsed {@link Definitions}
     * @throws CSARParsingException if the csar meta data file is in bad format
     */
    public Definitions parseDefinitions(Path definitionPath) throws CSARParsingException {
        try {
            return YamlParserUtil.parseFromUTF8File(definitionPath, Definitions.class);
        } catch (YAMLException e) {
            throw new CSARParsingException(definitionPath.toString(), CSARErrorCode.ERRONEOUS_DEFINITION_FILE,
                    "Unexpected error while parsing yaml definition file [" + definitionPath + "]", e);
        } catch (UnrecognizedPropertyException e) {
            int lineNr = e.getLocation().getLineNr();
            int colNr = e.getLocation().getColumnNr();
            throw new CSARParsingException(definitionPath.toString(), e.getPropertyName(), lineNr, colNr,
                    CSARErrorCode.UNRECOGNIZED_PROP_ERROR_DEFINITION_FILE, "Yaml is not valid TOSCA [" + definitionPath + "] property [" + e.getPropertyName()
                            + "] at line [" + lineNr + "] column [" + colNr + "] is not a valid property", e);
        } catch (JsonMappingException e) {
            int lineNr = e.getLocation().getLineNr();
            int colNr = e.getLocation().getColumnNr();
            throw new CSARParsingException(definitionPath.toString(), lineNr, colNr, CSARErrorCode.MAPPING_ERROR_DEFINITION_FILE, "Yaml is not valid TOSCA ["
                    + definitionPath + "] at line [" + lineNr + "] column [" + colNr + "]", e);
        } catch (JsonParseException e) {
            int lineNr = e.getLocation().getLineNr();
            int colNr = e.getLocation().getColumnNr();
            throw new CSARParsingException(definitionPath.toString(), lineNr, colNr, CSARErrorCode.ERRONEOUS_DEFINITION_FILE,
                    "Yaml file's content is invalid [" + definitionPath + "] at line [" + lineNr + "] column [" + colNr + "]", e);
        } catch (NoSuchFileException e) {
            throw new CSARParsingException(definitionPath.toString(), CSARErrorCode.MISSING_DEFINITION_FILE, "Yaml file [" + definitionPath
                    + "] not found in the archive", e);
        } catch (IOException e) {
            throw new CSARIOException("Unable to read yaml file [" + definitionPath + "]", e);
        }
    }
}
