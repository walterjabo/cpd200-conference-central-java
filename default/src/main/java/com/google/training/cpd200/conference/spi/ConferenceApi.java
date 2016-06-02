package com.google.training.cpd200.conference.spi;

import static com.google.training.cpd200.conference.service.OfyService.ofy;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.users.User;
import com.google.training.cpd200.conference.Constants;
import com.google.training.cpd200.conference.domain.Profile;
import com.google.training.cpd200.conference.form.ProfileForm;
import com.google.training.cpd200.conference.form.ProfileForm.TeeShirtSize;
import com.googlecode.objectify.Key;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Defines conference APIs.
 */
@Api(
    name = "conference", 
    version = "v1", 
    scopes = { Constants.EMAIL_SCOPE }, 
    clientIds = { Constants.WEB_CLIENT_ID, Constants.API_EXPLORER_CLIENT_ID }, 
    description = "API for the Conference Central Backend application."
    )
public class ConferenceApi {

    private static final Logger LOG = Logger.getLogger(ConferenceApi.class.getName());

    /*
     * Get the display name from the user's email. For example, if the email is
     * janedoe@example.com, then the display name becomes "janedoe"
     */
    private static String extractDefaultDisplayNameFromEmail(String email) {
        return email == null ? null : email.substring(0, email.indexOf("@"));
    }

    /**
     * Creates or updates a Profile object associated with the given user
     * object.
     *
     * @param user
     *            A User object injected by the cloud endpoints.
     * @param profileForm
     *            A ProfileForm object sent from the client form.
     * @return Profile object just created.
     * @throws UnauthorizedException
     *             when the User object is null.
     */

    // Declare this method as a method available externally through Endpoints
    @ApiMethod(name = "saveProfile", path = "profile", httpMethod = HttpMethod.POST)
    public Profile saveProfile(final User user, ProfileForm profileForm)
            throws UnauthorizedException {

        String userId = null;
        String mainEmail = null;
        String displayName = "Your name will go here";
        TeeShirtSize teeShirtSize = TeeShirtSize.NOT_SPECIFIED;

        // If the user is not logged in, throw an UnauthorizedException
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // Set the displayName to the value sent by the ProfileForm
        displayName = profileForm.getDisplayName();

        // Set the teeShirtSize to the value sent by the ProfileForm, if sent
        // otherwise leave it as the default value
        if (profileForm.getTeeShirtSize() != null) {
            teeShirtSize = profileForm.getTeeShirtSize();
        }
        
        // Get the userId and mainEmail
        mainEmail = user.getEmail();
        userId = user.getUserId();

        // If the displayName is null, set it to the default value based on the user's email
        // by calling extractDefaultDisplayNameFromEmail(...)
        if (displayName == null) {
            displayName = extractDefaultDisplayNameFromEmail(user.getEmail());
        }

        // Create a new Profile entity from the
        // userId, displayName, mainEmail and teeShirtSize
        // Profile profile = new Profile(userId, displayName, mainEmail, teeShirtSize);


        // Get the Profile from the datastore if it exists
        // otherwise create a new one
        Profile profile = ofy().load().key(Key.create(Profile.class, userId))
                .now();

        if (profile == null) {
            // Populate the displayName and teeShirtSize with default values
            // if not sent in the request
            if (displayName == null) {
                displayName = extractDefaultDisplayNameFromEmail(user
                        .getEmail());
            }
            if (teeShirtSize == null) {
                teeShirtSize = TeeShirtSize.NOT_SPECIFIED;
            }
            // Now create a new Proofile entity
            profile = new Profile(userId, displayName, mainEmail, teeShirtSize);
        } else {
            // The Profile entity already exists
            // Update the Profile entity
            profile.update(displayName, teeShirtSize);
        }

        // Save the entity in the datastore
        ofy().save().entity(profile).now();

        // Return the profile
        return profile;
    }

    /**
     * Returns a Profile object associated with the given user object. The cloud
     * endpoints system automatically inject the User object.
     *
     * @param user
     *            A User object injected by the cloud endpoints.
     * @return Profile object.
     * @throws UnauthorizedException
     *             when the User object is null.
     */
    @ApiMethod(name = "getProfile", path = "profile", httpMethod = HttpMethod.GET)
    public Profile getProfile(final User user) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // TODO
        // load the Profile Entity

        String userId = user.getUserId();
        Key key = Key.create(Profile.class, userId);
        Profile profile = (Profile) ofy().load().key(key).now();
        LOG.log(Level.INFO, "Did run getProfile()");
        return profile;
    }
}
