package com.springcloud.dubbo_provider.service;

import com.springcloud.dubbo_api.model.UserInfo;
import com.springcloud.dubbo_api.service.IUserService;
import org.apache.dubbo.config.annotation.Service;

@Service(version = "2.0")
public class UserServiceImpl implements IUserService {

    @Override
    public UserInfo printUser(UserInfo userInfo) {
        System.out.println(userInfo.getName());
        return new UserInfo();
    }
}
