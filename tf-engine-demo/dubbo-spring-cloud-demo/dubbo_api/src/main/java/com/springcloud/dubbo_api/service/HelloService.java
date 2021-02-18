package com.springcloud.dubbo_api.service;

import com.springcloud.dubbo_api.model.UserInfo;

import java.util.Map;

/**
 * @Date: 2019/9/2 16:26
 * @Version: 1.0
 * @Desc:
 */
public interface HelloService {
    String hello(String name);

    boolean check(String value, String oldValue);

    Map<String,String> getIpInfo(String ip);

    void printUser(UserInfo userInfo);
}
