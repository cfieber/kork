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

import com.netflix.config.ConfigurationManager;
import com.netflix.spinnaker.kork.archaius.internal.AggregatedConfigurationPropertySource;
import com.netflix.spinnaker.kork.archaius.internal.ArchaiusBootstrapConfiguration;
import org.apache.commons.configuration.AbstractConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import javax.annotation.PostConstruct;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnClass(ConfigurationManager.class)
//TODO(cfieber) @ConditionalOnExpression("!ConfigurationManager.isConfigurationInstalled()")
@Import(ArchaiusBootstrapConfiguration.class)
public class ArchaiusComponents {

  @Autowired
  AbstractConfiguration archaiusConfiguration;

  @Autowired
  AggregatedConfigurationPropertySource archaiusPropertySource;

  @Autowired
  ConfigurableApplicationContext configurableApplicationContext;

  @PostConstruct
  public void installConfig() {
    ConfigurationManager.install(archaiusConfiguration);
    configurableApplicationContext.getEnvironment().getPropertySources().addFirst(archaiusPropertySource);
  }
}
