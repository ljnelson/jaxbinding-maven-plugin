package com.edugility.jaxb;

import javax.xml.bind.annotation.XmlRootElement;

import java.io.Serializable;

@XmlRootElement
public class PersonImplementation implements Person, Serializable {

  private static final long serialVersionUID = 1L;

  private int age;

  private String name;

  public PersonImplementation() {
    super();
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public void setName(final String name) {
    this.name = name;
  }

  @Override
  public int getAge() {
    return this.age;
  }

  @Override
  public void setAge(final int age) {
    this.age = age;
  }

}