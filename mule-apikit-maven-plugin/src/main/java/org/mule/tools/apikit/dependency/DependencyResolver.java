/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.apikit.dependency;

import org.apache.maven.plugin.MojoExecutionException;
import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.model.BundleDependency;
import org.mule.maven.client.api.model.BundleDescriptor;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.maven.client.internal.AetherMavenClientProvider;
import org.mule.tools.apikit.utils.ZipUtils;

import java.io.File;
import java.net.URI;
import java.util.Optional;

public class DependencyResolver {

    public static final String COLON = ":";

    public static URI resolve(String resource) throws MojoExecutionException {
        MavenClient mavenClient = buildMavenClient();
        BundleDescriptor.Builder bundleDescriptorBuilder = buildBundleDescriptor(resource);
        BundleDependency bundleDependency = mavenClient.resolveBundleDescriptor(bundleDescriptorBuilder.build());
        return ZipUtils.extractMainFileUri(bundleDependency);
    }

    private static MavenClient buildMavenClient() throws MojoExecutionException {
        MavenConfiguration.MavenConfigurationBuilder mavenConfigurationBuilder = MavenConfiguration.newMavenConfigurationBuilder();
        AetherMavenClientProvider aetherMavenClientProvider = new AetherMavenClientProvider();
        setGlobalSettingsLocation(mavenConfigurationBuilder, aetherMavenClientProvider);
        mavenConfigurationBuilder.localMavenRepositoryLocation(aetherMavenClientProvider.getLocalRepositorySuppliers().environmentMavenRepositorySupplier().get());
        mavenConfigurationBuilder.forcePolicyUpdateAlways(true);
        return aetherMavenClientProvider.createMavenClient(mavenConfigurationBuilder.build());
    }

    private static void setGlobalSettingsLocation(MavenConfiguration.MavenConfigurationBuilder mavenConfigurationBuilder, AetherMavenClientProvider aetherMavenClientProvider) throws MojoExecutionException {
        Optional<File> settingsFile = aetherMavenClientProvider.getSettingsSupplierFactory().environmentUserSettingsSupplier();
        if (settingsFile.isPresent()) {
            mavenConfigurationBuilder.globalSettingsLocation(settingsFile.get());
        } else {
            throw new MojoExecutionException("it is mandatory to have a settings.xml configured");
        }
    }

    private static BundleDescriptor.Builder buildBundleDescriptor(String resource) {
        String[] resources = resource.split(COLON);
        BundleDescriptor.Builder bundleDescriptorBuilder = new BundleDescriptor.Builder();
        bundleDescriptorBuilder.setGroupId(resources[2]);
        bundleDescriptorBuilder.setArtifactId(resources[3]);
        bundleDescriptorBuilder.setVersion(resources[4]);
        bundleDescriptorBuilder.setClassifier(resources[5]);
        bundleDescriptorBuilder.setType(resources[6]);
        return bundleDescriptorBuilder;
    }


}
