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

import java.io.IOException;
import java.io.Serializable;

import java.net.URI;
import java.net.URL;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.ClassFile;

import org.scannotation.AnnotationDB;

public class JAXBElementScanner implements Serializable {

  private static final long serialVersionUID = 1L;

  private XmlAdapterGenerator generator;

  private Set<URI> uris;

  private Set<String> ignoredPackages;

  private BindingFilter bindingFilter;

  public JAXBElementScanner() {
    super();
  }

  public Set<String> getIgnoredPackages() {
    return this.ignoredPackages;
  }

  public void setIgnoredPackages(final Set<String> ignoredPackages) {
    this.ignoredPackages = ignoredPackages;
  }

  public Map<String, String> scan() throws IOException {
    final SortedMap<String, String> bindings = new TreeMap<String, String>();

    final Set<URI> uris = this.getURIs();
    if (uris == null || uris.isEmpty()) {
      // Nothing to do!
      return bindings;
    }

    final Set<String> ignoredPackages = this.getIgnoredPackages();

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

            final BindingFilter bindingFilter = getBindingFilter();            

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
                    final String implementationClassName = this.cf.getName();
                    if (bindingFilter == null || bindingFilter.accept(interfaceName, implementationClassName)) {
                      atLeastOneInterfaceProcessed = true;
                      bindings.put(interfaceName, implementationClassName);
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
    if (ignoredPackages != null) {
      db.setIgnoredPackages(ignoredPackages.toArray(new String[ignoredPackages.size()]));
    }

    final URL[] urls = new URL[uris.size()];
    int i = 0;
    for (final URI uri : uris) {
      urls[i++] = uri == null ? null : uri.toURL();
    }

    try {
      db.scanArchives(urls);
    } catch (final IllegalStateException unwrapMe) {
      final Throwable cause = unwrapMe.getCause();
      if (cause instanceof IOException) {
        throw (IOException)cause;
      } else {
        throw unwrapMe;
      }
    }
    return bindings;
  }

  public BindingFilter getBindingFilter() {
    return this.bindingFilter;
  }

  public void setBindingFilter(final BindingFilter bindingFilter) {
    this.bindingFilter = bindingFilter;
  }

  public Set<URI> getURIs() {
    return this.uris;
  }

  public void setURIs(final Set<URI> uris) {
    this.uris = uris;
  }

  public void addURI(final URI uri) {
    if (uri != null) {
      if (this.uris == null) {
        this.uris = new LinkedHashSet<URI>();
      }
      this.uris.add(uri);
    }
  }

  public XmlAdapterGenerator getXmlAdapterGenerator() {
    return this.generator;
  }

  public void setXmlAdapterGenerator(final XmlAdapterGenerator generator) {
    this.generator = generator;
  }

  public interface BindingFilter {
    
    public boolean accept(final String interfaceName, final String implementationClassName);

  }

  public static class BlacklistRegexBindingFilter implements BindingFilter, Serializable {
    
    private static final long serialVersionUID = 1L;

    private final Pattern regex;

    public BlacklistRegexBindingFilter(final String regex) {
      this(Pattern.compile(regex));
    }

    public BlacklistRegexBindingFilter(final Pattern regex) {
      super();
      this.regex = regex;
    }

    @Override
    public boolean accept(final String interfaceName, final String implementationClassName) {
      boolean result = interfaceName != null && implementationClassName != null;
      if (result) {
        if (regex == null) {
          result = true;
        } else {
          final Matcher matcher = this.regex.matcher(interfaceName);
          assert matcher != null;
          result = !matcher.find();
        }
      }
      return result;
    }

  }

  public static class WhitelistRegexBindingFilter implements BindingFilter, Serializable {
    
    private static final long serialVersionUID = 1L;

    private final Pattern regex;

    public WhitelistRegexBindingFilter(final String regex) {
      this(Pattern.compile(regex));
    }

    public WhitelistRegexBindingFilter(final Pattern regex) {
      super();
      this.regex = regex;
    }

    /**
     * Returns {@code true} if the supplied {@code interfaceName}
     * matches the regular expression supplied to this {@link
     * WhitelistRegexBindingFilter} at construction time; {@code
     * false} otherwise.
     */
    @Override
    public boolean accept(final String interfaceName, final String implementationClassName) {
      boolean result = interfaceName != null && implementationClassName != null && this.regex != null;
      if (result) {
        final Matcher matcher = this.regex.matcher(interfaceName);
        assert matcher != null;
        result = matcher.find();
      }
      return result;
    }

  }

}