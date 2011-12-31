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

import java.io.*;

import java.lang.annotation.Annotation;

import java.net.URL;
import java.net.URLClassLoader;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestCasePackageInfoModifier extends ClassLoader {

  public TestCasePackageInfoModifier() {
    super();
  }

  @Test
  public void testModification() throws Exception {
    final PackageInfoModifier modifier = new PackageInfoModifier();
    modifier.setPackage("com.edugility.jaxb");
    final Map<String, String> bindings = new HashMap<String, String>();
    bindings.put(Person.class.getName(), AnyTypeAdapter.class.getName());
    modifier.setBindings(bindings);

    final byte[] newClass = modifier.modify();
    assertNotNull(newClass);
    assertTrue(newClass.length > 0);

    final Class<?> c = this.defineClass("com.edugility.jaxb.package-info", newClass, 0, newClass.length);
    assertNotNull(c);
    this.resolveClass(c);

    final Annotation[] annotations = c.getAnnotations();
    assertNotNull(annotations);
    assertTrue(annotations.length == 2);

    Annotation a = annotations[0];
    assertNotNull(a);
    if (a instanceof Deprecated) {
      a = annotations[1];
      assertNotNull(a);
    }
    assertTrue(a instanceof XmlJavaTypeAdapters);
    final XmlJavaTypeAdapters adaptersAnnotation = (XmlJavaTypeAdapters)a;
    final XmlJavaTypeAdapter[] adapters = adaptersAnnotation.value();
    assertNotNull(adapters);
    assertEquals(1, adapters.length);
    final XmlJavaTypeAdapter adapter = adapters[0];
    assertNotNull(adapter);
    assertEquals(Person.class, adapter.type());
    assertEquals(AnyTypeAdapter.class, adapter.value());

  }

}