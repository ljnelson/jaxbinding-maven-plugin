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
import java.io.Serializable;

import java.security.ProtectionDomain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;

import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationMemberValue;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.ClassMemberValue;
import javassist.bytecode.annotation.MemberValue;

public class PackageInfoModifier {

  protected transient Logger logger;

  private Map<String, String> bindings;

  public PackageInfoModifier() {
    super();
    this.logger = this.createLogger();
    if (this.logger == null) {
      this.logger = Logger.getLogger(this.getClass().getName());
    }
  }

  protected Logger createLogger() {
    return Logger.getLogger(this.getClass().getName());
  }

  public Modification modify(final String pkg) throws CannotCompileException, IOException, NotFoundException {
    if (this.logger != null && this.logger.isLoggable(Level.FINER)) {
      this.logger.entering(this.getClass().getName(), "modify");
    }
    if (pkg == null) {
      throw new IllegalArgumentException("pkg", new NullPointerException("pkg"));
    }

    final String packageInfoClassName = String.format("%s.package-info", pkg);

    ClassPool classPool = this.getClassPool(packageInfoClassName);
    if (classPool == null) {
      classPool = ClassPool.getDefault();
    }
    assert classPool != null;

    Modification.Kind kind = Modification.Kind.UNMODIFIED;

    CtClass packageInfoCtClass = classPool.getOrNull(packageInfoClassName);
    if (packageInfoCtClass == null) {      
      packageInfoCtClass = classPool.makeClass(packageInfoClassName);
      kind = Modification.Kind.GENERATED;
    }
    assert packageInfoCtClass != null;
    
    final boolean modified = this.installXmlJavaTypeAdapters(packageInfoCtClass);
    if (modified && !kind.equals(Modification.Kind.GENERATED)) {
      kind = Modification.Kind.MODIFIED;
    }
    
    final byte[] bytes = packageInfoCtClass.toBytecode();
    assert bytes != null;
    assert bytes.length > 0;

    final Modification returnValue = new Modification(packageInfoCtClass, kind, bytes);

    if (this.logger != null && this.logger.isLoggable(Level.FINER)) {
      this.logger.exiting(this.getClass().getName(), "modify", returnValue);
    }
    return returnValue;
  }

  /**
   * Returns a Javassist {@link ClassPool} that is appropriate for the
   * supplied class name.
   *
   * <p>The default implementation of this method ignores the {@code
   * className} parameter and returns the return value of {@link
   * ClassPool#getDefault()}.  For nearly all cases, this is the
   * correct behavior and this method should not be overridden.</p>
   *
   * <p>If overrides of this method opt to return {@code null}, the
   * return value of {@link ClassPool#getDefault()} will be used
   * internally instead.</p>
   *
   * @param className the class name for which the returned {@link
   * ClassPool} might be appropriate; may be {@code null} and may
   * safely be ignored; provided for contextual information only
   *
   * @return a {@link ClassPool} instance, or {@code null}
   *
   * @see ClassPool
   *
   * @see ClassPool#getDefault()
   */
  protected ClassPool getClassPool(final String className) {
    return ClassPool.getDefault();
  }

