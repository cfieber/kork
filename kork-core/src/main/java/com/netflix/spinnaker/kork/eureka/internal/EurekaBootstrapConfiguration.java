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

package com.netflix.spinnaker.kork.eureka.internal;

import com.google.inject.Provider;
import com.netflix.appinfo.CloudInstanceConfig;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.eventbus.impl.EventBusImpl;
import com.netflix.eventbus.spi.EventBus;
import com.netflix.eventbus.spi.InvalidSubscriberException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.util.Map;

@Configuration
public class EurekaBootstrapConfiguration {

  @Value("${eureka.instance.namespace:netflix.appinfo.}")
  String eurekaInstanceConfigNamespace;

  @Value("${eureka.client.namespace:netflix.discovery.}")
  String eurekaClientConfigNamespace;

  @Bean
  @ConditionalOnMissingBean(EurekaInstanceConfig.class)
  public EurekaInstanceConfig eurekaInstanceConfig() {
    return new CloudInstanceConfig(fixNamespace(eurekaInstanceConfigNamespace));
  }

  @Bean
  @ConditionalOnMissingBean(EventBus.class)
  public EventBus eventBus() {
    return new EventBusImpl();
  }

  @Bean
  @ConditionalOnBean(HealthAggregator.class)
  public BootHealthCheckHandler bootHealthCheckHandler(DiscoveryClient discoveryClient, HealthAggregator aggregator, Map<String, HealthIndicator> healthIndicators) {
    BootHealthCheckHandler handler = new BootHealthCheckHandler(aggregator, healthIndicators);
    discoveryClient.registerHealthCheck(handler);
    return handler;
  }

  @Bean
  @ConditionalOnMissingBean(DiscoveryClient.DiscoveryClientOptionalArgs.class)
  DiscoveryClient.DiscoveryClientOptionalArgs discoveryClientOptionalArgs(EventBus eventBus) {
    DiscoveryClient.DiscoveryClientOptionalArgs args = new DiscoveryClient.DiscoveryClientOptionalArgs();
    setPrivateField(args, "eventBus", eventBus);
    return args;
  }

  @Bean
  public EurekaEventBridge eurekaEventBridge(EventBus eventBus, ApplicationContext applicationContext) {
    EurekaEventBridge bridge = new EurekaEventBridge(applicationContext);
    try {
      eventBus.registerSubscriber(bridge);
    } catch (InvalidSubscriberException ise) {
      throw new RuntimeException("Failed to register EurekaEventBridge with EventBus", ise);
    }
    return bridge;
  }

  @Bean
  @ConditionalOnMissingBean(EurekaClientConfig.class)
  public EurekaClientConfig eurekaClientConfig() {
    return new DefaultEurekaClientConfig(fixNamespace(eurekaClientConfigNamespace));
  }

  private static String fixNamespace(String namespace) {
    return namespace.endsWith(".") ? namespace : namespace + ".";
  }

  private static void setPrivateField(Object obj, String fieldName, Object value) {
    try {
      Field field = obj.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(obj, value);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("unable to set field " + fieldName, e);
    }
  }
}
