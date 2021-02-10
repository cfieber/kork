package com.netflix.spinnaker.security;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "authentication.config.enabled", matchIfMissing = true)
public class AuthenticationSupportAutoConfiguration {
  @Bean
  @ConditionalOnMissingBean(AuthenticatedUserSupport.class)
  AuthenticatedUserSupport defaultAuthenticatedUserSupport() {
    return new DefaultAuthenticatedUserSupport();
  }

  @Configuration
  static class AuthenticatedRequestConfiguration {
    @Autowired AuthenticatedUserSupport authenticatedUserSupport;

    @PostConstruct
    public void injectAuthenticatedUserSupport() {
      AuthenticatedRequest.setAuthenticatedUserSupport(authenticatedUserSupport);
    }
  }
}
