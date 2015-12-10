/**
 * Adds a ConferenceId to conferenceIdsToAttend.
 *
 * The method initConferenceIdsToAttend is not thread-safe, but we need a transaction for
 * calling this method after all, so it is not a practical issue.
 *
 * @param conferenceKey a websafe String representation of the Conference Key.
 */
public void addToConferenceKeysToAttend(String conferenceKey) {
    conferenceKeysToAttend.add(conferenceKey);
}

/**
 * Remove the conferenceId from conferenceIdsToAttend.
 *
 * @param conferenceKey a websafe String representation of the Conference Key.
 */
public void unregisterFromConference(String conferenceKey) {
    if (conferenceKeysToAttend.contains(conferenceKey)) {
        conferenceKeysToAttend.remove(conferenceKey);
    } else {
        throw new IllegalArgumentException("Invalid conferenceKey: " + conferenceKey);
    }
}