  /**
   * Installs an {@link XmlJavaTypeAdapters} annotation on the
   * supplied {@link CtClass}, which must represent {@code
   * package-info.class}.
   *
   * @param packageInfoCtClass the result of calling, e.g., {@link
   * ClassPool#get(String) ClassPool.get(this.getPackage() +
   * "package-info")}; must not be {@code null}
   *
   * @exception ClassNotFoundException 
   */
  private final boolean installXmlJavaTypeAdapters(final CtClass packageInfoCtClass) throws NotFoundException {
    if (packageInfoCtClass == null) {
      throw new IllegalArgumentException("packageInfoCtClass", new NullPointerException("packageInfoCtClass"));
    }
    final String pkg = packageInfoCtClass.getPackageName();
    if (pkg == null) {
      throw new IllegalArgumentException("packageInfoCtClass", new IllegalStateException("packageInfoCtClass.getPackage() == null"));
    }
    if (!String.format("%s.package-info", pkg).equals(packageInfoCtClass.getName())) {
      throw new IllegalArgumentException("Wrong CtClass: " + packageInfoCtClass);
    }

    boolean modified = false;

    final ClassFile packageInfoClassFile = packageInfoCtClass.getClassFile();
    assert packageInfoClassFile != null;

    final ConstPool constantPool = packageInfoClassFile.getConstPool();
    assert constantPool != null;

    AnnotationsAttribute annotationsAttribute = (AnnotationsAttribute)packageInfoClassFile.getAttribute(AnnotationsAttribute.visibleTag);
    if (annotationsAttribute == null) {
      annotationsAttribute = new AnnotationsAttribute(constantPool, AnnotationsAttribute.visibleTag);
      packageInfoClassFile.addAttribute(annotationsAttribute);
      modified = true;
    }
    assert annotationsAttribute != null;

    Annotation adaptersAnnotation = annotationsAttribute.getAnnotation(XmlJavaTypeAdapters.class.getName());
    if (adaptersAnnotation == null) {
      ClassPool classPool = this.getClassPool(XmlJavaTypeAdapters.class.getName());
      if (classPool == null) {
        classPool = ClassPool.getDefault();
      }
      assert classPool != null;
      CtClass xmlJavaTypeAdaptersCtClass = classPool.getOrNull(XmlJavaTypeAdapters.class.getName());
      assert xmlJavaTypeAdaptersCtClass != null;
      adaptersAnnotation = new Annotation(constantPool, xmlJavaTypeAdaptersCtClass);
      modified = true;
    } else if (adaptersAnnotation.getMemberValue("value") == null) {
      final ArrayMemberValue amv = new ArrayMemberValue(constantPool);
      amv.setValue(new AnnotationMemberValue[0]);
      adaptersAnnotation.addMemberValue("value", amv);
      modified = true;
    }
    assert adaptersAnnotation != null;
    assert adaptersAnnotation.getMemberValue("value") != null;

    modified = this.installXmlJavaTypeAdapters(adaptersAnnotation, constantPool) || modified;

    /*
     * THIS COMMENT IS UNDER REVISION
     *
     * You would think this line would be required ONLY in the case
     * where the annotation itself was not found.  But you actually
     * have to add it to its containing AnnotationsAttribute in ALL
     * cases.  This doesn't make any sense.  See
     * http://stackoverflow.com/questions/8689156/why-does-javassist-insist-on-looking-for-a-default-annotation-value-when-one-is/8689214#8689214
     * for details.
     *
     * Additionally, you must re-add the annotation as the last
     * operation here in all cases.  Otherwise the changes made by the
     * installXmlJavaTypeAdapters() method above are not actually made
     * permanent.
     */
    if (modified) {
      annotationsAttribute.addAnnotation(adaptersAnnotation);
    }

    return modified;
  }

