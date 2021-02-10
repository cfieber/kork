/*
 * Copyright 2020 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.security;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * @deprecated this is kind of a hack to coerce allowed accounts into granted authorities. A better
 *     solution would be to supply a UserDetails or Authentication details that implements {{@link
 *     HasAllowedAccounts}}.
 */
@Deprecated
public class AllowedAccountsAuthorities {
  public static final String PREFIX = "ALLOWED_ACCOUNT_";

  public static Collection<GrantedAuthority> getAllowedAccountAuthorities(UserDetails userDetails) {
    if (userDetails == null
        || userDetails.getAuthorities() == null
        || userDetails.getAuthorities().isEmpty()) {
      return Collections.emptySet();
    }

    return filterGrantedAuthorities(userDetails.getAuthorities()).collect(Collectors.toSet());
  }

  public static Collection<String> getAllowedAccounts(UserDetails userDetails) {
    if (userDetails == null) {
      return Collections.emptySet();
    }
    if (userDetails instanceof HasAllowedAccounts) {
      return ((HasAllowedAccounts) userDetails).getAllowedAccounts();
    }
    return getAllowedAccountsFromAuthorities(userDetails.getAuthorities());
  }

  public static Collection<GrantedAuthority> buildAllowedAccounts(Collection<String> accounts) {
    if (accounts == null || accounts.isEmpty()) {
      return Collections.emptySet();
    }

    return accounts.stream()
        .filter(Objects::nonNull)
        .filter(s -> !s.isEmpty())
        .map(String::toLowerCase)
        .map(s -> PREFIX + s)
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toSet());
  }

  /**
   * Package-private for temporary access from DefaultAuthenticatedUserSupport.
   *
   * <p>This should go away once we have refactors all usages of AuthenticatedRequest accepting a
   * principal on propagate..
   */
  static Collection<String> getAllowedAccountsFromAuthorities(
      Collection<? extends GrantedAuthority> authorities) {
    return filterGrantedAuthorities(authorities)
        .map(a -> a.getAuthority().substring(PREFIX.length()))
        .sorted()
        .collect(Collectors.toList());
  }

  private static Stream<? extends GrantedAuthority> filterGrantedAuthorities(
      Collection<? extends GrantedAuthority> authorities) {
    if (authorities == null || authorities.isEmpty()) {
      return Stream.empty();
    }
    return authorities.stream().filter(a -> a.getAuthority().startsWith(PREFIX));
  }
}
