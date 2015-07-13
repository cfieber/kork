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

package com.netflix.spinnaker.kork.archaius;

import com.netflix.config.ConcurrentCompositeConfiguration;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DeploymentContext;
import com.netflix.spinnaker.kork.archaius.internal.AggregatedConfigurationPropertySource;
import com.netflix.spinnaker.kork.archaius.internal.ArchaiusBootstrapConfiguration;
import jdk.nashorn.internal.runtime.regexp.joni.Config;
import org.apache.commons.configuration.AbstractConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;


@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnClass(ConfigurationManager.class)
@Import(ArchaiusBootstrapConfiguration.class)
public class ArchaiusComponents {

  @Autowired
  AbstractConfiguration archaiusConfiguration;

  @Autowired
  AggregatedConfigurationPropertySource archaiusPropertySource;

  @Autowired
  ConfigurableApplicationContext configurableApplicationContext;

  @Autowired
  Properties springBootDeploymentContextProperties;

  @Bean
  AbstractConfiguration archaiusConfigInstance() {
    configurableApplicationContext.getEnvironment().getPropertySources().addFirst(archaiusPropertySource);
    configurableApplicationContext.getEnvironment().getPropertySources().addLast(new PropertiesPropertySource("bootDeploymentContextBridge", springBootDeploymentContextProperties));
    return ConfigurationManager.getConfigInstance();
  }

  @Bean
  @DependsOn("archaiusConfigInstance")
  DeploymentContext archaiusDeploymentContext() {
    return ConfigurationManager.getDeploymentContext();
  }
}
