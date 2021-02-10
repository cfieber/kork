package com.netflix.spinnaker.security;

/**
 * Marker interface indicating these user attributes include the specific attributes that were
 * included in the {{@link User}} object and that extensions may have an expectation of being
 * present.
 */
public interface LegacyUserAttributes extends UserAttributes {
  String getEmail();

  String getFirstName();

  String getLastName();
}
