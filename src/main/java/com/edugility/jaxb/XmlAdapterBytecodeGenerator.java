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

import java.io.IOException;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;
import javassist.CannotCompileException;

import javassist.bytecode.BadBytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.SignatureAttribute;
import javassist.bytecode.SignatureAttribute.ClassSignature;

public class XmlAdapterBytecodeGenerator {

  private String adapterClassNameTemplate;

  public XmlAdapterBytecodeGenerator() {
    super();
    this.setAdapterClassNameTemplate("%s.%sTo%sAdapter");
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

  public final String getAdapterClassName(final String packageName, final String interfaceName, final String className) {
    if (packageName == null) {
      throw new IllegalArgumentException("packageName", new NullPointerException("packageName"));
    }
    if (interfaceName == null) {
      throw new IllegalArgumentException("interfaceName", new NullPointerException("interfaceName"));
    }
    if (className == null) {
      throw new IllegalArgumentException("className", new NullPointerException("className"));
    }
    final String template = this.getAdapterClassNameTemplate();
    if (template == null) {
      throw new IllegalStateException("The adapterClassNameTemplate property was null");
    }
    return String.format(template, packageName, this.getSimpleName(interfaceName), this.getSimpleName(className));
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

  public <I, C extends I> byte[] generate(final String adapterClassName, final Class<I> interfaceClass, final Class<C> implementationClass) throws BadBytecode, CannotCompileException, IOException, NotFoundException {
    if (adapterClassName == null) {
      throw new IllegalArgumentException("adapterClassName", new NullPointerException("adapterClassName"));
    }
    if (interfaceClass == null) {
      throw new IllegalArgumentException("interfaceClass", new NullPointerException("interfaceClass"));
    }
    if (implementationClass == null) {
      throw new IllegalArgumentException("implementationClass", new NullPointerException("implementationClass"));
    }
    return this.generate(adapterClassName, interfaceClass.getName(), implementationClass.getName());
  }

  public byte[] generate(final String adapterClassName, final String interfaceClassName, final String implementationClassName) throws BadBytecode, CannotCompileException, IOException, NotFoundException {
    if (adapterClassName == null) {
      throw new IllegalArgumentException("adapterClassName", new NullPointerException("adapterClassName"));
    }
    if (interfaceClassName == null) {
      throw new IllegalArgumentException("interfaceClassName", new NullPointerException("interfaceClassName"));
    }
    if (implementationClassName == null) {
      throw new IllegalArgumentException("implementationClassName", new NullPointerException("implementationClassName"));
    }
    
    ClassPool classPool = this.getClassPool(adapterClassName);
    if (classPool == null) {
      classPool = ClassPool.getDefault();
    }
    assert classPool != null;

    final CtClass universalXmlAdapterCtClass = classPool.get(UniversalXmlAdapter.class.getName());
    assert universalXmlAdapterCtClass != null;

    final CtClass adapterCtClass = classPool.makeClass(adapterClassName, universalXmlAdapterCtClass);
    assert adapterCtClass != null;
    final ClassFile adapterClassFile = adapterCtClass.getClassFile();
    assert adapterClassFile != null;

    assert adapterClassFile.getAttribute(SignatureAttribute.tag) == null; // we just created it after all

    final SignatureAttribute adapterClassSignatureAttribute = new SignatureAttribute(adapterClassFile.getConstPool(), String.format("L%s<L%s;L%s;>;", UniversalXmlAdapter.class.getName().replace('.', '/'), interfaceClassName.replace('.', '/'), implementationClassName.replace('.', '/')));
    adapterClassFile.addAttribute(adapterClassSignatureAttribute);

    assert adapterClassFile.getAttribute(SignatureAttribute.tag) != null;
    
    return adapterCtClass.toBytecode();

  }

  protected ClassPool getClassPool(final String className) {
    return ClassPool.getDefault();
  }

}