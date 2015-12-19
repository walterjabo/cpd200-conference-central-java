package com.google.training.cpd200.conference.domain;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;

import java.util.Date;

/**
 * Alert class stores alert date and content.
 */
@Entity
public class Alert {

    /**
     * The id for the datastore key.
     *
     * We use automatic id assignment for entities of Alert class.
     */
    @Id
    private Long id;

    /**
     * The content of the alert.
     */
    private String content;

    /**
     * Alert's date.
     */
    private Date date;
  
    /**
     * Holds Alert key as the parent.
     */
    @Parent
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    private Key<Alert> alertKey;

    /**
     * Set the default constructor to private.
     */
    private Alert() {}

    /**
     * Public constructor for Alert.
     * @param content The content of the alert.
     * @param date The date of the alert.
     */
    public Alert(String userId, String content, Date date) {
        this.content = content;
        this.date = date;
    }

    /**
     * Getter for content.
     * @return content.
     */
    public String getContent() {
        return content;
    }

    /**
     * Getter for date.
     * @return date.
     */
    public Date getDate() {
        return date;
    }
}
