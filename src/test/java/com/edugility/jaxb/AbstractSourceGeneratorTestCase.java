/* -*- mode: Java; c-basic-offset: 2; indent-tabs-mode: nil -*-
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

import java.io.File;
import java.io.FilenameFilter;

import java.nio.charset.Charset;

import java.lang.management.ManagementFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.tools.DiagnosticCollector;
import javax.tools.StandardJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public abstract class AbstractSourceGeneratorTestCase {

  protected File createScratchDirectory() throws Exception {
    final File tempDirectory = new File(System.getProperty("java.io.tmpdir"));
    assertTrue(tempDirectory.isDirectory());
    assertTrue(tempDirectory.canWrite());
    String pid = ManagementFactory.getRuntimeMXBean().getName();
    assertNotNull(pid);
    final int atIndex = pid.indexOf('@');
    assertTrue(atIndex > 0);
    pid = pid.substring(0, atIndex);
    final File directory = new File(tempDirectory, String.format("%s-%s", this.getClass().getSimpleName(), pid));
    if (!directory.exists()) {
      assertTrue(directory.mkdir());
    }
    assertTrue(directory.isDirectory());
    assertTrue(directory.canWrite());
    directory.deleteOnExit();
    return directory;
  }

  protected abstract File getDirectory();

  protected final void compile(final File... directories) throws Exception {
    assertNotNull(directories);
    assertTrue(directories.length > 0);
    final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull(compiler);
    final StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, Locale.getDefault(), Charset.forName("UTF8"));
    assertNotNull(fileManager);

    final List<String> options = new ArrayList<String>();
    options.add("-classpath");

    final StringBuilder classpath = new StringBuilder(System.getProperty("java.class.path"));
    classpath.append(File.pathSeparator);

    final List<File> files = new ArrayList<File>();

    for (final File directory : directories) {
      assertNotNull(directory);
      assertTrue(directory.isDirectory());
      assertTrue(directory.canWrite());
      assertTrue(directory.canRead());
      classpath.append(directory.getAbsolutePath());
      classpath.append(File.pathSeparator);

      String[] filenames = directory.list(new FilenameFilter() {
          @Override
          public final boolean accept(final File dir, final String name) {
            return name != null && name.endsWith(".java");
          }
        });
      assertNotNull(filenames);

      for (final String filename : filenames) {
        assertNotNull(filename);
        final File file = new File(directory, filename);
        assertTrue(file.isFile());
        assertTrue(file.canRead());
        files.add(file);
      }
    }

    options.add(classpath.toString());

    options.add("-d");
    options.add(this.getDirectory().getAbsolutePath());

    final CompilationTask task = compiler.getTask(null, fileManager, null, options, null, fileManager.getJavaFileObjects(files.toArray(new File[files.size()])));
    assertNotNull(task);

    try {
      assertTrue(task.call());
    } finally {
      fileManager.close();
    }
  }

  protected final void deleteAllScratchFiles() throws Exception {
    final File directory = this.getDirectory();
    deleteAllScratchFiles(directory);
  }

  private static final void deleteAllScratchFiles(final File directory) {
    assertNotNull(directory);
    assertTrue(directory.isDirectory());
    assertTrue(directory.canWrite());
    
    String[] filenames = directory.list();
    assertNotNull(filenames);
    
    for (final String filename : filenames) {
      assertNotNull(filename);
      final File f = new File(directory, filename);
      if (f.isDirectory()) {
        deleteAllScratchFiles(f);
      }
      assertTrue(String.format("Could not delete %s", f), f.delete());
    }
  }

  @After
  public void tearDown() throws Exception {
    if (!Boolean.TRUE.equals(Boolean.getBoolean("keep"))) {
      deleteAllScratchFiles();
      
      final File directory = this.getDirectory();
      assertNotNull(directory);
      assertTrue(directory.isDirectory());
      assertTrue(directory.canWrite());
      
      assertTrue(directory.delete());
      assertFalse(directory.exists());
    }
  }
  

}