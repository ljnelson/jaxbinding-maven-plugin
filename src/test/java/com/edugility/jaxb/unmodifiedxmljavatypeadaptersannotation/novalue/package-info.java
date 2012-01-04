@XmlJavaTypeAdapters({ @XmlJavaTypeAdapter(type = com.edugility.jaxb.Person.class, value = com.edugility.jaxb.AnyTypeAdapter.class) })
package com.edugility.jaxb.unmodifiedxmljavatypeadaptersannotation.novalue;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;