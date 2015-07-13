/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.kork.eureka;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import com.netflix.discovery.shared.LookupService;
import com.netflix.spinnaker.kork.archaius.ArchaiusComponents;
import com.netflix.spinnaker.kork.eureka.internal.EurekaBootstrapConfiguration;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
@Import(EurekaBootstrapConfiguration.class)
@AutoConfigureAfter(ArchaiusComponents.class)
public class EurekaComponents {

  @Bean
  @ConditionalOnMissingBean(ApplicationInfoManager.class)
  public ApplicationInfoManager applicationInfoManager(EurekaInstanceConfig eurekaInstanceConfig) {
    ApplicationInfoManager aim = ApplicationInfoManager.getInstance();
    aim.initComponent(eurekaInstanceConfig);
    return aim;
  }

  @Bean
  @ConditionalOnMissingBean(InstanceInfo.class)
  public InstanceInfo instanceInfo(ApplicationInfoManager applicationInfoManager) {
    return applicationInfoManager.getInfo();
  }

  @Bean
  @ConditionalOnMissingBean(DiscoveryClient.class)
  public DiscoveryClient discoveryClient(ApplicationInfoManager applicationInfoManager, EurekaClientConfig eurekaClientConfig, DiscoveryClient.DiscoveryClientOptionalArgs discoveryClientOptionalArgs) {
    return new DiscoveryClient(applicationInfoManager, eurekaClientConfig, discoveryClientOptionalArgs);
  }
}
