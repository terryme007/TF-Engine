package com.springcloud.dubbo_consumer.customer;

public class FintechService implements Customer {

    @Override
    public String getCustomerName(String customerId) {
        return "fintechCustomer"+customerId;
    }

    @Override
    public boolean checkInfo(String name, String idCode) {
        return "fintech".equals(name) && "123456".equals(idCode);
    }
}
