package com.netflix.spinnaker.security;

import java.util.Collection;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Hook point for supporting alternative spring security based auth schemes.
 *
 * <p>This interface exposes the interactions Spinnaker expects to deal with against the logged in
 * user and allows an implementer to adapt those to an alternative Authentication token in the
 * SecurityContext.
 */
public interface AuthenticatedUserSupport {

  /**
   * Retrieves the current username from the Authentication token.
   *
   * @param authentication the authentication token, possibly null
   * @return An Optional of the current username or empty if there is no current user
   */
  @Nonnull
  Optional<String> getUsernameFromAuthentication(@Nullable Authentication authentication);

  /**
   * Retrieves the allowed accounts from the current Authentication token.
   *
   * @param authentication the authentication token, possibly null
   * @return An Optional of the allowed accounts for the user or empty if there are no allowed
   *     accounts associated with the user token
   */
  @Nonnull
  Optional<Collection<String>> getAllowedAccountsFromAuthentication(
      @Nullable Authentication authentication);

  /**
   * Retrieves a serializable representation of user attributes from the Authentication token.
   *
   * <p>An authentication token can contain additional attributes for a user (for example - first
   * name, last name, email address, etc). In Spinnaker those attributes are included as part of the
   * {{@code /auth/user}} payload and may be of interest in other extensions as well.
   *
   * @param authentication the authentication token, possibly null
   * @return An Optional of the user attributes for the user or empty if there are no attributes
   *     associated with the user token
   */
  @Nonnull
  Optional<Object> getUserAttributesFromAuthentication(@Nullable Authentication authentication);

  /**
   * Retrieves the current Authentication token for the user.
   *
   * <p>This mostly exists as a convenience to allow the below getXXXFromAuthenticationContext to be
   * defaulted.
   *
   * @return A Optional of the current Authentication token or empty if there is no current token.
   */
  @Nonnull
  default Optional<Authentication> getCurrentAuthentication() {
    return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
  }

  /**
   * Retrieves the username of the currently authenticated user.
   *
   * @return An Optional of the currently authenticated user's username or empty if there is no user
   */
  @Nonnull
  default Optional<String> getUsernameFromAuthenticationContext() {
    return getCurrentAuthentication().flatMap(this::getUsernameFromAuthentication);
  }

  /**
   * Retrieves the allowed accounts for the currently authenticated user.
   *
   * @return An Optional of the currently authenticated user's allowed accounts or empty if there
   *     are no allowed accounts
   */
  @Nonnull
  default Optional<Collection<String>> getAllowedAccountsFromAuthenticationContext() {
    return getCurrentAuthentication().flatMap(this::getAllowedAccountsFromAuthentication);
  }

  /**
   * Retrieves a serializable representation of user attributes for the currently authenticated
   * user.
   *
   * @see AuthenticatedUserSupport#getUserAttributesFromAuthentication(Authentication)
   * @return An Optional of the currently authenticated user's attributes or empty if there are no
   *     allowed accounts
   */
  @Nonnull
  default Optional<Object> getUserAttributesFromAuthenticationContext() {
    return getCurrentAuthentication().flatMap(this::getUserAttributesFromAuthentication);
  }
}
