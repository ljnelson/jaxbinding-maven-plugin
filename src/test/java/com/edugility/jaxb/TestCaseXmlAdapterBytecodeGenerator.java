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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestCaseXmlAdapterBytecodeGenerator {

  private XmlAdapterBytecodeGenerator generator;

  public TestCaseXmlAdapterBytecodeGenerator() {
    super();
  }

  @Before
  public void setUp() throws Exception {
    this.generator = new XmlAdapterBytecodeGenerator();
  }

  @Test
  public void testGenerate() throws Exception {
    final byte[] newClass = this.generator.generate("com.edugility.jaxb.PersonToPersonImplementationAdapter", Person.class, PersonImplementation.class);
    assertNotNull(newClass);
    assertTrue(newClass.length > 0);

    final Class<?> adapterClass = new ClassDefiner().define("com.edugility.jaxb.PersonToPersonImplementationAdapter", newClass);
    assertNotNull(adapterClass);
    assertEquals("com.edugility.jaxb.PersonToPersonImplementationAdapter", adapterClass.getName());
    assertTrue(UniversalXmlAdapter.class.isAssignableFrom(adapterClass));
    assertSame(UniversalXmlAdapter.class, adapterClass.getSuperclass());

    final Type genericSuperclass = adapterClass.getGenericSuperclass();
    assertNotNull(genericSuperclass);
    assertTrue(genericSuperclass instanceof ParameterizedType);
    final ParameterizedType adapterSuperType = (ParameterizedType)genericSuperclass;
    assertSame(UniversalXmlAdapter.class, adapterSuperType.getRawType());
    final Type[] actualTypeArguments = adapterSuperType.getActualTypeArguments();
    assertNotNull(actualTypeArguments);
    assertEquals(2, actualTypeArguments.length);
    assertSame(Person.class, actualTypeArguments[0]);
    assertSame(PersonImplementation.class, actualTypeArguments[1]);

    final UniversalXmlAdapter adapter = (UniversalXmlAdapter)adapterClass.newInstance();
    assertNotNull(adapter);
  }

  private static final class ClassDefiner extends ClassLoader {
    
    public final Class<?> define(final String className, final byte[] classBytes) throws Exception {
      final Class<?> c = this.defineClass(className, classBytes, 0, classBytes.length);
      assertNotNull(c);
      this.resolveClass(c);
      return c;
    }

  }

}