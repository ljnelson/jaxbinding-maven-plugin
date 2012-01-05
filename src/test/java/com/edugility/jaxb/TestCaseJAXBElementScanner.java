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

import java.io.File;

import java.net.URI;
import java.net.URL;

import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestCaseJAXBElementScanner {

  @Test
  public void testAll() throws Exception {
    final JAXBElementScanner scanner = new JAXBElementScanner();
    scanner.setBindingFilter(new JAXBElementScanner.WhitelistRegexBindingFilter("^com\\.edugility\\."));
    final File testOutputDirectory = this.getTestOutputDirectory();
    assertNotNull(testOutputDirectory);
    assertTrue(testOutputDirectory.isDirectory());
    scanner.addURI(testOutputDirectory.toURI());
    final Map<String, String> map = scanner.scan();
    assertNotNull(map);
    assertFalse(map.isEmpty());
    assertEquals("com.edugility.jaxb.PersonImplementation", map.get("com.edugility.jaxb.Person"));
    System.out.println("Map: " + map);
  }

  public File getTestOutputDirectory() {
    final File directory = new File(System.getProperty("maven.project.build.testOutputDirectory", System.getProperty("project.build.testOutputDirectory", "target/test-classes")));
    assertTrue(directory.isDirectory());
    assertTrue(directory.canWrite());
    return directory;
  }
}