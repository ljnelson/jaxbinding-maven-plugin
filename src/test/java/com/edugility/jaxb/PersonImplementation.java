package com.edugility.jaxb;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PersonImplementation implements Person {

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