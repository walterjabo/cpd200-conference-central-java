package com.google.training.cpd200.conference.spi;

import static com.google.training.cpd200.conference.service.OfyService.factory;
import static com.google.training.cpd200.conference.service.OfyService.ofy;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.response.ConflictException;
import com.google.api.server.spi.response.ForbiddenException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.users.User;
import com.google.training.cpd200.conference.Constants;
import com.google.training.cpd200.conference.domain.Alert;
import com.google.training.cpd200.conference.domain.Conference;
import com.google.training.cpd200.conference.domain.Profile;
import com.google.training.cpd200.conference.form.ConferenceForm;
import com.google.training.cpd200.conference.form.ConferenceQueryForm;
import com.google.training.cpd200.conference.form.ProfileForm;
import com.google.training.cpd200.conference.form.ProfileForm.TeeShirtSize;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.cmd.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Named;

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

    private static final Logger LOG = Logger.getLogger(
           ConferenceApi.class.getName());
  
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

    /**
    * Gets the Profile entity for the current user
    * or creates it if it doesn't exist
    * @param user
    * @return user's Profile
    */
    private static Profile getProfileFromUser(User user) {
        // First fetch the user's Profile from the datastore.
        Profile profile = ofy().load().key(
                Key.create(Profile.class, user.getUserId())).now();
        if (profile == null) {
            // Create a new Profile if it doesn't exist.
            // Use default displayName and teeShirtSize
            String email = user.getEmail();
            profile = new Profile(user.getUserId(),
                    extractDefaultDisplayNameFromEmail(email), email, TeeShirtSize.NOT_SPECIFIED);
       }
       return profile;
   }

    /**
     * Creates a new Conference object and stores it to the datastore.
     *
     * @param user A user who invokes this method, null when the user is not signed in.
     * @param conferenceForm A ConferenceForm object representing user's inputs.
     * @return A newly created Conference Object.
     * @throws UnauthorizedException when the user is not signed in.
     */
    @ApiMethod(name = "createConference", path = "conference", httpMethod = HttpMethod.POST)
    public Conference createConference(final User user, final ConferenceForm conferenceForm)
        throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }

        // Get the userId of the logged in User
        String userId = user.getUserId();

        // Get the key for the User's Profile
        Key<Profile> profileKey = Key.create(Profile.class, userId);

        // Allocate a key for the conference -- let App Engine allocate the ID
        // Don't forget to include the parent Profile in the allocated ID
        final Key<Conference> conferenceKey = factory().allocateId(profileKey, Conference.class);

        // Get the Conference Id from the Key
        final long conferenceId = conferenceKey.getId();

        // Get the existing Profile entity for the current user if there is one
        // Otherwise create a new Profile entity with default values
        Profile profile = getProfileFromUser(user);

        // Create a new Conference Entity, specifying the user's Profile entity
        // as the parent of the conference
        Conference conference = new Conference(conferenceId, userId, conferenceForm);

        // Save Conference and Profile Entities
        ofy().save().entities(conference, profile).now();
    
        return conference;
    }

    /**
     * Queries against the datastore with the given filters and returns the result.
     *
     * Normally this kind of method is supposed to get invoked by a GET HTTP method,
     * but we do it with POST, in order to receive conferenceQueryForm Object via the POST body.
     *
     * @param conferenceQueryForm A form object representing the query.
     * @return A List of Conferences that match the query.
     */
    @ApiMethod(
            name = "queryConferences",
            path = "queryConferences",
            httpMethod = HttpMethod.POST
    )
    public List<Conference> queryConferences(ConferenceQueryForm conferenceQueryForm) {
        Iterable<Conference> conferenceIterable = conferenceQueryForm.getQuery();
        List<Conference> result = new ArrayList<>(0);
        List<Key<Profile>> organizersKeyList = new ArrayList<>(0);
        for (Conference conference : conferenceIterable) {
            organizersKeyList.add(Key.create(Profile.class, conference.getOrganizerUserId()));
            result.add(conference);
        }
        // To avoid separate datastore gets for each Conference, pre-fetch the Profiles.
        ofy().load().keys(organizersKeyList);
        return result;
    }
    
    /**
     * Returns a list of Conferences that the user created.
     * In order to receive the websafeConferenceKey via the JSON params, uses a POST method.
     *
     * @param user A user who invokes this method, null when the user is not signed in.
     * @return a list of Conferences that the user created.
     * @throws UnauthorizedException when the user is not signed in.
     */
    @ApiMethod(
            name = "getConferencesCreated",
            path = "getConferencesCreated",
            httpMethod = HttpMethod.POST
    )
    public List<Conference> getConferencesCreated(final User user) throws UnauthorizedException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        String userId = user.getUserId();
        Key<Profile> userKey = Key.create(Profile.class, userId);
        return ofy().load().type(Conference.class)
                .ancestor(userKey)
                .list();
    }

    /**
     * Just a wrapper for Boolean.
     */
    public static class WrappedBoolean {
    
        private final Boolean result;
    
        public WrappedBoolean(Boolean result) {
            this.result = result;
        }
    
        public Boolean getResult() {
            return result;
        }
    }

    /**
     * A wrapper class that can embrace a generic result or some kind of exception.
     *
     * Use this wrapper class for the return type of objectify transaction.
     * <pre>
     * {@code
     * // The transaction that returns Conference object.
     * TxResult<Conference> result = ofy().transact(new Work<TxResult<Conference>>() {
     *     public TxResult<Conference> run() {
     *         // Code here.
     *         // To throw 404
     *         return new TxResult<>(new NotFoundException("No such conference"));
     *         // To return a conference.
     *         Conference conference = somehow.getConference();
     *         return new TxResult<>(conference);
     *     }
     * }
     * // Actually the NotFoundException will be thrown here.
     * return result.getResult();
     * </pre>
     *
     * @param <ResultType> The type of the actual return object.
     */
    private static class TxResult<ResultType> {

        private ResultType result;

        private Throwable exception;

        private TxResult(ResultType result) {
            this.result = result;
        }

        private TxResult(Throwable exception) {
            if (exception instanceof NotFoundException ||
                    exception instanceof ForbiddenException ||
                    exception instanceof ConflictException) {
                this.exception = exception;
            } else {
                throw new IllegalArgumentException("Exception not supported.");
            }
        }

        private ResultType getResult() throws NotFoundException, ForbiddenException, ConflictException {
            if (exception instanceof NotFoundException) {
                throw (NotFoundException) exception;
            }
            if (exception instanceof ForbiddenException) {
                throw (ForbiddenException) exception;
            }
            if (exception instanceof ConflictException) {
                throw (ConflictException) exception;
            }
            return result;
        }
    }

    /**
     * Returns a Conference object with the given conferenceId.
     *
     * @param websafeConferenceKey The String representation of the Conference Key.
     * @return a Conference object with the given conferenceId.
     * @throws NotFoundException when there is no Conference with the given conferenceId.
     */
    @ApiMethod(
            name = "getConference",
            path = "conference/{websafeConferenceKey}",
            httpMethod = HttpMethod.GET
    )
    public Conference getConference(
            @Named("websafeConferenceKey") final String websafeConferenceKey)
            throws NotFoundException {
        Key<Conference> conferenceKey = Key.create(websafeConferenceKey);
        Conference conference = ofy().load().key(conferenceKey).now();
        if (conference == null) {
            throw new NotFoundException("No Conference found with key: " + websafeConferenceKey);
        }
        return conference;
    }
  
    /**
     * Registers to the specified Conference.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @param websafeConferenceKey The String representation of the Conference Key.
     * @return Boolean true when success, otherwise false
     * @throws UnauthorizedException when the user is not signed in.
     * @throws NotFoundException when there is no Conference with the given conferenceId.
     */
    @ApiMethod(
            name = "registerForConference",
            path = "conference/{websafeConferenceKey}/registration",
            httpMethod = HttpMethod.POST
    )
    public WrappedBoolean registerForConference(final User user,
                                         @Named("websafeConferenceKey")
                                         final String websafeConferenceKey)
        throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        // final String userId = getUserId(user);
        TxResult<Boolean> result = ofy().transact(new Work<TxResult<Boolean>>() {
            @Override
            public TxResult<Boolean> run() {
                Key<Conference> conferenceKey = Key.create(websafeConferenceKey);
                Conference conference = ofy().load().key(conferenceKey).now();
                // 404 when there is no Conference with the given conferenceId.
                if (conference == null) {
                    return new TxResult<>(new NotFoundException(
                            "No Conference found with key: " + websafeConferenceKey));
                }
                // Registration happens here.
                Profile profile = getProfileFromUser(user);
                if (profile.getConferenceKeysToAttend().contains(websafeConferenceKey)) {
                    return new TxResult<>(new ConflictException("You have already registered for this conference"));
                } else if (conference.getSeatsAvailable() <= 0) {
                    return new TxResult<>(new ConflictException("There are no seats available."));
                } else {
                    profile.addToConferenceKeysToAttend(websafeConferenceKey);
                    conference.bookSeats(1);
                    ofy().save().entities(profile, conference).now();
                    return new TxResult<>(true);
                }
            }
        });
        // NotFoundException is actually thrown here.
        return new WrappedBoolean(result.getResult());
    }

    /**
     * Unregister from the specified Conference.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @param websafeConferenceKey The String representation of the Conference Key to unregister
     *                             from.
     * @return Boolean true when success, otherwise false.
     * @throws UnauthorizedException when the user is not signed in.
     * @throws NotFoundException when there is no Conference with the given conferenceId.
     */
    @ApiMethod(
            name = "unregisterFromConference",
            path = "conference/{websafeConferenceKey}/registration",
            httpMethod = HttpMethod.DELETE
    )
    public WrappedBoolean unregisterFromConference(final User user,
                                            @Named("websafeConferenceKey")
                                            final String websafeConferenceKey)
            throws UnauthorizedException, NotFoundException, ForbiddenException, ConflictException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        TxResult<Boolean> result = ofy().transact(new Work<TxResult<Boolean>>() {
            @Override
            public TxResult<Boolean> run() {
                Key<Conference> conferenceKey = Key.create(websafeConferenceKey);
                Conference conference = ofy().load().key(conferenceKey).now();
                // 404 when there is no Conference with the given conferenceId.
                if (conference == null) {
                    return new TxResult<>(new NotFoundException(
                            "No Conference found with key: " + websafeConferenceKey));
                }
                // Un-registering from the Conference.
                Profile profile = getProfileFromUser(user);
                if (profile.getConferenceKeysToAttend().contains(websafeConferenceKey)) {
                    profile.unregisterFromConference(websafeConferenceKey);
                    conference.giveBackSeats(1);
                    ofy().save().entities(profile, conference).now();
                    return new TxResult<>(true);
                } else {
                    return new TxResult<>(false);
                }
            }
        });
        // NotFoundException is actually thrown here.
        return new WrappedBoolean(result.getResult());
    }

    /**
     * Returns the latest Alert object.
     *
     * @return the latest Alert object.
     */
    @ApiMethod(
            name = "getAlert",
            path = "alert",
            httpMethod = HttpMethod.GET
    )
    public Alert getAlert() {
        List<Alert> alertList = ofy().load().type(Alert.class).order("-date").list();
        if (alertList != null) {
            return alertList.get(0);
        }
        return null;
    }

    /**
     * Returns a collection of Conference Object that the user is going to attend.
     *
     * @param user An user who invokes this method, null when the user is not signed in.
     * @return a Collection of Conferences that the user is going to attend.
     * @throws UnauthorizedException when the User object is null.
     */
    @ApiMethod(
            name = "getConferencesToAttend",
            path = "getConferencesToAttend",
            httpMethod = HttpMethod.GET
    )
    public Collection<Conference> getConferencesToAttend(final User user)
            throws UnauthorizedException, NotFoundException {
        // If not signed in, throw a 401 error.
        if (user == null) {
            throw new UnauthorizedException("Authorization required");
        }
        Profile profile = ofy().load().key(Key.create(Profile.class, user.getUserId())).now();
        if (profile == null) {
            throw new NotFoundException("Profile doesn't exist.");
        }
        List<String> keyStringsToAttend = profile.getConferenceKeysToAttend();
        List<Key<Conference>> keysToAttend = new ArrayList<>();
        for (String keyString : keyStringsToAttend) {
            keysToAttend.add(Key.<Conference>create(keyString));
        }
        return ofy().load().keys(keysToAttend).values();
    }

    @ApiMethod(
            name = "filterPlayground",
            path = "filterPlayground",
            httpMethod = HttpMethod.POST
    )
    public List<Conference> filterPlayground() {
        Query<Conference> query = ofy().load().type(Conference.class);

        // Filter on city
        // query = query.filter("city =", "Paris");

        /*
        TODO
        add 2 filters:
        1: city equals to Chicago
        2: topic equals "Medical Innovations"
        */
      
        // Filter on city = "Chicago"
        // query = query.filter("city =", "Chicago");
        // Add a filter for topic = "Medical Innovations"
        // query = query.filter("topics =", "Medical Innovations");
        
        return query.list();
    }
}