  /**
   * Given a Javassist {@link Annotation} representing the {@code
   * @}{@link XmlJavaTypeAdapters} annotation, does whatever is
   * necessary to make such an {@link Annotation} contain an array of
   * {@link AnnotationMemberValue} objects, each of which represents
   * an {@link XmlJavaTypeAdapter} annotation that has been specified
   * in accordance with the rules represented by the return value of
   * the {@link #getBindings()} method.
   *
   * @param adaptersAnnotation a non-{@code null} Javassist {@link
   * Annotation} representation of the {@code @}{@link
   * XmlJavaTypeAdapters} annotation
   *
   * @param constantPool a {@link ConstPool} instance for use in
   * constructing new {@link Annotation} instances; Javassist
   * documentation does not specify whether {@code null} is a
   * permitted value or not
   *
   * @exception NotFoundException in extremely rare situations when a
   * JDK class cannot be located, usually due to {@link ClassLoader}
   * problems
   */
  private final boolean installXmlJavaTypeAdapters(final Annotation adaptersAnnotation, final ConstPool constantPool) throws NotFoundException {
    if (this.logger != null && this.logger.isLoggable(Level.FINER)) {
      this.logger.entering(this.getClass().getName(), "installXmlJavaTypeAdapters", new Object[] { adaptersAnnotation, constantPool });
    }
    if (adaptersAnnotation == null) {
      throw new IllegalArgumentException("adaptersAnnotation", new NullPointerException("adaptersAnnotation"));
    }
    if (!XmlJavaTypeAdapters.class.getName().equals(adaptersAnnotation.getTypeName())) {
      throw new IllegalArgumentException("Wrong annotation: " + adaptersAnnotation.getTypeName());
    }

    boolean modified = false;

    Map<String, String> bindings = this.getBindings();
    if (bindings != null && !bindings.isEmpty()) {
      final Set<Entry<String, String>> bindingEntries = bindings.entrySet();
      if (bindingEntries != null && !bindingEntries.isEmpty()) {

        ClassPool classPool = this.getClassPool(XmlJavaTypeAdapter.class.getName());
        if (classPool == null) {
          classPool = ClassPool.getDefault();
        }
        assert classPool != null;

        ArrayMemberValue adaptersHolder = (ArrayMemberValue)adaptersAnnotation.getMemberValue("value");

        final Map<String, Annotation> existingAnnotationMap = getExistingXmlJavaTypeAdapters(adaptersHolder);        

        final List<AnnotationMemberValue> adapters = new ArrayList<AnnotationMemberValue>();

        final CtClass xmlJavaTypeAdapterCtClass = classPool.get(XmlJavaTypeAdapter.class.getName());
        assert xmlJavaTypeAdapterCtClass != null;

        for (final Entry<String, String> entry : bindingEntries) {
          if (entry != null) {            
            final String adapterClassName = entry.getValue();
            if (adapterClassName != null) {
              
              final String typeName = entry.getKey();
              if (typeName == null) {
                // An @XmlJavaTypeAdapter annotation is supposed to
                // have a type() attribute.  If it does not, there's
                // nothing we can do.
                continue;
              }

              // Find or create a new @XmlJavaTypeAdapter with a type() of typeName.
              Annotation adapterAnnotation = null;
              if (existingAnnotationMap != null && !existingAnnotationMap.isEmpty()) {
                adapterAnnotation = existingAnnotationMap.get(typeName);
              }
              
              if (adapterAnnotation != null) {
                // Preexisting
                final ClassMemberValue v = (ClassMemberValue)adapterAnnotation.getMemberValue("value");
                assert v != null;
                final String existingClassName = v.getValue();
                if (adapterClassName.equals(existingClassName)) {
                  // The annotation is already correctly specified
                  continue;
                }
              }

              if (adapterAnnotation == null) {
                adapterAnnotation = this.newXmlJavaTypeAdapter(constantPool, typeName);
                modified = true;
              }
              assert adapterAnnotation != null;
              assert XmlJavaTypeAdapter.class.getName().equals(adapterAnnotation.getTypeName());
              assert typeName.equals(((ClassMemberValue)adapterAnnotation.getMemberValue("type")).getValue());

              modified = setXmlAdapter(adapterAnnotation, adapterClassName) || modified;
              assert adapterClassName.equals(((ClassMemberValue)adapterAnnotation.getMemberValue("value")).getValue());

              // We have just found or created an @XmlJavaTypeAdapter;
              // add it to the list of such values.  This superset
              // list will become the new list of @XmlJavaTypeAdapters
              // on the package-info class.
              adapters.add(new AnnotationMemberValue(adapterAnnotation, constantPool));
            }
          }
        }
        
        // Take the array of @XmlJavaTypeAdapter instances and set it
        // as the value for @XmlJavaTypeAdapters#value().
        final AnnotationMemberValue[] values = adapters.toArray(new AnnotationMemberValue[adapters.size()]);

        if (adaptersHolder == null) {
          adaptersHolder = new ArrayMemberValue(constantPool);
          adaptersAnnotation.addMemberValue("value", adaptersHolder);
          modified = true;
        }
        assert values != null;
        adaptersHolder.setValue(values);

      }
    }
    return modified;
  }

