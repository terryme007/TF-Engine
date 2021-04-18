package com.springcloud.dubbo_api.model;

import lombok.Data;

import java.util.List;

@Data
public class UserInfo {

    private String name;

    private int age;

    private List<UserInfo> friends;

    private String[] sportType;
}
