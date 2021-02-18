package com.springcloud.dubbo_consumer.customer;

public interface Customer {

    public String getCustomerName(String customerId);

    public boolean checkInfo(String name, String idCode);
}
