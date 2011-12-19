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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.BufferedWriter;
import java.io.IOException;

import java.net.URL;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.ClassFile;

import org.scannotation.AnnotationDB;

import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateRuntime;

public class PackageInfoGenerator extends JavaSourceGenerator {

    /*

      Using scannotation, look for all classes that have some JAXB
      annotations on them.

      Extract the interfaces they implement that are from designated
      packages.

      Use an XMLAdapterGenerator to generate XMLAdapters of the proper
      types in the designated directory.
      
    */

  private File adapterDirectory;

  private File directory;

  private String license;

  private String encoding;

  private String sourceFilenameTemplate;

  private final List<URL> urls;

  private final String interfacePackage;

  public PackageInfoGenerator(final String interfacePackage, final File packageDirectory) {
    super();
    this.interfacePackage = interfacePackage;
    if (interfacePackage == null) {
      throw new IllegalArgumentException("interfacePackage", new NullPointerException("interfacePackage"));
    }
    this.setDirectory(packageDirectory);
    this.setTemplateResourceName("package-info.mvel");
    this.urls = new ArrayList<URL>(11);
    this.setSourceFilenameTemplate("%s.java");
    this.setEncoding("UTF8");
  }

  public String getEncoding() {
    return this.encoding;
  }

  public void setEncoding(final String encoding) {
    this.encoding = encoding;
  }

  public String getSourceFilenameTemplate() {
    return this.sourceFilenameTemplate;
  }

  public void setSourceFilenameTemplate(final String template) {
    if (template == null) {
      throw new IllegalArgumentException("template", new NullPointerException("template"));
    }
    this.sourceFilenameTemplate = template;
  }

  public String getLicense() {
    return this.license;
  }

  public void setLicense(final String license) {
    this.license = license;
  }

  public File getAdapterDirectory() {
    return this.adapterDirectory;
  }

  public void setAdapterDirectory(final File directory) {
    this.adapterDirectory = directory;
  }

  public void processJAXBAnnotatedClasses() throws IOException {
    final SortedMap<String, String> adapterInfo = new TreeMap<String, String>();
    final AnnotationDB db = new AnnotationDB() {
        
        private static final long serialVersionUID = 1L;

        private ClassFile cf;

        @Override
        protected final void scanClass(final ClassFile cf) {
          // Overrides this method to keep track of the ClassFile being scanned.
          if (cf == null || !cf.isInterface()) {
            this.cf = cf;
          } else {
            this.cf = null;
          }
          super.scanClass(cf);
          this.cf = null;
        }

        @Override
        protected final void scanMethods(final ClassFile cf) {
          // Overrides this method to keep track of the ClassFile being scanned.
          if (cf == null || !cf.isInterface()) {
            this.cf = cf;
          } else {
            this.cf = null;
          }
          super.scanMethods(cf);
          this.cf = null;
        }

        @Override
        protected final void scanFields(final ClassFile cf) {
          // Overrides this method to keep track of the ClassFile being scanned.
          if (cf == null || !cf.isInterface()) {
            this.cf = cf;
          } else {
            this.cf = null;
          }
          super.scanFields(cf);
          this.cf = null;
        }

        @Override
        protected final void populate(final Annotation[] annotations, final String className) {
          // All scannotation activity passes through here.
          if (annotations != null && className != null && annotations.length > 0 && this.cf != null) {
            assert className.equals(this.cf.getName());
            assert !this.cf.isInterface();

            final String interfacePackage = getInterfacePackageName();
            assert interfacePackage != null;

            for (final Annotation a : annotations) {
              assert a != null;
              final String typeName = a.getTypeName();
              assert typeName != null;
              if (typeName.startsWith("javax.xml.bind.annotation.")) {
                // OK, we have a class with JAXB annotations on it.
                // Get its interfaces efficiently.
                boolean atLeastOneInterfaceProcessed = false;
                final String[] interfaces = this.cf.getInterfaces();
                if (interfaces != null && interfaces.length > 0) {
                  for (final String interfaceName : interfaces) {
                    assert interfaceName != null;
                    if (interfaceName.startsWith(interfacePackage)) {
                      atLeastOneInterfaceProcessed = true;
                      XmlAdapterGenerator generator = null;
                      try {
                        generator = generateXMLAdapter(interfaceName, this.cf);
                      } catch (final IOException wrapMe) {
                        throw new IllegalStateException(wrapMe);
                      }
                      adapterInfo.put(interfaceName, generator.getAdapterClassName());
                    }
                  }
                }
                if (atLeastOneInterfaceProcessed) {
                  break;
                }
              }
            }

          }
        }
      };
    try {
      db.scanArchives(this.getURLs());
    } catch (final IllegalStateException unwrapMe) {
      final Throwable cause = unwrapMe.getCause();
      if (cause instanceof IOException) {
        throw (IOException)cause;
      } else {
        throw unwrapMe;
      }
    }

    this.generatePackageInfo(adapterInfo);
  }

