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

import java.util.Set;

import com.edugility.jaxb.PackageInfoGenerator;

import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugin.logging.Log;

/**
 * @goal generate-package-info
 *
 * @phase generate-sources
 *
 * @requiresDependencyResolution test
 */
public class GeneratePackageInfoMojo extends AbstractJAXBMojo {

  /**
   * @parameter property="interfacePackage" required
   */
  private String interfacePackage;

  /**
   * @parameter property="packageDirectoryRoot" default-value="${project.build.directory}/generated-sources/jaxb"
   */
  private File packageDirectoryRoot;

  /**
   * @parameter property="packagesToIgnore"
   */
  private Set<String> packagesToIgnore;

  /**
   * @parameter property="packageInfoGenerator"
   */
  private PackageInfoGenerator packageInfoGenerator;

  public GeneratePackageInfoMojo() {
    super();
  }

  @Override
  public void execute() throws MojoExecutionException {

    PackageInfoGenerator generator = this.getPackageInfoGenerator();
    if (generator == null) {
      generator = new PackageInfoGenerator(this.getInterfacePackage(), this.getPackageDirectoryRoot());
    }

    final Set<String> packagesToIgnore = this.getPackagesToIgnore();
    if (packagesToIgnore != null) {
      generator.setPackagesToIgnore(packagesToIgnore);
    }

    

  }

  public String getInterfacePackage() {
    return this.interfacePackage;
  }

  public void setInterfacePackage(final String interfacePackage) {
    if (interfacePackage == null) {
      this.interfacePackage = "";
    } else {
      this.interfacePackage = interfacePackage;
    }
  }
  
  public File getPackageDirectoryRoot() {
    return this.packageDirectoryRoot;
  }

  public void setPackageDirectoryRoot(final File packageDirectoryRoot) {
    if (packageDirectoryRoot == null) {
      throw new IllegalArgumentException("packageDirectoryRoot", new NullPointerException("packageDirectoryRoot"));
    }
    this.packageDirectoryRoot = packageDirectoryRoot;
  }

  public Set<String> getPackagesToIgnore() {
    return this.packagesToIgnore;
  }

  public void setPackagesToIgnore(final Set<String> packagesToIgnore) {
    this.packagesToIgnore = packagesToIgnore;
  }

  public PackageInfoGenerator getPackageInfoGenerator() {
    return this.packageInfoGenerator;
  }

  public void setPackageInfoGenerator(final PackageInfoGenerator generator) {
    this.packageInfoGenerator = generator;
  }

}