  private final Annotation newXmlJavaTypeAdapter(final ConstPool constantPool, final String typeName) throws NotFoundException {

    ClassPool classPool = this.getClassPool(XmlJavaTypeAdapter.class.getName());
    if (classPool == null) {
      classPool = ClassPool.getDefault();
    }
    assert classPool != null;

    final CtClass xmlJavaTypeAdapterCtClass = classPool.getOrNull(XmlJavaTypeAdapter.class.getName());
    assert xmlJavaTypeAdapterCtClass != null;
    
    final Annotation adapterAnnotation = new Annotation(constantPool, xmlJavaTypeAdapterCtClass);

    // Retrieve the "holder" for the type() annotation attribute
    // ("Foo.class" in the following sample:
    //
    //  @XmlJavaTypeAdapter(type = Foo.class, value = FooToFooImplAdapter.class)
    //
    final ClassMemberValue typeClassHolder = (ClassMemberValue)adapterAnnotation.getMemberValue("type");

    // (Because @XmlJavaTypeAdapter is a JDK-supplied class, and
    // Javassist is just reading it, we are guaranteed that the type()
    // annotation attribute will be modeled in Javassist.)
    assert typeClassHolder != null;

    // Set the holder's value, thus installing the annotation's type()
    // value.
    typeClassHolder.setValue(typeName);

    return adapterAnnotation;
  }

  private static final boolean setXmlAdapter(final Annotation adapterAnnotation, final String adapterClassName) {
    if (adapterClassName == null) {
      throw new IllegalArgumentException("adapterClassName", new NullPointerException("adapterClassName"));
    }
    if (adapterAnnotation == null) {
      throw new IllegalArgumentException("adapterAnnotation", new NullPointerException("adapterAnnotation"));
    }
    if (!XmlJavaTypeAdapter.class.getName().equals(adapterAnnotation.getTypeName())) {
      throw new IllegalArgumentException("adapterAnnotation does not represent " + XmlJavaTypeAdapter.class.getName());
    }

    // Retrieve the "holder" for the value() annotation
    // attribute ("FooToFooImplAdapter.class" in the
    // following sample:
    // 
    //   @XmlJavaTypeAdapter(type = Foo.class, value = FooToFooImplAdapter.class)
    //
    final ClassMemberValue adapterClassHolder = (ClassMemberValue)adapterAnnotation.getMemberValue("value");

    // (Because @XmlJavaTypeAdapter is a JDK-supplied class,
    // and Javassist is just reading it, we are guaranteed
    // that the type() annotation attribute will be modeled
    // in Javassist.)
    assert adapterClassHolder != null;

    final String old = adapterClassHolder.getValue();

    // Set the holder's value, thus installing the
    // annotation's value() value.
    adapterClassHolder.setValue(adapterClassName);

    if (old == null) {
      return adapterClassName != null;
    }
    return !old.equals(adapterClassName);
  }

