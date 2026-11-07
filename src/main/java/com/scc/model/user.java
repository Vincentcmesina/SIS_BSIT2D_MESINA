package com.scc.model;

public record user(int id, String username, String password, String roles, String status) {
    @Override
    public String toString(){
        return String.format("[%d] %s (%s) - Status: %s", id, username, roles, status);

    }
}