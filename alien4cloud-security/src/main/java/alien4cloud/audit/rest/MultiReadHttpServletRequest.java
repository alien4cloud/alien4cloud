package alien4cloud.audit.rest;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.io.IOUtils;

public class MultiReadHttpServletRequest extends HttpServletRequestWrapper {

    private byte[] cachedBytes;

    public MultiReadHttpServletRequest(HttpServletRequest request) {
        super(request);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (cachedBytes == null) {
            cacheInputStream();
        }
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

        public CachedServletInputStream() {
            /* create a new input stream from the cached request body */
            input = new ByteArrayInputStream(cachedBytes);
        }

        @Override
        public int read() throws IOException {
            return input.read();
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
            throw new UnsupportedOperationException("Unsupported in the current implementation");
        }
    }
}