  /**
   * A narrow-purpose helper method that accepts an {@link
   * ArrayMemberValue} parameter whose value is (semantically) the
   * return value of the {@link XmlJavaTypeAdapters#value()}
   * annotation attribute, and returns a {@link Map} of {@link
   * Annotation} objects representing {@link XmlJavaTypeAdapter}
   * annotations indexed by the {@linkplain XmlJavaTypeAdapter#type()
   * types} that each of them governs.
   *
   * <p>This method never returns {@code null}.</p>
   *
   * @param adaptersHolder the {@link ArrayMemberValue} that resulted
   * from calling {@link Annotation#getArrayMemberValue()
   * Annotation#getArrayMemberValue("value")} on an {@link Annotation}
   * that is Javassist's representation of the {@link
   * XmlJavaTypeAdapters} annotation.  This parameter may be {@code
   * null}.
   *
   * @return a {@link Map} of {@link Annotation} objects representing
   * {@link XmlJavaTypeAdapter} annotations indexed by the {@linkplain
   * XmlJavaTypeAdapter#type() types} that each of them governs; never
   * {@code null}
   */
  private static final Map<String, Annotation> getExistingXmlJavaTypeAdapters(final ArrayMemberValue adaptersHolder) {
    // Build a Map indexing existing @XmlJavaTypeAdapter annotations
    // by the types that they govern.  First create an array of the
    // existing @XmlJavaTypeAdapter annotations.  This is more
    // difficult than it should be; hence the copious boilerplate
    // below.
    
    final List<AnnotationMemberValue> existingAdapterHolders;
    if (adaptersHolder == null) {
      existingAdapterHolders = null;
    } else {
      final MemberValue[] rawMemberValue = adaptersHolder.getValue();
      if (rawMemberValue == null || rawMemberValue.length <= 0) {
        existingAdapterHolders = null;
      } else {
        existingAdapterHolders = new ArrayList<AnnotationMemberValue>();
        for (final MemberValue mv : rawMemberValue) {
          if (mv instanceof AnnotationMemberValue) {
            existingAdapterHolders.add((AnnotationMemberValue)mv);
          }
        }
      }
    }
      
    // Loop through the existing @XmlJavaTypeAdapter annotations and
    // store them in a map indexed by the names of the types they
    // govern.
    final Map<String, Annotation> xmlJavaTypeAdapters = new HashMap<String, Annotation>();
    if (existingAdapterHolders != null && !existingAdapterHolders.isEmpty()) {
      for (final AnnotationMemberValue existingAdapterHolder : existingAdapterHolders) {
        if (existingAdapterHolder != null) {
          final Annotation xmlJavaTypeAdapter = existingAdapterHolder.getValue();
          if (xmlJavaTypeAdapter != null && XmlJavaTypeAdapter.class.getName().equals(xmlJavaTypeAdapter.getTypeName())) {
            final ClassMemberValue typeHolder = (ClassMemberValue)xmlJavaTypeAdapter.getMemberValue("type");
            if (typeHolder != null) {
              final String interfaceTypeName = typeHolder.getValue();
              if (interfaceTypeName != null) {
                xmlJavaTypeAdapters.put(interfaceTypeName, xmlJavaTypeAdapter);
              }
            }
          }
        }
      }
    }
    
    return xmlJavaTypeAdapters;
  }

  public Map<String, String> getBindings() {
    return this.bindings;
  }

  public void setBindings(final Map<String, String> bindings) {
    this.bindings = bindings;
  }

  public static final class Modification implements Serializable {

    private static final long serialVersionUID = 1L;

    public static enum Kind {
      UNMODIFIED, GENERATED, MODIFIED;
    }

    private final CtClass packageInfoCtClass;

    private final byte[] bytes;

    private final Kind kind;

    private Modification(final CtClass packageInfoCtClass, final Kind kind, final byte[] bytes) {
      super();
      if (packageInfoCtClass == null) {
        throw new IllegalArgumentException("packageInfoCtClass", new NullPointerException("packageInfoCtClass"));
      }
      if (!"package-info".equals(packageInfoCtClass.getSimpleName())) {
        throw new IllegalArgumentException("packageInfoCtClass must be a package-info class: " + packageInfoCtClass);
      }
      this.packageInfoCtClass = packageInfoCtClass;
      assert packageInfoCtClass.isFrozen();
      if (kind == null) {
        this.kind = Kind.MODIFIED;
      } else {
        this.kind = kind;
      }
      if (bytes == null) {
        this.bytes = new byte[0];
      } else {
        this.bytes = bytes;
      }
    }

    public final String getPackageName() {
      return this.packageInfoCtClass.getPackageName();
    }

    public final Kind getKind() {
      return this.kind;
    }

    public final byte[] toByteArray() {
      return this.bytes;
    }

  }

}
