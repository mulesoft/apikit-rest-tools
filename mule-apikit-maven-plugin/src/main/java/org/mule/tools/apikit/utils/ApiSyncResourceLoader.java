package org.mule.tools.apikit.utils;

import org.apache.maven.plugin.MojoExecutionException;
import org.mule.apikit.loader.ResourceLoader;
import org.mule.tools.apikit.dependency.DependencyResolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;

public class ApiSyncResourceLoader implements ResourceLoader {

    @Override
    public InputStream getResourceAsStream(String resourceString) {
        String resource = getResource(resourceString).getPath();
        File initialFile = new File(resource);
        try {
            return new FileInputStream(initialFile);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    public URI getResource(String resourceString) {
        try {
            return DependencyResolver.resolve(resourceString);
        } catch (MojoExecutionException e) {
            return null;
        }
    }
}
