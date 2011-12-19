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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;
import org.mvel2.templates.TemplateRuntime;

public class XmlAdapterGenerator extends JavaSourceGenerator {

  private static final String LS = System.getProperty("line.separator", "\n");

  private String adapterClassNameTemplate;

  private String license;

  private String pkg;

  private final String interfaceName;

  private final String className;

  private String sourceFilenameTemplate;

  private String encoding;

  private File directory;

  private String comment;

  private Date generationDate;

  public XmlAdapterGenerator(final String interfaceName, final String className, final File directory) {
    super();
    this.interfaceName = interfaceName;
    this.className = className;
    if (interfaceName == null) {
      throw new IllegalArgumentException("interfaceName", new NullPointerException("interfaceName"));
    }
    if (className == null) {
      throw new IllegalArgumentException("className", new NullPointerException("className"));
    }
    this.setAdapterClassNameTemplate("%s.%sTo%sAdapter");
    this.setSourceFilenameTemplate("%s.java");
    this.setEncoding("UTF8");
    this.setTemplateResourceName(String.format("%s.mvel", XmlAdapterGenerator.class.getSimpleName()));
    this.setDirectory(directory);    
  }

  public Date getGenerationDate() {
    return this.generationDate;
  }

  public void setGenerationDate(final Date date) {
    this.generationDate = date;
  }

  public String getAdapterClassNameTemplate() {
    return this.adapterClassNameTemplate;
  }

  public void setAdapterClassNameTemplate(final String adapterClassNameTemplate) {
    if (adapterClassNameTemplate == null) {
      throw new IllegalArgumentException("adapterClassNameTemplate", new NullPointerException("adapterClassNameTemplate"));
    }
    this.adapterClassNameTemplate = adapterClassNameTemplate;
  }

  public final String getAdapterClassName() {
    final String template = this.getAdapterClassNameTemplate();
    if (template == null) {
      throw new IllegalStateException("The adapterClassNameTemplate property was null");
    }
    return String.format(template, this.getPackage(), this.getSimpleName(this.getInterfaceName()), this.getSimpleName(this.getClassName()));
  }

  public String getClassName() {
    return this.className;
  }

  public String getInterfaceName() {
    return this.interfaceName;
  }

  private final String getSimpleName(final String name) {
    String returnValue = null;
    if (name != null) {
      final int lastDotIndex = name.lastIndexOf('.');
      if (lastDotIndex < 0) {
        returnValue = name;
      } else {
        assert name.length() > lastDotIndex + 1;
        returnValue = name.substring(lastDotIndex + 1);
      }
    }
    return returnValue;
  }

  private final String getPackage(final String name) {
    String returnValue = null;
    if (name != null) {
      final int lastDotIndex = name.lastIndexOf('.');
      if (lastDotIndex < 0) {
        returnValue = "";
      } else {
        returnValue = name.substring(0, lastDotIndex);
      }
    }
    return returnValue;
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

  public String getPackage() {
    return this.pkg;
  }

  public void setPackage(final String pkg) {
    this.pkg = pkg;
  }

  public String getEncoding() {
    return this.encoding;
  }

  public void setEncoding(final String encoding) {
    this.encoding = encoding;
  }

  public String getGenerationComment() {
    return this.comment;
  }

  public void setGenerationComment(final String comment) {
    this.comment = comment;
  }

  public final File getSourceFile() {
    String sft = this.getSourceFilenameTemplate();
    assert sft != null;

    final String adapterClassName = this.getAdapterClassName();
    assert adapterClassName != null;

    final String sourceFileName = String.format(sft, this.getSimpleName(adapterClassName));

    final File sourceFile = new File(directory, sourceFileName);
    return sourceFile;
  }

  public File generate() throws IOException {
    final File directory = this.getDirectory();
    assert directory != null;

    String sourceFilenameTemplate = this.getSourceFilenameTemplate();
    if (sourceFilenameTemplate == null) {
      throw new IllegalStateException("The sourceFilenameTemplate property cannot be null");
    }
    
    final File sourceFile = this.getSourceFile();
    if (sourceFile == null) {
      throw new IllegalStateException("The getSourceFile() method must not return null");
    }

    final String source = this.getXmlAdapterSource();
    assert source != null;

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
    return sourceFile;
  }

  private final String getXmlAdapterSource() {
    final CompiledTemplate compiledTemplate = this.getCompiledTemplate();
    assert compiledTemplate != null;
    
    final Map<Object, Object> parameters = new HashMap<Object, Object>(17);
    parameters.put("license", this.getLicense());
    parameters.put("adapterPackage", this.getPackage());
    final String interfaceName = this.getInterfaceName();
    parameters.put("interfaceName", interfaceName);
    parameters.put("interfacePackage", this.getPackage(interfaceName));
    parameters.put("interfaceSimpleName", this.getSimpleName(interfaceName));

    final String className = this.getClassName();
    parameters.put("className", className);
    parameters.put("classPackage", this.getPackage(className));
    parameters.put("classSimpleName", this.getSimpleName(className));
    
    parameters.put("generator", this.getClass().getName());

    final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    Date generationDate = this.getGenerationDate();
    if (generationDate == null) {
      generationDate = new Date();
    }
    String generationTimestamp = formatter.format(generationDate);
    assert generationTimestamp != null;
    assert generationTimestamp.length() == 24;

    // Convert invalid timezone specifications at the end of the
    // format string from, e.g., 0000 to 00:00
    generationTimestamp = String.format("%s:%s", generationTimestamp.substring(0, 22), generationTimestamp.substring(22));
    parameters.put("generationTimestamp", generationTimestamp);

    parameters.put("generationComment", this.getGenerationComment());

    return (String)TemplateRuntime.execute(compiledTemplate, parameters);
  }

}