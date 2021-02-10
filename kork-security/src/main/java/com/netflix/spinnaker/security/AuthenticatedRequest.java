/*
 * Copyright 2015 Netflix, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.netflix.spinnaker.security;

import static java.lang.String.format;

import com.google.common.base.Preconditions;
import com.netflix.spinnaker.kork.common.Header;
import com.netflix.spinnaker.kork.exceptions.SpinnakerException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.CollectionUtils;

public class AuthenticatedRequest {

  private static final Logger log = LoggerFactory.getLogger(AuthenticatedRequest.class);

  /**
   * Determines the current user principal and how to interpret that principal to extract user
   * identity and allowed accounts.
   */
  private static class PrincipalExtractor {
    private final AuthenticatedUserSupport authenticatedUserSupport;

    PrincipalExtractor(AuthenticatedUserSupport authenticatedUserSupport) {
      this.authenticatedUserSupport =
          Objects.requireNonNull(authenticatedUserSupport, "authenticationUserSupport");
    }

    /** @return The comma separated list of accounts for the current principal. */
    Optional<String> getSpinnakerAccounts() {
      var accounts = authenticatedUserSupport.getAllowedAccountsFromAuthenticationContext();
      if (accounts.isEmpty()) {
        return get(Header.ACCOUNTS);
      }
      return accounts.map(AuthenticatedRequest::normalizedAccountsFromCollection);
    }

    /**
     * @param principal the principal to inspect for accounts
     * @return the comma separated list of accounts for the provided principal.
     */
    Optional<Collection<String>> getSpinnakerAccounts(Object principal) {
      if (principal instanceof Authentication) {
        return authenticatedUserSupport
            .getAllowedAccountsFromAuthentication((Authentication) principal)
            .filter(c -> !c.isEmpty());
      } else if (principal instanceof UserDetails) {
        Collection<String> allowedAccounts =
            AllowedAccountsAuthorities.getAllowedAccounts((UserDetails) principal);
        if (!CollectionUtils.isEmpty(allowedAccounts)) {
          return Optional.of(allowedAccounts);
        }
      }

      return Optional.empty();
    }

    /** @return the user id of the current user */
    Optional<String> getSpinnakerUser() {
      var user = authenticatedUserSupport.getUsernameFromAuthenticationContext();
      if (user.isEmpty()) {
        return get(Header.USER);
      }

      return user;
    }

    /**
     * @param principal the principal from which to extract the userid
     * @return the user id of the provided principal
     */
    Optional<String> getSpinnakerUser(Object principal) {
      if (principal instanceof Authentication) {
        return authenticatedUserSupport.getUsernameFromAuthentication((Authentication) principal);
      }
      if (principal instanceof UserDetails) {
        return Optional.ofNullable(((UserDetails) principal).getUsername());
      }

      return Optional.empty();
    }
  }

  private static String normalizedAccountsFromCollection(Collection<String> accounts) {
    if (accounts == null) {
      return null;
    }

    String csv =
        accounts.stream()
            .filter(a -> a != null && !a.trim().isEmpty())
            .map(String::trim)
            .collect(Collectors.joining(","));

    if (csv.isEmpty()) {
      return null;
    }
    return csv;
  }

  /** A static singleton reference to the PrincipalExtractor for AuthenticatedRequest. */
  private static final AtomicReference<PrincipalExtractor> PRINCIPAL_EXTRACTOR =
      new AtomicReference<>(new PrincipalExtractor(new DefaultAuthenticatedUserSupport()));

  /**
   * Replaces the AuthenticatedUserSupport for ALL callers of AuthenticatedRequest.
   *
   * <p>This is a gross and terrible thing, and exists because we made everything in
   * AuthenticatedRequest static. This exists as a terrible DI mechanism to support supplying a
   * different opinion on how to pull details from the current user principal, and should only be
   * called at app initialization time to inject that opinion.
   *
   * @param authenticatedUserSupport the AuthenticatedUserSupport
   */
  public static void setAuthenticatedUserSupport(
      @Nonnull AuthenticatedUserSupport authenticatedUserSupport) {
    Objects.requireNonNull(authenticatedUserSupport, "AuthenticatedUserSupport is required");
    PRINCIPAL_EXTRACTOR.set(new PrincipalExtractor(authenticatedUserSupport));
    log.info(
        "replaced AuthenticatedRequest PrincipalExtractor AuthenticatedUserSupport with {}",
        authenticatedUserSupport.getClass().getSimpleName());
  }

  /**
   * Allow a given HTTP call to be anonymous. Normally, all requests to Spinnaker services should be
   * authenticated (i.e. include USER &amp; ACCOUNTS HTTP headers). However, in specific cases it is
   * necessary to make an anonymous call. If an anonymous call is made that is not wrapped in this
   * method, it will result in a log message and a metric being logged (indicating a potential bug).
   * Use this method to avoid the log and metric. To make an anonymous call wrap it in this
   * function, e.g.
   *
   * <pre><code>AuthenticatedRequest.allowAnonymous(() -&gt; { // do HTTP call here });</code></pre>
   */
  @SneakyThrows(Exception.class)
  public static <V> V allowAnonymous(Callable<V> closure) {
    String originalValue = MDC.get(Header.XSpinnakerAnonymous);
    MDC.put(Header.XSpinnakerAnonymous, "anonymous");

    try {
      return closure.call();
    } finally {
      setOrRemoveMdc(Header.XSpinnakerAnonymous, originalValue);
    }
  }

  /**
   * Prepare an authentication context to run as the supplied user wrapping the supplied action
   *
   * <p>The original authentication context is restored after the action completes.
   *
   * @param username the username to run as
   * @param closure the action to run as the user
   * @param <V> the return type of the supplied action
   * @return an action that will run the supplied action as the supplied user
   */
  @SuppressWarnings("unused")
  public static <V> Callable<V> runAs(String username, Callable<V> closure) {
    return runAs(username, Collections.emptySet(), closure);
  }

  /**
   * Prepare an authentication context to run as the supplied user wrapping the supplied action
   *
   * @param username the username to run as
   * @param restoreOriginalContext whether the original authentication context should be restored
   *     after the action completes
   * @param closure the action to run as the user
   * @param <V> the return type of the supplied action
   * @return an action that will run the supplied action as the supplied user
   */
  @SuppressWarnings("unused")
  public static <V> Callable<V> runAs(
      String username, boolean restoreOriginalContext, Callable<V> closure) {
    return runAs(username, Collections.emptySet(), restoreOriginalContext, closure);
  }

  /**
   * Prepare an authentication context to run as the supplied user wrapping the supplied action
   *
   * <p>The original authentication context is restored after the action completes.
   *
   * @param username the username to run as
   * @param allowedAccounts the allowed accounts for the user as an authorization fallback
   * @param closure the action to run as the user
   * @param <V> the return type of the supplied action
   * @return an action that will run the supplied action as the supplied user
   */
  public static <V> Callable<V> runAs(
      String username, Collection<String> allowedAccounts, Callable<V> closure) {
    return runAs(username, allowedAccounts, true, closure);
  }

  /**
   * Prepare an authentication context to run as the supplied user wrapping the supplied action
   *
   * @param username the username to run as
   * @param allowedAccounts the allowed accounts for the user as an authorization fallback
   * @param restoreOriginalContext whether the original authentication context should be restored
   *     after the action completes
   * @param closure the action to run as the user
   * @param <V> the return type of the supplied action
   * @return an action that will run the supplied action as the supplied user
   */
  public static <V> Callable<V> runAs(
      String username,
      Collection<String> allowedAccounts,
      boolean restoreOriginalContext,
      Callable<V> closure) {
    String spinnakerAccounts = normalizedAccountsFromCollection(allowedAccounts);
    return wrapCallableForPrincipal(closure, restoreOriginalContext, username, spinnakerAccounts);
  }

  /**
   * Propagates the current users authentication context when for the supplied action
   *
   * <p>The original authentication context is restored after the action completes.
   *
   * @param closure the action to run
   * @param <V> the return type of the supplied action
   * @return an action that will run propagating the current users authentication context
   */
  public static <V> Callable<V> propagate(Callable<V> closure) {
    return propagate(closure, true);
  }

  /**
   * Propagates the current users authentication context for the supplied action
   *
   * @param closure the action to run
   * @param restoreOriginalContext whether the original authentication context should be restored
   *     after the action completes
   * @param <V> the return type of the supplied action
   * @return an action that will run propagating the current users authentication context
   */
  public static <V> Callable<V> propagate(Callable<V> closure, boolean restoreOriginalContext) {
    String user = getSpinnakerUser().orElse(null);
    String accounts = getSpinnakerAccounts().orElse(null);
    return wrapCallableForPrincipal(closure, restoreOriginalContext, user, accounts);
  }

  /** @deprecated use runAs instead to switch to a different user */
  @Deprecated
  public static <V> Callable<V> propagate(Callable<V> closure, Object principal) {
    return propagate(closure, true, principal);
  }

  /** @deprecated use runAs instead to switch to a different user */
  @Deprecated
  public static <V> Callable<V> propagate(
      Callable<V> closure, boolean restoreOriginalContext, Object principal) {
    if (principal == null) {
      throw new SpinnakerException("unable to propagate request with null principal")
          .setRetryable(false);
    }
    String accounts =
        getSpinnakerAccounts(principal)
            .map(AuthenticatedRequest::normalizedAccountsFromCollection)
            .orElse(null);
    String username =
        getSpinnakerUser(principal)
            .orElseThrow(
                () ->
                    new SpinnakerException(
                            "unable to retrieve username from supplied principal of type "
                                + principal.getClass().getName())
                        .setRetryable(false));

    return wrapCallableForPrincipal(closure, restoreOriginalContext, username, accounts);
  }

  private static <V> Callable<V> wrapCallableForPrincipal(
      Callable<V> closure,
      boolean restoreOriginalContext,
      String spinnakerUser,
      String spinnakerAccounts) {
    String userOrigin = getSpinnakerUserOrigin().orElse(null);
    String executionId = getSpinnakerExecutionId().orElse(null);
    String requestId = getSpinnakerRequestId().orElse(null);
    String spinnakerApp = getSpinnakerApplication().orElse(null);

    return () -> {
      // Deal with (set/reset) known X-SPINNAKER headers, all others will just stick around
      Map<String, String> originalMdc = MDC.getCopyOfContextMap();

      try {
        setOrRemoveMdc(Header.USER.getHeader(), spinnakerUser);
        setOrRemoveMdc(Header.USER_ORIGIN.getHeader(), userOrigin);
        setOrRemoveMdc(Header.ACCOUNTS.getHeader(), spinnakerAccounts);
        setOrRemoveMdc(Header.REQUEST_ID.getHeader(), requestId);
        setOrRemoveMdc(Header.EXECUTION_ID.getHeader(), executionId);
        setOrRemoveMdc(Header.APPLICATION.getHeader(), spinnakerApp);

        return closure.call();
      } finally {
        clear();

        if (restoreOriginalContext && originalMdc != null) {
          MDC.setContextMap(originalMdc);
        }
      }
    };
  }

  public static Map<String, Optional<String>> getAuthenticationHeaders() {
    Map<String, Optional<String>> headers = new HashMap<>();
    headers.put(Header.USER.getHeader(), getSpinnakerUser());
    headers.put(Header.ACCOUNTS.getHeader(), getSpinnakerAccounts());

    // Copy all headers that look like X-SPINNAKER*
    Map<String, String> allMdcEntries = MDC.getCopyOfContextMap();

    if (allMdcEntries != null) {
      for (Map.Entry<String, String> mdcEntry : allMdcEntries.entrySet()) {
        String header = mdcEntry.getKey();

        boolean isSpinnakerHeader =
            header.toLowerCase().startsWith(Header.XSpinnakerPrefix.toLowerCase());
        boolean isSpinnakerAuthHeader =
            Header.USER.getHeader().equalsIgnoreCase(header)
                || Header.ACCOUNTS.getHeader().equalsIgnoreCase(header);

        if (isSpinnakerHeader && !isSpinnakerAuthHeader) {
          headers.put(header, Optional.ofNullable(mdcEntry.getValue()));
        }
      }
    }

    return headers;
  }

  public static Optional<String> getSpinnakerUser() {
    return PRINCIPAL_EXTRACTOR.get().getSpinnakerUser();
  }

  private static Optional<String> getSpinnakerUser(Object principal) {
    return PRINCIPAL_EXTRACTOR.get().getSpinnakerUser(principal);
  }

  public static Optional<String> getSpinnakerAccounts() {
    return PRINCIPAL_EXTRACTOR.get().getSpinnakerAccounts();
  }

  private static Optional<Collection<String>> getSpinnakerAccounts(Object principal) {
    return PRINCIPAL_EXTRACTOR.get().getSpinnakerAccounts(principal);
  }

  /**
   * Returns or creates a spinnaker request ID.
   *
   * <p>If a request ID already exists, it will be propagated without change. If a request ID does
   * not already exist:
   *
   * <p>1. If an execution ID exists, it will create a hierarchical request ID using the execution
   * ID, followed by a UUID. 2. If an execution ID does not exist, it will create a simple UUID
   * request id.
   */
  public static Optional<String> getSpinnakerRequestId() {
    return Optional.of(
        get(Header.REQUEST_ID)
            .orElse(
                getSpinnakerExecutionId()
                    .map(id -> format("%s:%s", id, UUID.randomUUID().toString()))
                    .orElse(UUID.randomUUID().toString())));
  }

  public static Optional<String> getSpinnakerExecutionType() {
    return get(Header.EXECUTION_TYPE);
  }

  public static Optional<String> getSpinnakerUserOrigin() {
    return get(Header.USER_ORIGIN);
  }

  public static Optional<String> getSpinnakerExecutionId() {
    return get(Header.EXECUTION_ID);
  }

  public static Optional<String> getSpinnakerApplication() {
    return get(Header.APPLICATION);
  }

  public static Optional<String> get(Header header) {
    return get(header.getHeader());
  }

  public static Optional<String> get(String header) {
    return Optional.ofNullable(MDC.get(header));
  }

  public static void setAccounts(String accounts) {
    set(Header.ACCOUNTS, accounts);
  }

  public static void setUser(String user) {
    set(Header.USER, user);
  }

  public static void setUserOrigin(String value) {
    set(Header.USER_ORIGIN, value);
  }

  public static void setRequestId(String value) {
    set(Header.REQUEST_ID, value);
  }

  public static void setExecutionId(String value) {
    set(Header.EXECUTION_ID, value);
  }

  public static void setApplication(String value) {
    set(Header.APPLICATION, value);
  }

  public static void setExecutionType(String value) {
    set(Header.EXECUTION_TYPE, value);
  }

  public static void set(Header header, String value) {
    set(header.getHeader(), value);
  }

  public static void set(String header, String value) {
    Preconditions.checkArgument(
        header.startsWith(Header.XSpinnakerPrefix),
        "Header '%s' does not start with 'X-SPINNAKER-'",
        header);
    MDC.put(header, value);
  }

  public static void clear() {
    MDC.clear();

    try {
      // force clear to avoid the potential for a memory leak if log4j is being used
      Class<?> log4jMDC = Class.forName("org.apache.log4j.MDC");
      log4jMDC.getDeclaredMethod("clear").invoke(null);
    } catch (Exception ignored) {
    }
  }

  private static void setOrRemoveMdc(String key, String value) {
    if (value != null) {
      MDC.put(key, value);
    } else {
      MDC.remove(key);
    }
  }
}
