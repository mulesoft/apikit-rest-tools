/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.apikit.utils;

import org.apache.commons.io.FilenameUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipUtils {

    public static final String DEFAULT_FILE_NAME = "api.raml";
    public static final String RAML = "raml";
    public static final String DIRECTORY_ERROR_MESSAGE = "Could not create directory: ";

    public static void unzip(File destination, String fileName) throws IOException {
        ZipFile zipFile = new ZipFile(getPath(destination, fileName));
        for (Enumeration entries = zipFile.entries(); entries.hasMoreElements(); ) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            File file = new File(getPath(destination, entry.getName()));
            checkDirectoryCreation(file.getParentFile());
            if (entry.isDirectory()) {
                checkDirectoryCreation(file);
            } else {
                copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(getFileOutputStream(file)));
            }
        }
        zipFile.close();
    }

    private static FileOutputStream getFileOutputStream(File file) throws IOException {
        if (FilenameUtils.getExtension(file.getPath()).equalsIgnoreCase(RAML)) {
            String path = FilenameUtils.getFullPath(file.getPath());
            String newFileName = FilenameUtils.concat(path, DEFAULT_FILE_NAME);
            File renamedFile = new File(newFileName);
            file.renameTo(renamedFile);
            return new FileOutputStream(renamedFile);
        }
        return new FileOutputStream(file);
    }

    private static void checkDirectoryCreation(File file) throws IOException {
        if (!buildDirectory(file)) {
            throw new IOException(DIRECTORY_ERROR_MESSAGE + file);
        }
    }

    public static boolean buildDirectory(File file) {
        return file.exists() || file.mkdirs();
    }

    public static void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len = in.read(buffer);
        while (len >= 0) {
            out.write(buffer, 0, len);
            len = in.read(buffer);
        }
        in.close();
        out.close();
    }

    public static String getPath(File file, String fileName) {
        return FilenameUtils.concat(file.getAbsolutePath(), fileName);
    }

}
