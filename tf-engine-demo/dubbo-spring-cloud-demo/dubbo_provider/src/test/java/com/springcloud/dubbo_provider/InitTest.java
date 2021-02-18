package com.springcloud.dubbo_provider;

public class InitTest {

    private static InitTest initTest=new InitTest();
    private static int a;
    private static int b=0;

    public InitTest() {
        a++;
        b++;
    }

    public int getA() {
        return a;
    }

    public int getB() {
        return b;
    }

    public static void main(String[] args) {
        InitTest initTest=new InitTest();
        System.out.println(initTest.getA());
        System.out.println(initTest.getB());
        System.out.println(initTest.getClass().getClassLoader());
        System.out.println(String.class.getClassLoader());
    }
}
