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

package com.netflix.spinnaker.kork;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.LookupService;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.ResourcePropertySource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlatformComponentsTest {

  @BeforeClass
  public static void init() {
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
  }

  @Test
  public void basicContextCreation() {
    try (ConfigurableApplicationContext ctx = createContext()) {
      Assert.assertNotNull(ctx.getBean(InstanceInfo.class));
    }
  }

  private AnnotationConfigApplicationContext createContext(Class<?>... configurations) {
    List<Class<?>> configs = new ArrayList<>(Arrays.asList(BaseApplicationContext.class, PlatformComponents.class));
    configs.addAll(Arrays.asList(configurations));

    return new AnnotationConfigApplicationContext(configs.toArray(new Class<?>[configs.size()]));
  }

  @Configuration
  public static class BaseApplicationContext {

    @Autowired
    ConfigurableApplicationContext ctx;

    @Bean
    static PropertySourcesPlaceholderConfigurer ppc() {
      return new PropertySourcesPlaceholderConfigurer();
    }

    @PostConstruct
    public void initialize() {
      EncodedResource testProps = new EncodedResource(new ClassPathResource("/test.properties"));
      try {
        ctx.getEnvironment().getPropertySources().addFirst(new ResourcePropertySource("test", testProps));
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
  }
}
