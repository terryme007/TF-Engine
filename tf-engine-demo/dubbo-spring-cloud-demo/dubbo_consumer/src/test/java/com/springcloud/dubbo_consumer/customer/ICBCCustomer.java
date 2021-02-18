package com.springcloud.dubbo_consumer.customer;

public class ICBCCustomer extends CustomerService {

    public ICBCCustomer(Customer customer) {
        super(customer);
    }

    @Override
    public String getCustomerName(String customerId){
        return super.getCustomerName(customerId);
    }

    @Override
    public boolean checkInfo(String name,String idCode){
        return super.checkInfo(name, idCode);
    }
}