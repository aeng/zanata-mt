package org.zanata.mt.api.dto;

import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Response entity for any API error
 *
 * @author Alex Eng<a href="mailto:aeng@redhat.com">aeng@redhat.com</a>
 */
public class APIErrorResponse implements Serializable {

    private final static DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ssZ");

    private static final long serialVersionUID = 6040356482529107259L;

    private int status;

    private String title;

    private String details;

    private String timestamp;

    public APIErrorResponse() {
    }

    public APIErrorResponse(Response.Status status, String title) {
        this(status, null, title);
    }

    public APIErrorResponse(Response.Status status, Exception e, String title) {
        this.status = status.getStatusCode();
        this.title = title;
        if (e != null) {
            this.details = e.getMessage();
        }
        this.timestamp = ZonedDateTime.now().format(DATE_FORMATTER);
    }

    /**
     * The HTTP status code.
     */
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Summary of the problem.
     */
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Detail explanation for this error.
     */
    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    /**
     * Timestamp of the response. Format: dd-MM-yyyy HH:mm:ssZ
     */
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
