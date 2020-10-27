/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.apikit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
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

import org.apache.maven.shared.utils.io.FileUtils;
import org.mule.tools.apikit.model.MuleConfig;
import org.mule.tools.apikit.model.MuleConfigBuilder;
import org.mule.tools.apikit.model.MuleDomain;
import org.mule.tools.apikit.model.NamedContent;
import org.mule.tools.apikit.model.Properties;
import org.mule.tools.apikit.model.RuntimeEdition;
import org.mule.tools.apikit.model.ScaffolderContext;
import org.mule.tools.apikit.model.ScaffolderContextBuilder;
import org.mule.tools.apikit.model.ScaffolderResource;
import org.mule.tools.apikit.model.ScaffoldingAccessories;
import org.mule.tools.apikit.model.ScaffoldingConfiguration;
import org.mule.tools.apikit.model.ScaffoldingResult;
import org.mule.tools.apikit.utils.ApiSyncResourceLoader;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

/**
 * Goal for apikit:create
 */
@Mojo(name = "create")
public class CreateMojo
        extends AbstractMojo {

    @Component
    private BuildContext buildContext;

    private static String SPEC_FOLDER = "src/main/resources/api";

    /**
     * Pattern of where to find the spec .raml, .yaml or .yml files.
     */
    @Parameter
    private String[] specIncludes =
            new String[]{SPEC_FOLDER + "/**/*.yaml", SPEC_FOLDER + "/**/*.yml", SPEC_FOLDER + "/**/*.raml",
                    SPEC_FOLDER + "/**/*.json"};

    /**
     * Pattern of what to exclude searching for .yaml files.
     */
    @Parameter
    private String[] specExcludes = new String[]{};

    /**
     * Spec source directory to use as root of specInclude and specExclude patterns.
     */
    @Parameter(defaultValue = "${basedir}")
    private File specDirectory;

    /**
     * Pattern of where to find the Mule XMLs.
     */
    @Parameter
    private String[] muleXmlIncludes = new String[]{"src/main/mule/**/*.xml", "src/main/resources/**/*.xml"};

    /**
     * Pattern of what to exclude searching for Mule XML files.
     */
    @Parameter
    private String[] muleXmlExcludes = new String[]{};

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
     * Group id of the asset to use to scaffold.
     */
    @Parameter(property = "groupId")
    private String groupId;
    /**
     * Artifact of the asset to use to scaffold.
     */
    @Parameter(property = "artifact")
    private String artifact;
    /**
     * Version of the asset to use to scaffold.
     */
    @Parameter(property = "version")
    private String version;

    /**
     * Mule runtime edition that is being used.
     */
    @Parameter(property = "runtimeEdition", defaultValue = "EE")
    private String runtimeEdition;

    @Parameter(property = "scaffoldingConfigurationFile")
    private File scaffoldingConfigurationFile;

    public static final String COLON = ":";

    private Log log;

    public void execute() throws MojoExecutionException {
        Validate.notNull(muleXmlDirectory, "Error: muleXmlDirectory parameter cannot be null");
        Validate.notNull(specDirectory, "Error: specDirectory parameter cannot be null");
        validateGAV();
        log = getLog();
        ScaffoldingAccessories scaffoldingAccessories = readScaffoldingConfigurationMojo();
        validateExternalCommonFile(scaffoldingAccessories);
        List<String> muleXmlFiles = getIncludedFiles(muleXmlDirectory, muleXmlIncludes, muleXmlExcludes);
        String domainFile = processDomain();
        if (minMuleVersion != null) {
            log.info("Mule version provided: " + minMuleVersion);
        }
        MainAppScaffolder mainAppScaffolder = getMainAppScaffolder();
        List<MuleConfig> muleConfigs = createMuleConfigsFromLocations(muleXmlFiles);
        List<ApiSpecification> apiSpecificationList = createAPISpecificationList();
        ScaffoldingConfiguration.Builder configurationBuilder = createBuilder(scaffoldingAccessories, domainFile, muleConfigs);
        for (ApiSpecification apiSpecification : apiSpecificationList) {
            try {
                ScaffoldingConfiguration configuration = configurationBuilder.withApi(apiSpecification).build();
                validateProperties(scaffoldingAccessories);
                ScaffoldingResult result = mainAppScaffolder.run(configuration);
                if (result.isSuccess()) {
                    copyGeneratedNamedContents(result.getGeneratedConfigs(), muleXmlOutputDirectory);
                    copyGeneratedNamedContents(result.getGeneratedResources(), muleResourcesOutputDirectory);
                }
            } catch (Exception e) {
                throw new MojoExecutionException(e.getMessage());
            }
        }
    }

    private ScaffoldingConfiguration.Builder createBuilder(ScaffoldingAccessories scaffoldingAccessories, String domainFile, List<MuleConfig> muleConfigs) throws MojoExecutionException {
        ScaffoldingConfiguration.Builder configurationBuilder = getConfigurationBuilder(domainFile, muleConfigs);
        String apiSyncResource = hasDependency() ? createResourceForApiSync() : null;
        configurationBuilder.withApiSyncResource(apiSyncResource);
        configurationBuilder.withAccessories(scaffoldingAccessories);
        return configurationBuilder;
    }

    private void validateProperties(ScaffoldingAccessories scaffoldingAccessories) throws MojoExecutionException {
        Properties properties = scaffoldingAccessories.getProperties();
        if (properties != null && (isEmpty(properties.getFormat()) || properties.getFiles() == null)) {
            throw new MojoExecutionException("format and files must be present for properties");
        }
    }

    private void validateExternalCommonFile(ScaffoldingAccessories scaffoldingAccessories) throws MojoExecutionException {
        if (isNotEmpty(scaffoldingAccessories.getExternalCommonFile()) && !FilenameUtils.getExtension(scaffoldingAccessories.getExternalCommonFile()).equals("xml")) {
            throw new MojoExecutionException("externalCommonFile must end with .xml");
        }
    }

    private List<ApiSpecification> createAPISpecificationList() {
        List<ApiSpecification> apiSpecificationList = new ArrayList<>();
        if (hasDependency()) {
            String apiSyncPath = createResourceForApiSync();
            ApiReference apiReference = ApiReference.create(apiSyncPath, new ApiSyncResourceLoader());
            ParseResult parseResult = new ParserService().parse(apiReference);
            apiSpecificationList.add(parseResult.get());
        } else {
            List<String> specFiles = getIncludedFiles(specDirectory, specIncludes, specExcludes);
            log.info("Processing the following RAML files: " + specFiles);
            apiSpecificationList = getApiSpecifications(specFiles);
        }
        return apiSpecificationList;
    }

    private boolean hasDependency() {
        return isNotEmpty(groupId) && isNotEmpty(artifact) && isNotEmpty(version);
    }

    private String createResourceForApiSync() {
        return "resource::".concat(groupId).concat(COLON).concat(artifact).concat(COLON)
                .concat(version).concat(COLON).concat("raml").concat(COLON).concat("zip")
                .concat(COLON).concat(artifact).concat(".raml");
    }


    private void validateGAV() throws MojoExecutionException {
        List<String> gavElements = new ArrayList<>();
        gavElements.add(groupId);
        gavElements.add(artifact);
        gavElements.add(version);
        Iterables.removeIf(gavElements, Predicates.isNull());
        if (gavElements.size() > 0 && gavElements.size() < 3) {
            throw new MojoExecutionException("Any argument from the groupId-artifact-version is missing.");
        }
    }

    protected ScaffoldingAccessories readScaffoldingConfigurationMojo() throws MojoExecutionException {
        ObjectMapper mapper = new ObjectMapper();
        ScaffoldingAccessories scaffoldingConfigurationMojo;
        try {
            scaffoldingConfigurationMojo = mapper.readValue(scaffoldingConfigurationFile, ScaffoldingAccessories.class);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }
        return scaffoldingConfigurationMojo;
    }


    private static ScaffoldingConfiguration.Builder getConfigurationBuilder(String domainFile, List<MuleConfig> muleConfigs) throws MojoExecutionException {
        ScaffoldingConfiguration.Builder configurationBuilder = new ScaffoldingConfiguration.Builder();
        configurationBuilder.withMuleConfigurations(muleConfigs);
        if (domainFile != null) {
            try {
                MuleDomain muleDomain = MuleDomain.fromInputStream(Files.newInputStream(Paths.get(domainFile)));
                configurationBuilder.withDomain(muleDomain);
            } catch (Exception e) {
                throw new MojoExecutionException(e.getMessage());
            }
        }
        return configurationBuilder;
    }

    private static <T extends NamedContent> void copyGeneratedNamedContents(List<T> generatedNamedContents, File muleXmlDirectory) throws
            IOException {
        for (T generatedNamedContent : generatedNamedContents) {
            String name = generatedNamedContent.getName();
            name = isBlank(name) ? "api.xml" : name;
            File file = new File(muleXmlDirectory, name);
            try (FileOutputStream stream = new FileOutputStream(file)) {
                IOUtils.copy(generatedNamedContent.getContent(), stream);
            }
        }
    }

    private MainAppScaffolder getMainAppScaffolder() {
        RuntimeEdition muleRuntimeEdition = RuntimeEdition.valueOf(this.runtimeEdition);
        ScaffolderContext context = ScaffolderContextBuilder.builder().withRuntimeEdition(muleRuntimeEdition).build();
        return new MainAppScaffolder(context);
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
                MuleConfig muleConfig = MuleConfigBuilder.fromStream(Files.newInputStream(Paths.get(location)), false);
                muleConfig.setName(FileUtils.filename(location));
                muleConfigs.add(muleConfig);
            } catch (Exception e) {
                log.warn(location + " could not be parsed as mule config");
            }
        }
        return muleConfigs;
    }

}