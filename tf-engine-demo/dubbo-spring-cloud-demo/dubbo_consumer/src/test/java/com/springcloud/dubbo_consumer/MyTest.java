package com.springcloud.dubbo_consumer;

import com.springcloud.dubbo_consumer.customer.Customer;
import com.springcloud.dubbo_consumer.customer.FintechService;
import com.springcloud.dubbo_consumer.customer.ICBCCustomer;
import com.springcloud.dubbo_consumer.customer.SunyardService;
import org.junit.Test;

public class MyTest {

    @Test
    public void test1(){
        SunyardService sunyardService =new SunyardService();
        Customer sunyardCustomer=new ICBCCustomer(sunyardService);
        System.out.println(sunyardCustomer.getCustomerName("123"));
        System.out.println(sunyardCustomer.checkInfo("sunyard","123456"));
        System.out.println("===========================");
        FintechService fintechService=new FintechService();
        Customer fintechCustomer = new ICBCCustomer(fintechService);
        System.out.println(fintechCustomer.getCustomerName("1234"));
        System.out.println(fintechCustomer.checkInfo("sunyard","123456"));
    }
}
