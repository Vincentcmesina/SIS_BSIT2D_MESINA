package com.scc.dao;

public class Databaseinitializer {
    public static void initializer(){
        new UserDao();
        System.out.println(" -> Database tables verified");
    }
}
