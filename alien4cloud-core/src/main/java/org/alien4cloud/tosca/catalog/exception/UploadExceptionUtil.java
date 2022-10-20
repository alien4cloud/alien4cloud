package org.alien4cloud.tosca.catalog.exception;

import alien4cloud.component.repository.exception.CSARUsedInActiveDeployment;
import alien4cloud.component.repository.exception.ToscaTypeAlreadyDefinedInOtherCSAR;
import alien4cloud.exception.AlreadyExistException;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.impl.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileUploadBase.SizeLimitExceededException;

/**
 * Utility class that generates parsing errors from exceptions.
 */
@Slf4j
public final class UploadExceptionUtil {

    public static ParsingError parsingErrorFromException(Exception e) {
        log.debug("Archive import failed", e);
        if (e instanceof AlreadyExistException) {
            return new ParsingError(ErrorCode.CSAR_ALREADY_EXISTS, "CSAR already exists", null,
                    "Unable to override an existing CSAR if the version is not a SNAPSHOT version.", null, null);
        } else if (e instanceof CSARUsedInActiveDeployment) {
            return new ParsingError(ErrorCode.CSAR_USED_IN_ACTIVE_DEPLOYMENT, "CSAR used in active deployment", null,
                    "Unable to override a csar used in an active deployment.", null, null);
        } else if (e instanceof ToscaTypeAlreadyDefinedInOtherCSAR) {
            return new ParsingError(ErrorCode.TOSCA_TYPE_ALREADY_EXISTS_IN_OTHER_CSAR, "Tosca type conflict", null, e.getMessage(), null, null);
        } else if (e instanceof SizeLimitExceededException) {
            return new ParsingError(ErrorCode.FILE_SIZE_EXCEEDED, "Uploaded file size exceeded", null, e.getMessage(), null, null);
        }
        log.error("Unexpected error while parsing archive.", e);
        return new ParsingError(ErrorCode.ERRONEOUS_ARCHIVE_FILE, "Failed to process archive for unexpected reason", null, e.getMessage(), null, null);
    }
}
