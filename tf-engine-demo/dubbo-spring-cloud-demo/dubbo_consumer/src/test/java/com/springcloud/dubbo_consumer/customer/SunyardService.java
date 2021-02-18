package com.springcloud.dubbo_consumer.customer;

public class SunyardService implements Customer {

    @Override
    public String getCustomerName(String customerId) {
        return "sunyardCustomer"+customerId;
    }

    @Override
    public boolean checkInfo(String name, String idCode) {
        return "sunyard".equals(name) && "123456".equals(idCode);
    }
}
