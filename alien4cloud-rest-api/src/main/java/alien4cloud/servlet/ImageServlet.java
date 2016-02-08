package alien4cloud.servlet;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.WebApplicationContextUtils;

import alien4cloud.images.IImageDAO;
import alien4cloud.images.ImageData;
import alien4cloud.utils.ImageQuality;

/**
 * A servlet that query an image by id and return's it.
 * 
 * @author luc boutier
 */
@Component
public class ImageServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private IImageDAO imageDAO;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(config.getServletContext());
        this.imageDAO = context.getBean(IImageDAO.class);
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Cache-Control", "public");
        resp.setHeader("Pragma", null);
        resp.setHeader("Expires", null);

        final String imageId = req.getParameter("id");
        final String quality = req.getParameter("quality");
        final ImageQuality imageQuality;
        if (quality == null) {
            imageQuality = ImageQuality.QUALITY_BEST;
        } else {
            imageQuality = ImageQuality.valueOf(quality);
        }

        final ImageData imageData = this.imageDAO.readImage(imageId, imageQuality);
        if (imageData != null) {
            // Set content type
            resp.setContentType(imageData.getMime());
            // Set content size
            resp.setContentLength(imageData.getData().length);

            // Open the file and output streams
            final OutputStream out = resp.getOutputStream();
            try {
                out.write(imageData.getData());
            } finally {
                out.close();
            }
        } else {
            resp.setStatus(HttpStatus.NOT_FOUND.value());
        }
    }
}