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

import com.netflix.config.AggregatedConfiguration;
import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.configuration.Configuration;
import org.springframework.core.env.EnumerablePropertySource;

import java.util.*;

public class AggregatedConfigurationPropertySource extends EnumerablePropertySource<AggregatedConfiguration> {

  private final Set<String> ignoredConfigurationNames;

  public AggregatedConfigurationPropertySource(String name, AggregatedConfiguration source, String... ignoredConfigurationNames) {
    this(name, source, new HashSet<>(Arrays.asList(ignoredConfigurationNames)));
  }

  public AggregatedConfigurationPropertySource(String name, AggregatedConfiguration source, Set<String> ignoredConfigurationNames) {
    super(name, source);
    this.ignoredConfigurationNames = ignoredConfigurationNames == null ? Collections.<String>emptySet() : Collections.unmodifiableSet(ignoredConfigurationNames);
  }

  @Override
  public Object getProperty(String name) {
    Set<String> validIgnoredConfigurationNames = new HashSet<>(ignoredConfigurationNames);
    validIgnoredConfigurationNames.retainAll(getSource().getConfigurationNames());
    IdentityHashMap<Configuration, Boolean> ignoreConfigs = new IdentityHashMap<>();
    for (String ignoreConfig : validIgnoredConfigurationNames) {
      Configuration config = getSource().getConfiguration(ignoreConfig);
      if (config != null) {
        ignoreConfigs.put(config, Boolean.TRUE);
      }
    }
    for (Configuration config : this.getSource().getConfigurations()) {
      if (!ignoreConfigs.containsKey(config)) {
        try {
          Object result = config.getProperty(name);
          if (result != null) {
            return result;
          }
        } catch (NoSuchElementException ignored) {
          // keep trying down the configuration list
        }
      }
    }
    return null;
  }

  @Override
  public String[] getPropertyNames() {
    List<String> keys = new ArrayList<>();
    for (Iterator<String> i = getSource().getKeys(); i.hasNext(); ) {
      keys.add(i.next());
    }
    return keys.toArray(new String[keys.size()]);
  }
}
