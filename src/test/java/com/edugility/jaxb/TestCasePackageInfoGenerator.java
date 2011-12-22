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

import java.net.URL;
import java.net.URLClassLoader;

import java.lang.management.ManagementFactory;

import java.util.Locale;

import javax.tools.DiagnosticCollector;
import javax.tools.StandardJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestCasePackageInfoGenerator extends AbstractSourceGeneratorTestCase {

  private PackageInfoGenerator generator;

  @Before
  public void setUp() throws Exception {
    final File adapterDirectory = this.createScratchDirectory();

    final File packageDirectoryRoot = new File(adapterDirectory, "com/edugility/jaxb");
    if (!packageDirectoryRoot.exists()) {
      assertTrue(packageDirectoryRoot.mkdirs());
    }
    packageDirectoryRoot.deleteOnExit();

    this.generator = new PackageInfoGenerator("com.edugility.jaxb", packageDirectoryRoot);

    final XmlAdapterGenerator xmlAdapterGenerator = new XmlAdapterGenerator();
    xmlAdapterGenerator.setDirectory(adapterDirectory);
    this.generator.setXmlAdapterGenerator(xmlAdapterGenerator);

    final URL myLocation = this.getClass().getProtectionDomain().getCodeSource().getLocation();
    assertNotNull(myLocation);

    this.generator.addURL(myLocation);

  }

  @Override
  public File getDirectory() {
    File directory = null;
    if (this.generator != null) {
      final XmlAdapterGenerator xmlAdapterGenerator = this.generator.getXmlAdapterGenerator();
      if (xmlAdapterGenerator != null) {
        directory = xmlAdapterGenerator.getDirectory();
      }
    }
    return directory;
  }

  @Test
  public void testAll() throws Exception {
    this.generator.generate();
    this.compile(this.generator.getPackageDirectoryRoot(), this.getDirectory());
    final URLClassLoader urlClassLoader = new URLClassLoader(new URL[] { this.getDirectory().toURI().toURL() });
    final Class<?> packageInfoClass = urlClassLoader.loadClass("com.edugility.jaxb.package-info");
    assertNotNull(packageInfoClass);
    final XmlJavaTypeAdapters xmlJavaTypeAdaptersAnnotation = packageInfoClass.getAnnotation(XmlJavaTypeAdapters.class);
    assertNotNull(xmlJavaTypeAdaptersAnnotation);
    final XmlJavaTypeAdapter[] individualAdapters = xmlJavaTypeAdaptersAnnotation.value();
    assertNotNull(individualAdapters);
    assertEquals(1, individualAdapters.length);
    final XmlJavaTypeAdapter adapterAnnotation = individualAdapters[0];
    assertNotNull(adapterAnnotation);
    final Class<? extends XmlAdapter> c = adapterAnnotation.value();
    assertNotNull(c);
  }

}