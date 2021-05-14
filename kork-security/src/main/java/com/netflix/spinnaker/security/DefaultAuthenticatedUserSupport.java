package com.netflix.spinnaker.security;

import java.security.Principal;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.userdetails.UserDetails;

public class DefaultAuthenticatedUserSupport implements AuthenticatedUserSupport {
  @Override
  @Nonnull
  public Optional<String> getUsernameFromAuthentication(@Nullable Authentication authentication) {
    return Optional.ofNullable(authentication)
        .map(Authentication::getPrincipal)
        .map(
            p -> {
              if (p instanceof UserDetails) {
                return ((UserDetails) p).getUsername();
              }
              if (p instanceof Principal) {
                return ((Principal) p).getName();
              }
              if (p instanceof String) {
                return ((String) p);
              }
              return authentication.getName();
            });
  }

  @Override
  @Nonnull
  public Optional<Collection<String>> getAllowedAccountsFromAuthentication(
      @Nullable Authentication authentication) {
    return Optional.ofNullable(authentication)
        .flatMap(
            a -> {
              if (a.getPrincipal() instanceof HasAllowedAccounts) {
                return Optional.ofNullable(
                    ((HasAllowedAccounts) a.getPrincipal()).getAllowedAccounts());
              } else if (a.getDetails() instanceof HasAllowedAccounts) {
                return Optional.ofNullable(
                    ((HasAllowedAccounts) a.getDetails()).getAllowedAccounts());
              } else {
                return Optional.ofNullable(
                    AllowedAccountsAuthorities.getAllowedAccountsFromAuthorities(
                        a.getAuthorities()));
              }
            });
  }

  @Override
  @Nonnull
  public Optional<Object> getUserAttributesFromAuthentication(
      @Nullable Authentication authentication) {
    Function<Object, Optional<Object>> buildUserAttributes =
        (userAttributes) ->
            Optional.ofNullable(((UserAttributes) userAttributes).getPublicAttributes());

    return Optional.ofNullable(authentication)
        .flatMap(
            a -> {
              if (a instanceof UserAttributes) {
                return buildUserAttributes.apply(a);
              } else if (a.getDetails() instanceof UserAttributes) {
                return buildUserAttributes.apply(a.getDetails());
              } else if (a.getPrincipal() instanceof UserAttributes) {
                return buildUserAttributes.apply(a.getPrincipal());
              } else if (a.getPrincipal() instanceof UserDetails
                  && a.getPrincipal() instanceof CredentialsContainer) {
                ((CredentialsContainer) a.getPrincipal()).eraseCredentials();
                ;
                return Optional.of(a.getPrincipal());
              }

              return Optional.empty();
            });
  }
}
