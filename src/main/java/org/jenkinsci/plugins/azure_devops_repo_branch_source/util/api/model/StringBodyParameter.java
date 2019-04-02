package org.jenkinsci.plugins.azure_devops_repo_branch_source.util.api.model;

import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.AnnotationHandler;
import org.kohsuke.stapler.InjectedParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.lang.annotation.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link InjectedParameter} annotation to use on
 * {@link org.kohsuke.stapler.WebMethod} parameters to extract the body of the request
 * as a single string.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Documented
@InjectedParameter(StringBodyParameter.StringBodyHandler.class)
public @interface StringBodyParameter {

    /**
     * Webmethod parameter annotation that extracts the body of the request.
     */
    class StringBodyHandler extends AnnotationHandler<StringBodyParameter> {

        private static final Logger LOGGER = Logger.getLogger(StringBodyHandler.class.getName());


        @Override
        public Object parse(final StaplerRequest request, final StringBodyParameter a, final Class type, final String parameterName) throws ServletException {

            final String rawContentType = request.getContentType();
            final String contentType = StringHelper.determineContentTypeWithoutCharset(rawContentType);

            if (MediaType.APPLICATION_FORM_URLENCODED.equals(contentType)) {
                return request.getParameter(parameterName);
            }

            // default to application/json
            final String characterEncoding = request.getCharacterEncoding();
            try {
                return IOUtils.toString(request.getInputStream(), characterEncoding);
            } catch (final IOException e) {
                LOGGER.log(Level.SEVERE, "Unable to obtain request body: {}", e.getMessage());
            }
            return null;
        }

    }
}
