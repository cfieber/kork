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

import com.netflix.config.PollResult;
import com.netflix.config.PolledConfigurationSource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import java.util.*;

public class SpringEnvironmentConfigurationSource implements PolledConfigurationSource {
  private static final Object NULL = new Object();
  private final ConfigurableEnvironment environment;
  private final Set<String> excludedSources;

  public SpringEnvironmentConfigurationSource(ConfigurableEnvironment environment, String... excludedSources) {
    this.environment = environment;
    this.excludedSources = new HashSet<>(Arrays.asList(excludedSources));
  }

  @Override
  public PollResult poll(boolean initial, Object checkPoint) throws Exception {
    final Map<String, Object> properties = new HashMap<>();
    for (PropertySource source : environment.getPropertySources()) {
      if (!excludedSources.contains(source.getName()) && source instanceof EnumerablePropertySource) {
        for (String propertyName : ((EnumerablePropertySource) source).getPropertyNames()) {
          if (propertyName != null && !properties.containsKey(propertyName)) {
            Object value = source.getProperty(propertyName);
            if (value == null) {
              value = NULL;
            }
            properties.put(propertyName, value);
          }
        }
      }
    }
    final Map<String, Object> nullStripped = new HashMap<>(properties.size());
    for (Map.Entry<String, Object> entry : properties.entrySet()) {
      if (entry.getValue() != NULL) {
        nullStripped.put(entry.getKey(), entry.getValue());
      }
    }
    return PollResult.createFull(nullStripped);
  }
}
