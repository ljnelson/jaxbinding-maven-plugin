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

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;

import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationMemberValue;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;

import javassist.CtClass;
import javassist.ClassPool;
import javassist.NotFoundException;
import javassist.CannotCompileException;

public class PackageInfoModifier {

  private Map<String, String> bindings;

  private String pkg;

  public PackageInfoModifier() {
    super();
  }

  public String getPackage() {
    return this.pkg;
  }

  public void setPackage(final String pkg) {
    this.pkg = pkg;
  }

  /**
   * @todo Not sure of the parameter; might be better to return a {@code byte[]}.
   */
  public byte[] modify(final OutputStream stream) throws CannotCompileException, ClassNotFoundException, IOException, NotFoundException {
    byte[] returnValue = null;
    final String pkg = this.getPackage();
    if (pkg != null) {
      final URL packageInfoResource = this.getResource(String.format("%s/package-info.class", pkg.replace(".", "/")));
      if (packageInfoResource != null) {
        final ClassPool classPool = ClassPool.getDefault();
        final CtClass packageInfoCtClass = classPool.get(String.format("%s.package-info", pkg));
        assert packageInfoCtClass != null;
        this.addXmlJavaTypeAdapters(packageInfoCtClass);
        returnValue = packageInfoCtClass.toBytecode();

        assert assertModifiedClassIsOK(packageInfoCtClass);

        if (stream != null) {
          final DataOutputStream dos;
          if (stream instanceof BufferedOutputStream) {
            dos = new DataOutputStream(stream);
          } else if (stream instanceof DataOutputStream) {
            dos = (DataOutputStream)stream;
          } else {
            dos = new DataOutputStream(new BufferedOutputStream(stream));
          }
          packageInfoCtClass.toBytecode(dos);
          dos.flush();
        }
      }
    }
    return returnValue;
  }

  private static final boolean assertModifiedClassIsOK(final CtClass packageInfoCtClass) throws CannotCompileException {
    assert packageInfoCtClass != null;
    final Class<?> c = packageInfoCtClass.toClass();
    assert c != null;
    System.out.println("*** location: " + c.getProtectionDomain().getCodeSource().getLocation());

    final XmlJavaTypeAdapters adapters = c.getAnnotation(XmlJavaTypeAdapters.class);
    assert adapters != null;
    
    final XmlJavaTypeAdapter[] adapterArray = adapters.value();

    return true;
  }

  private final void addXmlJavaTypeAdapters(final CtClass packageInfoClass) throws ClassNotFoundException, NotFoundException {
    if (packageInfoClass == null) {
      throw new IllegalArgumentException("packageInfoClass", new NullPointerException("packageInfoClass"));
    }
    if (!"package-info".equals(packageInfoClass.getSimpleName())) {
      throw new IllegalArgumentException("Wrong CtClass: " + packageInfoClass);
    }

    final ClassPool classPool = ClassPool.getDefault();
    assert classPool != null;
    
    final ClassFile packageInfoClassFile = packageInfoClass.getClassFile();
    assert packageInfoClassFile != null;

    final ConstPool constantPool = packageInfoClassFile.getConstPool();
    assert constantPool != null;

    AnnotationsAttribute annotationsAttribute = (AnnotationsAttribute)packageInfoClassFile.getAttribute(AnnotationsAttribute.visibleTag);
    assert annotationsAttribute != null;

    Annotation adaptersAnnotation = annotationsAttribute.getAnnotation(XmlJavaTypeAdapters.class.getName());
    if (adaptersAnnotation == null) {
      final CtClass xmlJavaTypeAdaptersCtClass = classPool.get(XmlJavaTypeAdapters.class.getName());
      assert xmlJavaTypeAdaptersCtClass != null;
      adaptersAnnotation = new Annotation(constantPool, xmlJavaTypeAdaptersCtClass);
      annotationsAttribute.addAnnotation(adaptersAnnotation);
    } else if (adaptersAnnotation.getMemberValue("value") == null) {
      final ArrayMemberValue amv = new ArrayMemberValue(constantPool);
      amv.setValue(new AnnotationMemberValue[0]);
      adaptersAnnotation.addMemberValue("value", amv);
    }
    assert adaptersAnnotation != null;
    assert adaptersAnnotation.getMemberValue("value") != null;

    /*
     * You would think this line would be required ONLY in the case
     * where the annotation itself was not found.  But you actually
     * have to add it to its containing AnnotationsAttribute in ALL
     * cases.  This doesn't make any sense.  See
     * http://stackoverflow.com/questions/8689156/why-does-javassist-insist-on-looking-for-a-default-annotation-value-when-one-is/8689214#8689214
     * for details.
     */
    annotationsAttribute.addAnnotation(adaptersAnnotation);

    this.addXmlJavaTypeAdapters(adaptersAnnotation, constantPool);

    assert assertAnnotationsOK(packageInfoClass);

  }

