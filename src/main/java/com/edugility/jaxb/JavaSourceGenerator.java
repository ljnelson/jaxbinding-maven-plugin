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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.URL;

import org.mvel2.templates.CompiledTemplate;
import org.mvel2.templates.TemplateCompiler;

@Deprecated
public abstract class JavaSourceGenerator {

  private static final String LS = System.getProperty("line.separator", "\n");

  private String encoding;

  private String templateResourceName;
  
  private String template;

  private transient CompiledTemplate compiledTemplate;

  protected JavaSourceGenerator() {
    super();
    this.setEncoding("UTF8");
  }

  public String getEncoding() {
    return this.encoding;
  }

  public void setEncoding(final String encoding) {
    this.encoding = encoding;
  }

  public String getTemplateResourceName() {
    return this.templateResourceName;
  }

  public void setTemplateResourceName(final String resourceName) {
    this.templateResourceName = resourceName;
  }

  protected URL getTemplateResource() {
    final String name = this.getTemplateResourceName();
    if (name == null) {
      throw new IllegalStateException("You must set a template resource name");
    }
    final ClassLoader[] loaders = new ClassLoader[] { Thread.currentThread().getContextClassLoader(), this.getClass().getClassLoader() };
    URL templateURL = null;
    for (final ClassLoader loader : loaders) {
      if (templateURL == null && loader != null) {
        templateURL = loader.getResource(name);
        if (templateURL != null) {
          break;
        }
      }
    }
    if (templateURL == null) {
      templateURL = ClassLoader.getSystemResource(name);
    }
    return templateURL;
  }

  public String getTemplate() throws IOException {
    if (this.template == null) {
      final URL templateURL = this.getTemplateResource();
      if (templateURL == null) {
        throw new IllegalStateException(String.format("Could not find a template resource.  The template resource name was %s", this.getTemplateResourceName()));
      }

      BufferedReader reader = null;
      InputStream stream = null;
      try {
        stream = templateURL.openStream();
        if (stream != null) {
          reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
          String line = null;
          final StringBuilder sb = new StringBuilder();
          while ((line = reader.readLine()) != null) {
            sb.append(line);
            sb.append(LS);
          }
          this.template = sb.toString();
        }
      } finally {
        if (stream != null) {
          try {
            stream.close();
          } catch (final IOException nothingWeCanDo) {
            
          }
        }
        if (reader != null) {
          try {
            reader.close();
          } catch (final IOException nothingWeCanDo) {
            
          }
        }
      }
    }
    return this.template;
  }

  public void setTemplate(String template) {
    this.template = template;
    this.compiledTemplate = null;
    if (template != null) {
      this.compiledTemplate = TemplateCompiler.compileTemplate(template);
    }
  }

  protected CompiledTemplate getCompiledTemplate() throws IOException {
    if (this.compiledTemplate == null) {
      final String template = this.getTemplate();
      if (template != null) {
        this.compiledTemplate = TemplateCompiler.compileTemplate(template);
      }
    }
    return this.compiledTemplate;
  }

  protected static final String getSimpleName(final String name) {
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

  protected static final String getPackage(final String name) {
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



}
