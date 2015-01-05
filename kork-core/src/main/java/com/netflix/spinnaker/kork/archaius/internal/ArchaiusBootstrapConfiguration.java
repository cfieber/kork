/*
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.kork.archaius.internal;

import com.netflix.config.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Configuration
public class ArchaiusBootstrapConfiguration {
  private static final String SPRING_ENVIRONMENT_BRIDGE = "SpringEnvironment";
  private static final String ARCHAIUS_BRIDGE = "ArchaiusPropertySource";

  @Autowired
  ConfigurableEnvironment environment;

  /**
   * Spring environment properties are polled and available as dynamic properties in archaius.
   *
   * @return the PolledConfigurationSource that reads Springs Environment
   */
  @Bean
  PolledConfigurationSource springEnvironmentSource() {
    return new SpringEnvironmentConfigurationSource(environment, ARCHAIUS_BRIDGE);
  }

  /**
   * Any additional configurations loaded via archaius configuration manager will show up in Springs Environment
   *
   * @return the PropertySource that reads from Archaius
   */
  @Bean
  AggregatedConfigurationPropertySource aggregatedConfigurationPropertySource() {
    return new AggregatedConfigurationPropertySource(ARCHAIUS_BRIDGE, archaiusConfiguration(), SPRING_ENVIRONMENT_BRIDGE);
  }


  @Bean
  FixedDelayPollingScheduler springConfigurationPoller() {
    return new FixedDelayPollingScheduler(0, (int) TimeUnit.SECONDS.toMillis(30), false);
  }

  @Bean
  Properties springBootDeploymentContextProperties() {
    Properties props = new Properties();
    if (System.getProperties().containsKey("spring.config.name")) {
      props.setProperty(DeploymentContext.ContextKey.appId.getKey(), System.getProperty("spring.config.name"));
    }

    String environmentProfile = null;
    for (String profile : environment.getActiveProfiles()) {
      if (!("local".equals(profile) || "default".equals(profile))) {
        environmentProfile = profile;
        break;
      }
    }
    if (environmentProfile != null) {
      props.setProperty(DeploymentContext.ContextKey.environment.getKey(), environmentProfile);
    }

    return props;
  }

  @Bean
  DynamicConfiguration springConfiguration() {
    return new DynamicConfiguration(springEnvironmentSource(), springConfigurationPoller());
  }

  @Bean
  ConcurrentCompositeConfiguration archaiusConfiguration() {
    ConcurrentCompositeConfiguration config = new ConcurrentCompositeConfiguration();
    config.addConfiguration(springConfiguration(), SPRING_ENVIRONMENT_BRIDGE);
    return config;
  }
}
