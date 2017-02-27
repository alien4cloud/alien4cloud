package alien4cloud.rest.csar;

import org.alien4cloud.tosca.model.Csar;
import org.apache.commons.collections4.CollectionUtils;

import alien4cloud.tosca.parser.ParsingResult;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CsarUploadUtil {

    public static CsarUploadResult toUploadResult(ParsingResult<Csar> result) {
        CsarUploadResult uploadResult = new CsarUploadResult();
        uploadResult.setCsar(result.getResult());
        if (CollectionUtils.isNotEmpty(result.getContext().getParsingErrors())) {
            uploadResult.getErrors().put(result.getContext().getFileName(), result.getContext().getParsingErrors());
        }
        return uploadResult;
    }
}
