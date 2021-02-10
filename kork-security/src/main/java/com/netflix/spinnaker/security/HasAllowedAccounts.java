package com.netflix.spinnaker.security;

import java.util.Collection;

/**
 * Marker interface indicating the implementing object can supply allowed accounts for a user.
 *
 * <p>Allows a custom user principal object, Authentication token, or Authentication token details
 * object to indicate that it supplies a list of allowed accounts for the user.
 */
public interface HasAllowedAccounts {
  Collection<String> getAllowedAccounts();
}
