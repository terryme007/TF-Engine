package com.springcloud.dubbo_provider.service;

import com.springcloud.dubbo_api.service.IHelloService;
import org.apache.dubbo.config.annotation.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 *
 * @Date: 2019/9/3
 * @Time: 21:55
 * @email: inwsy@hotmail.com
 * Description:
 */
@Service(version = "1.0")
public class HelloServiceImpl implements IHelloService {
    @Override
    public String hello(String name) {
        return "Hello " + name;
    }

    @Override
    public boolean check(String value, String oldValue) {
        return value==null?oldValue==null:value.equals(oldValue);
    }

    @Override
    public Map<String, String> getIpInfo(String ip) {
        Map<String, String> res=new HashMap<>();
        res.put("ip",ip);
        res.put("city","浙江省杭州市");
        res.put("address","江南大道3888号");
        res.put("country","CN");
        return res;
    }
}
