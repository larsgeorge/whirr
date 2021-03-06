/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.whirr.cli.command;

import static org.apache.whirr.service.ClusterSpec.Property.CLUSTER_NAME;
import static org.apache.whirr.service.ClusterSpec.Property.IDENTITY;

import java.util.EnumSet;
import java.util.Map;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.whirr.cli.Command;
import org.apache.whirr.service.ClusterSpec;
import org.apache.whirr.service.ClusterSpec.Property;
import org.apache.whirr.service.Service;
import org.apache.whirr.service.ServiceFactory;

import com.google.common.collect.Maps;

/**
 * An abstract command for interacting with clusters.
 */
public abstract class AbstractClusterSpecCommand extends Command {

  protected ServiceFactory factory;

  protected OptionParser parser = new OptionParser();
  private Map<Property, OptionSpec> optionSpecs;
  private OptionSpec<String> configOption = parser
    .accepts("config", "Note that Whirr properties specified in " + 
      "this file  should all have a whirr. prefix.")
    .withRequiredArg()
    .describedAs("config.properties")
    .ofType(String.class);

  public AbstractClusterSpecCommand(String name, String description, ServiceFactory factory) {
    super(name, description);
    this.factory = factory;

    optionSpecs = Maps.newHashMap();
    for (Property property : EnumSet.allOf(Property.class)) {
      ArgumentAcceptingOptionSpec<?> spec = parser
        .accepts(property.getSimpleName(), property.getDescription())
        .withRequiredArg()
        .ofType(property.getType());
      if (property.hasMultipleArguments()) {
        spec.withValuesSeparatedBy(',');
      }
      optionSpecs.put(property, spec);
    }
  }

  protected ClusterSpec getClusterSpec(OptionSet optionSet) throws ConfigurationException {
    Configuration optionsConfig = new PropertiesConfiguration();
    for (Map.Entry<Property, OptionSpec> entry : optionSpecs.entrySet()) {
      Property property = entry.getKey();
      OptionSpec option = entry.getValue();
      if (property.hasMultipleArguments()) {
        optionsConfig.setProperty(property.getConfigName(),
            optionSet.valuesOf(option));
      } else {
        optionsConfig.setProperty(property.getConfigName(),
            optionSet.valueOf(option));
      }
    }
    CompositeConfiguration config = new CompositeConfiguration();
    config.addConfiguration(optionsConfig);
    if (optionSet.has(configOption)) {
      Configuration defaults = new PropertiesConfiguration(optionSet.valueOf(configOption));
      config.addConfiguration(defaults);
    }

    for (Property required : EnumSet.of(CLUSTER_NAME, IDENTITY)) {
      if (config.getString(required.getConfigName()) == null) {
        throw new IllegalArgumentException(String.format("Option '%s' not set.",
            required.getSimpleName()));
      }
    }
    return new ClusterSpec(config);
  }

  /**
   * Create the specified service
   * @param serviceName
   * @return
   * @throws IllegalArgumentException if serviceName is not found
   */
  protected Service createService(String serviceName) {
    Service service = factory.create(serviceName);
    if (service == null) {
      throw new IllegalArgumentException("Unable to find service "
          + serviceName + ", exiting");
    }
    return service;
  }

}