  public final File getDirectory() {
    return this.directory;
  }
  
  public final void setDirectory(final File directory) {
    if (directory == null) {
      throw new IllegalArgumentException("directory", new NullPointerException("directory"));
    }
    if (!directory.exists()) {
      throw new IllegalArgumentException("directory", new FileNotFoundException(String.format("Directory %s does not exist", directory)));
    }
    if (!directory.isDirectory()) {
      throw new IllegalArgumentException(String.format("%s is not a directory", directory));
    }
    if (!directory.canWrite()) {
      throw new IllegalArgumentException(String.format("Cannot write to directory %s", directory));
    }
    this.directory = directory;
  }

  public final String getInterfacePackageName() {
    return this.interfacePackage;
  }

  public void generatePackageInfo(final Map<String, String> entries) throws IOException {
    final CompiledTemplate compiledTemplate = this.getCompiledTemplate();
    assert compiledTemplate != null;

    final Map<Object, Object> parameters = new HashMap<Object, Object>(17);
    parameters.put("bindings", entries);
    parameters.put("interfacePackageName", this.interfacePackage);

    final String source = (String)TemplateRuntime.execute(compiledTemplate, parameters);
    assert source != null;

    final File directory = this.getDirectory();
    assert directory != null;

    final File sourceFile = new File(directory, "package-info.java");

    FileOutputStream fos = new FileOutputStream(sourceFile);
    String encoding = this.getEncoding();
    final OutputStreamWriter ows;
    if (encoding == null) {
      ows = new OutputStreamWriter(fos);
    } else {
      ows = new OutputStreamWriter(fos, encoding);
    }
    final BufferedWriter bw = new BufferedWriter(ows);
    try {
      bw.write(source, 0, source.length());
      bw.flush();
    } finally {
      if (fos != null) {
        fos.close();
      }
      if (ows != null) {
        ows.close();
      }
      if (bw != null) {
        bw.close();
      }
    }

  }

  public URL[] getURLs() {
    return this.urls.toArray(new URL[this.urls.size()]);
  }

  public void setURLs(final URL[] urls) {
    this.urls.clear();
    if (urls != null && urls.length > 0) {
      this.urls.addAll(Arrays.asList(urls));
    }
  }

  public boolean addURL(final URL url) {
    boolean returnValue = false;
    if (url != null) {
      returnValue = this.urls.add(url);
    }
    return returnValue;
  }

  public boolean removeURL(final URL url) {
    boolean returnValue = false;
    if (url != null && !this.urls.isEmpty()) {
      returnValue = this.urls.remove(returnValue);
    }
    return returnValue;
  }

  public boolean containsURL(final URL url) {
    boolean returnValue = false;
    if (url != null) {
      returnValue = this.urls.contains(url);
    }
    return returnValue;
  }

  private final XmlAdapterGenerator generateXMLAdapter(final String interfaceName, final ClassFile cf) throws IOException {
    XmlAdapterGenerator xmlAdapterGenerator = null;
    if (interfaceName != null && cf != null) {
      xmlAdapterGenerator = this.createXmlAdapterGenerator(interfaceName, cf.getName());
      if (xmlAdapterGenerator == null) {
        xmlAdapterGenerator = new XmlAdapterGenerator(interfaceName, cf.getName(), this.getAdapterDirectory());
      }
      assert xmlAdapterGenerator != null;
      xmlAdapterGenerator.setDirectory(this.getAdapterDirectory());
      xmlAdapterGenerator.setLicense(this.getLicense());
      xmlAdapterGenerator.setEncoding(this.getEncoding());
      xmlAdapterGenerator.setSourceFilenameTemplate(this.getSourceFilenameTemplate());
      xmlAdapterGenerator.generate();
    }
    return xmlAdapterGenerator;
  }

  protected XmlAdapterGenerator createXmlAdapterGenerator(final String interfaceName, final String className) {
    final XmlAdapterGenerator generator = new XmlAdapterGenerator(interfaceName, className, this.getAdapterDirectory());
    if (interfaceName != null) {
      final String packageName;
      final int lastDotIndex = interfaceName.lastIndexOf('.');
      if (lastDotIndex < 0) {
        packageName = "";
      } else {
        packageName = interfaceName.substring(0, lastDotIndex);
      }
      generator.setPackage(packageName);
    }
    return generator;
  }

}