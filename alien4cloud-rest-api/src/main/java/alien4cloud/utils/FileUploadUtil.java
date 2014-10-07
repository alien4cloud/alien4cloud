package alien4cloud.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.springframework.web.multipart.MultipartFile;

public final class FileUploadUtil {
    private FileUploadUtil() {
    }

    /**
     * Some implementations of Transfer to may fails silently (jetty) under some OS.
     * This methods ensure that the transfer worked well and workaround it if not.
     * 
     * @throws IOException In case we fail to copy the file (in case the transfer to failed).
     */
    public static void safeTransferTo(Path targetPath, MultipartFile multipartFile) throws IOException {
        File targetFile = targetPath.toFile();

        multipartFile.transferTo(targetFile);

        if (targetFile.exists()) {
            if (targetFile.length() == 0) {
                copyMultiPart(targetPath, multipartFile);
            }
        } else {
            copyMultiPart(targetPath, multipartFile);
        }
    }

    private static void copyMultiPart(Path targetPath, MultipartFile multipartFile) throws IOException {
        InputStream in = null;
        BufferedOutputStream out = null;
        try {
            in = multipartFile.getInputStream();
            out = new BufferedOutputStream(new FileOutputStream(targetPath.toFile()));
            IOUtils.copy(in, out);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

    }
}
