package alien4cloud.rest.csar;

import org.alien4cloud.tosca.model.Csar;

import alien4cloud.tosca.parser.ParsingResult;

public class CsarUploadUtil {

    public static CsarUploadResult toUploadResult(ParsingResult<Csar> result) {
        CsarUploadResult uploadResult = new CsarUploadResult();
        uploadResult.setCsar(result.getResult());
        if (result.getContext().getParsingErrors() != null && !result.getContext().getParsingErrors().isEmpty()) {
            uploadResult.getErrors().put(result.getContext().getFileName(), result.getContext().getParsingErrors());
        }
        return uploadResult;
    }
}