  private static final boolean assertAnnotationsOK(final CtClass packageInfoClass) throws ClassNotFoundException {

    final List<Object> annotations = java.util.Arrays.asList(packageInfoClass.getAnnotations());
    for (final Object a : annotations) {
      System.out.println("*** class annotation: " + a);
      System.out.println("    ...of type: " + a.getClass());
      System.out.println("    ...assignable to java.lang.annotation.Annotation? " + java.lang.annotation.Annotation.class.isInstance(a));

      if (a instanceof XmlJavaTypeAdapters) {
        final XmlJavaTypeAdapters x = (XmlJavaTypeAdapters)a;
        System.out.println("    ...value: " + java.util.Arrays.asList(x.value()));
      }
    }

    System.out.println("*** class annotations: " + java.util.Arrays.asList(packageInfoClass.getAnnotations()));
    return true;
  }

  private final boolean addXmlJavaTypeAdapters(final Annotation adaptersAnnotation, final ConstPool constantPool) throws ClassNotFoundException, NotFoundException {
    if (adaptersAnnotation == null) {
      throw new IllegalArgumentException("adaptersAnnotation", new NullPointerException("adaptersAnnotation"));
    }
    if (!XmlJavaTypeAdapters.class.getName().equals(adaptersAnnotation.getTypeName())) {
      throw new IllegalArgumentException("Wrong annotation: " + adaptersAnnotation.getTypeName());
    }
    boolean returnValue = false;
    
    Map<String, String> bindings = this.getBindings();
    if (bindings != null && !bindings.isEmpty()) {
      final Set<Entry<String, String>> bindingEntries = bindings.entrySet();
      if (bindingEntries != null && !bindingEntries.isEmpty()) {

        final CtClass xmlJavaTypeAdapterCtClass = ClassPool.getDefault().get(XmlJavaTypeAdapter.class.getName());
        assert xmlJavaTypeAdapterCtClass != null;

        ArrayMemberValue adaptersHolder = (ArrayMemberValue)adaptersAnnotation.getMemberValue("value");

        // Build a Map indexing existing 
        final Map<String, Annotation> existingAnnotationMap = new HashMap<String, Annotation>();
        final AnnotationMemberValue[] existingAdapterHolders;
        if (adaptersHolder == null) {
          existingAdapterHolders = null;
        } else {
          existingAdapterHolders = (AnnotationMemberValue[])adaptersHolder.getValue();
        }
        if (existingAdapterHolders != null && existingAdapterHolders.length > 0) {
          for (final AnnotationMemberValue existingAdapterHolder : existingAdapterHolders) {
            if (existingAdapterHolder != null) {
              final Annotation value = existingAdapterHolder.getValue();
              if (value != null) {
                assert value instanceof XmlJavaTypeAdapter;
                final ClassMemberValue typeHolder = (ClassMemberValue)value.getMemberValue("type");
                assert typeHolder != null;
                final String interfaceTypeName = typeHolder.getValue();
                assert interfaceTypeName != null;
                existingAnnotationMap.put(interfaceTypeName, value);
              }
            }
          }
        }

        final List<AnnotationMemberValue> xmlJavaTypeAdapterValueElements = new ArrayList<AnnotationMemberValue>();

        for (final Entry<String, String> entry : bindingEntries) {
          if (entry != null) {            
            final String adapterClassName = entry.getValue();
            if (adapterClassName != null) {
              returnValue = true;
              final String typeName = entry.getKey();
              assert typeName != null; // required by contract

              Annotation adapterAnnotation = existingAnnotationMap.get(typeName);
              if (adapterAnnotation == null) {
                adapterAnnotation = new Annotation(constantPool, xmlJavaTypeAdapterCtClass);
                // Set the "type" portion of @XmlJavaTypeAdapter(type = Foo.class, value = FooToFooImplAdapter.class)
                final ClassMemberValue typeClassHolder = (ClassMemberValue)adapterAnnotation.getMemberValue("type");
                assert typeClassHolder != null;
                typeClassHolder.setValue(typeName);
              }

              // Set the "value" portion of @XmlJavaTypeAdapter(type = Foo.class, value = FooToFooImplAdapter.class)
              final ClassMemberValue adapterClassHolder = (ClassMemberValue)adapterAnnotation.getMemberValue("value");
              assert adapterClassHolder != null;
              adapterClassHolder.setValue(adapterClassName);

              xmlJavaTypeAdapterValueElements.add(new AnnotationMemberValue(adapterAnnotation, constantPool));
            }
          }
        }
        
        // Take the array of @XmlJavaTypeAdapter instances and set it
        // as the value for @XmlJavaTypeAdapters#value().
        final AnnotationMemberValue[] values = xmlJavaTypeAdapterValueElements.toArray(new AnnotationMemberValue[xmlJavaTypeAdapterValueElements.size()]);
        assert values != null;
        assert values.length == xmlJavaTypeAdapterValueElements.size();

        if (adaptersHolder == null && values.length > 0) {
          adaptersHolder = new ArrayMemberValue(constantPool);
          adaptersAnnotation.addMemberValue("value", adaptersHolder);
        }
        adaptersHolder.setValue(values);
      }
    }
    return returnValue;
  }

  public Map<String, String> getBindings() {
    return this.bindings;
  }

  public void setBindings(final Map<String, String> bindings) {
    this.bindings = bindings;
  }

  public URL getResource(final String name) {
    URL url = null;
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    if (loader == null) {
      loader = this.getClass().getClassLoader();
    }
    if (loader == null) {
      url = ClassLoader.getSystemResource(name);
    } else {
      url = loader.getResource(name);
    }
    return url;
  }

}