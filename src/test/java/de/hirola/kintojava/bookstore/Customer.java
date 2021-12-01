package de.hirola.kintojava.bookstore;

import de.hirola.kintojava.model.Persisted;
import de.hirola.kintojava.model.PersistentObject;

import java.util.Objects;
import java.util.UUID;

public class Customer extends PersistentObject {

    // attributes to save in local datastore
    @Persisted
    private String customerID;
    @Persisted
    private String firstName;
    @Persisted
    private String lastName;
    // 1:1 relation - one address
    @Persisted
    private Address address;

    // we need a constructor for reflection
    public Customer() {
        customerID = UUID.randomUUID().toString();
    }

    public Customer(String firstName, String lastName, Address address) {
        customerID = UUID.randomUUID().toString();
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
    }

    public String getCustomerID() {
        return customerID;
    }

    public void setCustomerID(String customerID) {
        this.customerID = customerID;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Customer customer = (Customer) o;
        return Objects.equals(customerID, customer.customerID)
                && Objects.equals(firstName, customer.firstName)
                && Objects.equals(lastName, customer.lastName)
                && Objects.equals(address, customer.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerID, firstName, lastName, address);
    }
}
