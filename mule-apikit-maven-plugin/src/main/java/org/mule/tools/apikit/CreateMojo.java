/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.apikit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.Scanner;
import org.mule.apikit.model.ApiSpecification;
import org.mule.apikit.model.api.ApiReference;
import org.mule.parser.service.ParserService;
import org.mule.parser.service.result.ParseResult;
import org.mule.tools.apikit.model.ConfigurationGroup;
import org.mule.tools.apikit.model.CustomConfiguration;
import org.mule.tools.apikit.model.MuleConfig;
import org.mule.tools.apikit.model.MuleConfigBuilder;
import org.mule.tools.apikit.model.MuleDomain;
import org.mule.tools.apikit.model.RuntimeEdition;
import org.mule.tools.apikit.model.ScaffolderContext;
import org.mule.tools.apikit.model.ScaffolderContextBuilder;
import org.mule.tools.apikit.model.ScaffolderResource;
import org.mule.tools.apikit.model.ScaffoldingConfiguration;
import org.mule.tools.apikit.model.ScaffoldingConfigurationMojo;
import org.mule.tools.apikit.model.ScaffoldingResult;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Goal for apikit:create
 */
@Mojo(name = "create")
public class CreateMojo
    extends AbstractMojo {

  @Component
  private BuildContext buildContext;

  /**
   * Pattern of where to find the spec .raml, .yaml or .yml files.
   */
  @Parameter
  private String[] specIncludes =
      new String[] {"src/main/resources/api/**/*.yaml", "src/main/resources/api/**/*.yml", "src/main/resources/api/**/*.raml",
          "src/main/resources/api/**/*.json"};

  /**
   * Pattern of what to exclude searching for .yaml files.
   */
  @Parameter
  private String[] specExcludes = new String[] {};

  /**
   * Spec source directory to use as root of specInclude and specExclude patterns.
   */
  @Parameter(defaultValue = "${basedir}")
  private File specDirectory;

  /**
   * Pattern of where to find the Mule XMLs.
   */
  @Parameter
  private String[] muleXmlIncludes = new String[] {"src/main/mule/**/*.xml", "src/main/resources/**/*.xml"};

  /**
   * Pattern of what to exclude searching for Mule XML files.
   */
  @Parameter
  private String[] muleXmlExcludes = new String[] {};

  /**
   * Spec source directory to use as root of muleInclude and muleExclude patterns.
   */
  @Parameter(defaultValue = "${basedir}")
  private File muleXmlDirectory;

  /**
   * Where to output the generated mule config files.
   */
  @Parameter(defaultValue = "${basedir}/src/main/mule")
  private File muleXmlOutputDirectory;

  /**
   * Where to output the generated mule config files.
   */
  @Parameter(defaultValue = "${basedir}/src/main/resources")
  private File muleResourcesOutputDirectory;

  /**
   * Spec source directory to use as root of muleDomain.
   */
  @Parameter(property = "domainDirectory")
  private File domainDirectory;

  /**
   * Mule version that is being used.
   */
  @Parameter(property = "minMuleVersion")
  private String minMuleVersion;

  /**
   * Mule runtime edition that is being used.
   */
  @Parameter(property = "runtimeEdition", defaultValue = "CE")
  private String runtimeEdition;

  @Parameter(property = "scaffoldingConfigurationFile")
  private File scaffoldingConfigurationFile;

  private Log log;

  List<String> getIncludedFiles(File sourceDirectory, String[] includes, String[] excludes) {
    Scanner scanner = buildContext.newScanner(sourceDirectory, true);
    scanner.setIncludes(includes);
    scanner.setExcludes(excludes);
    scanner.scan();

    String[] includedFiles = scanner.getIncludedFiles();
    for (int i = 0; i < includedFiles.length; i++) {
      includedFiles[i] = new File(scanner.getBasedir(), includedFiles[i]).getAbsolutePath();
    }

    String[] result = new String[includedFiles.length];
    System.arraycopy(includedFiles, 0, result, 0, includedFiles.length);
    return Arrays.asList(result);
  }


  private List<MuleConfig> createMuleConfigsFromLocations(List<String> muleConfigsPaths) {
    List<MuleConfig> muleConfigs = new ArrayList<>();
    for (String location : muleConfigsPaths) {
      try {
        muleConfigs.add(MuleConfigBuilder.fromStream(Files.newInputStream(Paths.get(location)), false));
      } catch (Exception e) {
        log.warn(location + " could not be parsed as mule config");
      }
    }
    return muleConfigs;
  }


  public void execute() throws MojoExecutionException {
    Validate.notNull(muleXmlDirectory, "Error: muleXmlDirectory parameter cannot be null");
    Validate.notNull(specDirectory, "Error: specDirectory parameter cannot be null");

    log = getLog();

    ObjectMapper mapper = new ObjectMapper();
    ScaffoldingConfigurationMojo scaffoldingConfigurationMojo = null;
    try {
       scaffoldingConfigurationMojo = mapper.readValue(scaffoldingConfigurationFile, ScaffoldingConfigurationMojo.class);
    } catch (IOException e) {
      e.printStackTrace();
    }

    List<String> specFiles = getIncludedFiles(specDirectory, specIncludes, specExcludes);
    List<String> muleXmlFiles = getIncludedFiles(muleXmlDirectory, muleXmlIncludes, muleXmlExcludes);
    String domainFile = processDomain();
    if (minMuleVersion != null) {
      log.info("Mule version provided: " + minMuleVersion);
    }
    log.info("Processing the following RAML files: " + specFiles);
    log.info("Processing the following xml files as mule configs: " + muleXmlFiles);

    MainAppScaffolder mainAppScaffolder = getMainAppScaffolder();
    List<MuleConfig> muleConfigs = createMuleConfigsFromLocations(muleXmlFiles);
    List<ApiSpecification> apiSpecificationList = getApiSpecifications(specFiles);
    ScaffoldingConfiguration.Builder configurationBuilder = getConfigurationBuilder(domainFile, muleConfigs);

    for (ApiSpecification apiSpecification : apiSpecificationList) {
      try {
        configurationBuilder.withShowConsole(scaffoldingConfigurationMojo.isShowConsole());
        ConfigurationGroup configurationGroup = scaffoldingConfigurationMojo.getConfigurationGroup();
        if(configurationGroup != null){
          configurationGroup.setPath(muleResourcesOutputDirectory.getPath());
        }
        CustomConfiguration customConfiguration = new CustomConfiguration(scaffoldingConfigurationMojo.getExternalCommonFile(), scaffoldingConfigurationMojo.getApiId(), configurationGroup);

        if(customConfiguration.getExternalConfigurationFile().isPresent() && !FilenameUtils.getExtension(customConfiguration.getExternalConfigurationFile().get()).equals("xml")){
          throw new RuntimeException("externalCommonFile must end with .xml");
        }
        configurationBuilder.withCustomConfiguration(customConfiguration);
        ScaffoldingConfiguration configuration = configurationBuilder.withApi(apiSpecification).build();
        ScaffoldingResult result = mainAppScaffolder.run(configuration);

        if (result.isSuccess()) {
          copyGeneratedConfigs(result.getGeneratedConfigs(), muleXmlOutputDirectory);
          copyGeneratedResources(result.getGeneratedResources(), muleResourcesOutputDirectory);
        }
      } catch (Exception e) {
        throw new MojoExecutionException(e.getMessage());
      }
    }
  }

  private static ScaffoldingConfiguration.Builder getConfigurationBuilder(String domainFile, List<MuleConfig> muleConfigs) throws MojoExecutionException {
    ScaffoldingConfiguration.Builder configurationBuilder = new ScaffoldingConfiguration.Builder();
    configurationBuilder.withMuleConfigurations(muleConfigs);
    if (domainFile != null) {
      MuleDomain muleDomain;
      try {
        muleDomain = MuleDomain.fromInputStream(Files.newInputStream(Paths.get(domainFile)));
      } catch (Exception e) {
        throw new MojoExecutionException(e.getMessage());
      }
      configurationBuilder.withDomain(muleDomain);
    }
    return configurationBuilder;
  }

  private static void copyGeneratedConfigs(List<MuleConfig> generatedConfigs, File muleXmlDirectory) throws IOException {
    for (MuleConfig generatedConfig : generatedConfigs) {
      String name = generatedConfig.getName();
      name = StringUtils.isBlank(name) ? "api.xml" : name;
      File file = new File(muleXmlDirectory, name);
      try (FileOutputStream stream = new FileOutputStream(file)) {
        IOUtils.copy(generatedConfig.getContent(), stream);
      }
    }
  }

  private static void copyGeneratedResources(List<ScaffolderResource> generatedResources, File muleXmlDirectory) throws IOException {
    for (ScaffolderResource scaffolderResource : generatedResources) {
      String name = scaffolderResource.getName();
      name = StringUtils.isBlank(name) ? "api.xml" : name;
      File file = new File(muleXmlDirectory, name);
      try (FileOutputStream stream = new FileOutputStream(file)) {
        IOUtils.copy(scaffolderResource.getContent(), stream);
      }
    }
  }

  private MainAppScaffolder getMainAppScaffolder() {
    RuntimeEdition muleRuntimeEdition = RuntimeEdition.valueOf(this.runtimeEdition);
    ScaffolderContext context = ScaffolderContextBuilder.builder().withRuntimeEdition(muleRuntimeEdition).build();
    return new MainAppScaffolder(context);
  }

  private List<ApiSpecification> getApiSpecifications(List<String> specFiles) {
    List<ApiSpecification> apiSpecificationList = new ArrayList<>();
    for (String specFile : specFiles) {
      ApiReference apiReference = ApiReference.create(Paths.get(specFile).toUri());
      ParseResult parseResult = new ParserService().parse(apiReference);

      if (parseResult.success()) {
        apiSpecificationList.add(parseResult.get());
      }
    }
    return apiSpecificationList;
  }

  private String processDomain() {
    String domainFile = null;

    if (domainDirectory != null) {
      List<String> domainFiles = getIncludedFiles(domainDirectory, new String[]{"*.xml"}, new String[]{});
      if (domainFiles.size() > 0) {
        domainFile = domainFiles.get(0);
        if (domainFiles.size() > 1) {
          log.info("There is more than one domain file inside of the domain folder. The domain: " + domainFile
              + " will be used.");
        }
      } else {
        log.error("The specified domain directory [" + domainDirectory + "] does not contain any xml file.");
      }
    } else {
      log.info("No domain was provided. To send it, use -DdomainDirectory.");
    }
    return domainFile;
  }
}
