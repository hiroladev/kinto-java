package de.hirola.kintojava.bookstore;

import de.hirola.kintojava.model.KintoObject;
import de.hirola.kintojava.model.Persisted;

import java.util.ArrayList;

public class Store extends KintoObject {

    // attributes to save in local datastore
    @Persisted
    private String name;
    // 1:m relations - many customers
    @Persisted
    private ArrayList<Customer> customers;

    // we need a constructor for reflection
    public Store() {
        name = "My Shop";
        customers = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Customer> getCustomers() {
        return customers;
    }

    public void addCustomer(Customer customer) {
        if (customer != null) {
            if (!customers.contains(customer)) {
                customers.add(customer);
            }
        }
    }
}
