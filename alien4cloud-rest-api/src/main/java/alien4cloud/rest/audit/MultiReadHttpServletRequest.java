package alien4cloud.rest.audit;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.Lists;

public class MultiReadHttpServletRequest extends HttpServletRequestWrapper {

    private byte[] cachedBytes;

    public MultiReadHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (cachedBytes == null)
            cacheInputStream();

        return new CachedServletInputStream();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    private void cacheInputStream() throws IOException {
        /*
         * Cache the inputstream in order to read it multiple times. For
         * convenience, I use apache.commons IOUtils
         */
        ByteArrayOutputStream capturedRequestStream = new ByteArrayOutputStream();
        IOUtils.copy(super.getInputStream(), capturedRequestStream);
        cachedBytes = capturedRequestStream.toByteArray();
    }

    /* An inputstream which reads the cached request body */
    public class CachedServletInputStream extends ServletInputStream {

        private ByteArrayInputStream input;

        private List<ReadListener> readListeners = Lists.newArrayList();

        public CachedServletInputStream() {
            /* create a new input stream from the cached request body */
            input = new ByteArrayInputStream(cachedBytes);
        }

        @Override
        public int read() throws IOException {
            int readData = input.read();
            if (isFinished()) {
                for (ReadListener listener : readListeners) {
                    // Notify that there's nothing more to read
                    listener.onAllDataRead();
                }
            }
            return readData;
        }

        @Override
        public boolean isFinished() {
            return input.available() <= 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            try {
                readListener.onDataAvailable();
            } catch (IOException e) {
                // Data is always available as it has already been cached
            }
            readListeners.add(readListener);
        }

    }

}
