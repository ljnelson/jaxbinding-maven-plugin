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
import java.io.Serializable;

import java.net.URL;

import java.nio.charset.Charset;

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

/**
 * @deprecated Use the {@link PackageInfoModifier} class instead.
 */
@Deprecated
public class PackageInfoGenerator extends JavaSourceGenerator implements Serializable {

  private static final long serialVersionUID = 1L;

  private File packageDirectoryRoot;

  private XmlAdapterGenerator generator;

  private final List<URL> urls;

  private final String interfacePackage;

  private Set<String> packagesToIgnore;

  public PackageInfoGenerator(final String interfacePackage, final File packageDirectoryRoot) {
    super();
    this.interfacePackage = interfacePackage;
    if (interfacePackage == null) {
      throw new IllegalArgumentException("interfacePackage", new NullPointerException("interfacePackage"));
    }
    this.setDirectory(packageDirectoryRoot);
    this.setTemplateResourceName("package-info.mvel");
    this.urls = new ArrayList<URL>(11);
    this.setEncoding("UTF8");
  }

  public Set<String> getPackagesToIgnore() {
    return this.packagesToIgnore;
  }

  public void setPackagesToIgnore(final Set<String> packagesToIgnore) {
    this.packagesToIgnore = packagesToIgnore;
  }

  public void generate() throws IOException {
    final SortedMap<String, String> adapterInfo = new TreeMap<String, String>();

    final AnnotationDB db = new AnnotationDB() {
        
        private static final long serialVersionUID = 1L;

        private ClassFile cf;

        /**
         * Overrides the superclass' implementation to track the
         * {@link ClassFile} being scanned.
         */
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

        /**
         * Overrides the superclass' implementation to track the
         * {@link ClassFile} being scanned.
         */
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

        /**
         * Overrides the superclass' implementation to track the
         * {@link ClassFile} being scanned.
         */
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

        /**
         * Overrides the superclass' implementation to track the
         * {@link ClassFile} being scanned.
         */
        @Override
        protected final void populate(final Annotation[] annotations, final String className) {
          // All scannotation activity passes through here.
          if (annotations != null && className != null && annotations.length > 0 && this.cf != null) {
            assert className.equals(this.cf.getName());
            assert !this.cf.isInterface();

            final String interfacePackage = getInterfacePackage();
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
                      adapterInfo.put(interfaceName, generator.getAdapterClassName(interfacePackage, interfaceName, this.cf.getName()));
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
    db.setScanParameterAnnotations(false);
    final Set<String> packagesToIgnore = this.getPackagesToIgnore();
    if (packagesToIgnore != null) {
      db.setIgnoredPackages(packagesToIgnore.toArray(new String[packagesToIgnore.size()]));
    }

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

  public final File getPackageDirectoryRoot() {
    return this.packageDirectoryRoot;
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
    this.packageDirectoryRoot = directory;
  }

  public final String getInterfacePackage() {
    return this.interfacePackage;
  }

  private final void generatePackageInfo(final Map<String, String> entries) throws IOException {
    final CompiledTemplate compiledTemplate = this.getCompiledTemplate();
    assert compiledTemplate != null;

    final Map<Object, Object> parameters = new HashMap<Object, Object>(17);
    parameters.put("bindings", entries);
    parameters.put("interfacePackageName", this.interfacePackage);

    final String source = (String)TemplateRuntime.execute(compiledTemplate, parameters);
    assert source != null;

    final File directory = this.getPackageDirectoryRoot();
    assert directory != null;

    final File sourceFile = new File(directory, "package-info.java");

    FileOutputStream fos = new FileOutputStream(sourceFile);
    String encoding = this.getEncoding();
    final OutputStreamWriter ows;
    if (encoding == null) {
      ows = new OutputStreamWriter(fos, Charset.defaultCharset());
    } else {
      ows = new OutputStreamWriter(fos, Charset.forName(encoding));
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
      returnValue = this.urls.remove(url);
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
    if (interfaceName == null) {
      throw new IllegalArgumentException("interfaceName", new NullPointerException("interfaceName"));
    }
    if (cf == null) {
      throw new IllegalArgumentException("cf", new NullPointerException("cf"));
    }

    XmlAdapterGenerator xmlAdapterGenerator = this.getXmlAdapterGenerator();
    if (xmlAdapterGenerator == null) {
      throw new IllegalStateException("Please install an XmlAdapterGenerator");
    }

    xmlAdapterGenerator.generate(getPackage(interfaceName), interfaceName, cf.getName());

    return xmlAdapterGenerator;
  }

  public XmlAdapterGenerator getXmlAdapterGenerator() {
    return this.generator;
  }

  public void setXmlAdapterGenerator(final XmlAdapterGenerator generator) {
    this.generator = generator;
  }

}