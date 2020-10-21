/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.apikit.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mule.maven.client.api.model.BundleDependency;

import java.net.URI;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class ZipUtils {

    public static final String EXCHANGE = "exchange.json";
    public static final String MAIN = "main";
    public static final String FILE_PREFFIX = "jar:file:";
    public static final String FILE_SEPARATOR = "!/";

    public static URI extractMainFileUri(BundleDependency bundleDependency) {
        try {
            Map<String, String> map;
            ObjectMapper mapper = new ObjectMapper();
            JarFile jarFile = new JarFile(bundleDependency.getBundleUri().getPath());
            ZipEntry exchangeJsonEntry = jarFile.getEntry(EXCHANGE);
            map = mapper.readValue(jarFile.getInputStream(exchangeJsonEntry), Map.class);
            String main = map.get(MAIN);
            String uriPath = FILE_PREFFIX + jarFile.getName() + FILE_SEPARATOR + main;
            return new URI(uriPath);
        } catch (Exception e) {
            return null;
        }
    }
}
