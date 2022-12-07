package com.kafka.java.demo21;

public class Student {

    public Student(String name, String age){
        this.name = name;
        this.age = age;
    }
    private String name;
    private String age;

    public String Name(){
        return this.name;
    }
    public String Age(){
        return this.age;
    }

}


