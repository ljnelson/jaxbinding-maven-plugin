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

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class AbstractImplementationBinder {

  public AbstractImplementationBinder() {
    super();
  }

  public Set<Class<?>> getImplementedInterfaces(final Iterable<Class<?>> classes) {
    Set<Class<?>> returnValue = null;
    if (classes != null) {
      final Iterator<Class<?>> iterator = classes.iterator();
      if (iterator != null && iterator.hasNext()) {
        returnValue = new LinkedHashSet<Class<?>>();
        while (iterator.hasNext()) {
          final Class<?> cls = iterator.next();
          if (cls != null) {
            this.addImplementedInterfaces(returnValue, cls);
          }
        }
      }
    }
    if (returnValue == null) {
      returnValue = Collections.emptySet();
    }
    return returnValue;
  }

  /**
   * @param cls the {@link Class} whose interface (the second
   * parameter) is being tested.  This parameter may be {@code null}.
   *
   * @param iface the {@linkplain Class#isInterface() interface} that
   * the {@link cls} parameter implements that is being tested.  This
   * parameter may be {@code null}.
   *
   * @return {@code true} if {@code iface} should be accepted as a
   * relevant interface that {@code cls} implements; {@code false}
   * otherwise
   */
  protected boolean accept(final Class<?> cls, final Class<?> iface) {
    return cls != null && iface != null && iface.isInterface() && !cls.isInterface();
  }

  private final void addImplementedInterfaces(final Set<Class<?>> interfaceSet, final Class<?> cls) {
    if (interfaceSet != null && cls != null) {
      if (cls.isInterface()) {
        interfaceSet.add(cls);
      }
      final Class<?>[] interfaces = cls.getInterfaces();
      if (interfaces != null && interfaces.length > 0) {
        for (final Class<?> iface : interfaces) {
          if (this.accept(cls, iface)) {
            interfaceSet.add(iface);
          }
        }
      }
    }
  }

}