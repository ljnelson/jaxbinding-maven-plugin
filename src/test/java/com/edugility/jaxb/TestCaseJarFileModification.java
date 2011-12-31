/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil -*-
 *
 * $Id$
 *
 * Copyright (c) 2010-2011 Edugility LLC.
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT.  IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 * The original copy of this license is available at
 * http://www.opensource.org/license/mit-license.html.
 */
package com.edugility.jaxb;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.util.Enumeration;

import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestCaseJarFileModification {

  public File getSourceJarFile() throws IOException, URISyntaxException {
    final URL url = this.getClass().getResource("/source.jar");
    assertNotNull(url);
    final URI uri = url.toURI();
    assertNotNull(uri);
    final String path = uri.getPath();
    assertNotNull(path);
    return new File(path);
  }

  public File getTargetDirectory() throws IOException {
    final File targetDirectory = new File(System.getProperty("maven.project.build.directory", System.getProperty("project.build.directory", "target")));
    assertTrue(targetDirectory.isDirectory());
    assertTrue(targetDirectory.canWrite());
    return targetDirectory;
  }

  @Test
  public void testDeletion() throws IOException, URISyntaxException {
    final File file = this.getSourceJarFile();
    assertNotNull(file);
    assertTrue(file.isFile());
    assertTrue(file.canRead());
    JarFile sourceFile = null;
    JarOutputStream out = null;
    BufferedOutputStream bufferedOut = null;
    try {
      sourceFile = new JarFile(file);
      final Enumeration<JarEntry> entries = sourceFile.entries();
      if (entries != null && entries.hasMoreElements()) {
        out = new JarOutputStream(new FileOutputStream(new File(this.getTargetDirectory(), "target.jar")));
        while (entries.hasMoreElements()) {
          final JarEntry entry = entries.nextElement();
          if (entry != null) {
            final String entryName = entry.getName();
            assert entryName != null;
            if (!entryName.equalsIgnoreCase("a.txt")) {
              final InputStream is = sourceFile.getInputStream(entry);
              out.putNextEntry(new JarEntry(entryName));
              
              final byte[] buffer = new byte[4096];
              int bytesRead = 0;
              while ((bytesRead = is.read(buffer)) >= 0) {
                out.write(buffer, 0, bytesRead);
              }
              is.close();
              out.flush();
              out.closeEntry();
            }
          }
        }
      }
    } finally {
      if (out != null) {
        out.flush();
        out.close();
      }
      if (sourceFile != null) {
        sourceFile.close();
      }
    }
  }

}