package de.hirola.kintojava.bookstore;

import de.hirola.kintojava.model.Persisted;
import de.hirola.kintojava.model.PersistentObject;

import java.util.ArrayList;
import java.util.List;
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
    @Persisted
    private boolean hasBonus;
    @Persisted
    private Author favoriteAuthor;
    // 1:1 relation - one address
    @Persisted
    private final List<Address> addressList;

    // we need a constructor for reflection
    public Customer() {
        customerID = UUID.randomUUID().toString();
        firstName = "";
        lastName = "";
        hasBonus = false;
        favoriteAuthor = null;
        addressList = new ArrayList<>();
    }

    public Customer(String firstName, String lastName, boolean hasBonus, Address address) {
        customerID = UUID.randomUUID().toString();
        this.firstName = firstName;
        this.lastName = lastName;
        this.hasBonus = hasBonus;
        favoriteAuthor = null;
        addressList = new ArrayList<>();
        addressList.add(address);
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

    public boolean isHasBonus() {
        return hasBonus;
    }

    public void setHasBonus(boolean hasBonus) {
        this.hasBonus = hasBonus;
    }

    public List<Address> getAddressList() {
        return addressList;
    }

    public void addAddress(Address address) {
        if (!addressList.contains(address)) {
            addressList.add(address);
        }
    }

    public Author getFavoriteAuthor() {
        return favoriteAuthor;
    }

    public void setFavoriteAuthor(Author favoriteAuthor) {
        this.favoriteAuthor = favoriteAuthor;
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
                && Objects.equals(lastName, customer.lastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerID, firstName, lastName);
    }
}
