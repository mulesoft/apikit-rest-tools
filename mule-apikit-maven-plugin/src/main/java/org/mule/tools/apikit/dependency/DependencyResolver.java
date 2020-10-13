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

import java.io.File;
import java.util.Optional;

public class DependencyResolver {

    public static final String FAT_RAML = "fat-raml";
    public static final String ZIP = "zip";

    public static String resolve(String groupId, String artifact, String version) throws MojoExecutionException {
        MavenConfiguration.MavenConfigurationBuilder mavenConfigurationBuilder = MavenConfiguration.newMavenConfigurationBuilder();
        AetherMavenClientProvider aetherMavenClientProvider = new AetherMavenClientProvider();
        Optional<File> settingsFile = aetherMavenClientProvider.getSettingsSupplierFactory().environmentUserSettingsSupplier();
        if (settingsFile.isPresent()) {
            mavenConfigurationBuilder.globalSettingsLocation(settingsFile.get());
        } else {
            throw new MojoExecutionException("it is mandatory to have a settings.xml configured");
        }
        mavenConfigurationBuilder.localMavenRepositoryLocation(aetherMavenClientProvider.getLocalRepositorySuppliers().environmentMavenRepositorySupplier().get());
        mavenConfigurationBuilder.forcePolicyUpdateAlways(true);
        MavenClient mavenClient = aetherMavenClientProvider.createMavenClient(mavenConfigurationBuilder.build());
        BundleDescriptor.Builder bundleDescriptorBuilder = new BundleDescriptor.Builder();
        bundleDescriptorBuilder.setGroupId(groupId);
        bundleDescriptorBuilder.setArtifactId(artifact);
        bundleDescriptorBuilder.setVersion(version);
        bundleDescriptorBuilder.setClassifier(FAT_RAML);
        bundleDescriptorBuilder.setType(ZIP);
        BundleDependency bundleDependency = mavenClient.resolveBundleDescriptor(bundleDescriptorBuilder.build());
        return bundleDependency.getBundleUri().getPath();
    }

}
