package com.springcloud.dubbo_consumer.customer;

public abstract class CustomerService implements Customer {

    private Customer customer;

    CustomerService(Customer customer) {
        this.customer = customer;
    }

    public String getCustomerName(String customerId){
        return customer.getCustomerName(customerId);
    }

    public boolean checkInfo(String name,String idCode){
        return customer.checkInfo(name,idCode);
    }
}
