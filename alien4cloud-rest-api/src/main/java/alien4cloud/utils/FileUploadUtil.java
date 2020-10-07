package alien4cloud.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class FileUploadUtil {
    private FileUploadUtil() {
    }

    /**
     * Implementation of transferTo of the multipart file is not reliable as relative to configured folder (by default system tmp folder).
     * This method uses the input stream to copy the file to the right location.
     *
     * @throws IOException In case we fail to copy the file (in case the transfer to failed).
     */
    public static void safeTransferTo(Path targetPath, MultipartFile multipartFile) throws IOException {
        if (multipartFile.isEmpty()) {
            log.debug("Uploaded file is empty.");
            return;
        }
        try (InputStream fileStream = multipartFile.getInputStream()) {
            Files.copy(fileStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static void safeTransferTo (Path targetPath, InputStream stream) throws IOException {
       Files.copy(stream, targetPath, StandardCopyOption.REPLACE_EXISTING);
    }
}
