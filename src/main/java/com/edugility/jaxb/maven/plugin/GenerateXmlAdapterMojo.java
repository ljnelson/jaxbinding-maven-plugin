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
package com.edugility.jaxb.maven.plugin;

import java.io.File;
import java.io.IOException;

import java.util.List;

import com.edugility.jaxb.XmlAdapterGenerator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import org.apache.maven.plugin.logging.Log;

/**
 * @goal generate-xml-adapter
 * @requiresDependencyResolution test
 * @phase generate-sources
 */
public class GenerateXmlAdapterMojo extends AbstractJAXBMojo {

  /*
   * @parameter property="xmlAdapterGenerator"
   */
  private XmlAdapterGenerator generator;

  /*
   * @parameter default-value="${project.build.directory}/generated-sources/jaxb" property="directory"
   */
  private File directory;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    final Log log = this.getLog();

    XmlAdapterGenerator generator = this.getXmlAdapterGenerator();
    if (generator == null) {
      generator = new XmlAdapterGenerator();
    }

    File directory = this.getDirectory();
    if (directory == null) {
      directory = generator.getDirectory();
      if (directory == null) {
        throw new MojoExecutionException("No directory set", new IllegalStateException("No directory set"));
      }
    } else if (!directory.exists()) {
      if (!directory.mkdirs()) {
        throw new MojoExecutionException(String.format("Could not create directory path %s", directory), new IOException(String.format("Could not create directory path %s", directory)));
      }
    }
    assert directory.isDirectory();
    assert directory.canWrite();

    
    

  }

  public XmlAdapterGenerator getXmlAdapterGenerator() {
    return this.generator;
  }

  public void setXmlAdapterGenerator(final XmlAdapterGenerator generator) {
    this.generator = generator;
  }

  public File getDirectory() {
    return this.directory;
  }

  public void setDirectory(final File directory) {
    this.directory = directory;
  }

}