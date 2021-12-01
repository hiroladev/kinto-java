package de.hirola.kintojava.bookstore;

import de.hirola.kintojava.model.Persisted;
import de.hirola.kintojava.model.PersistentObject;

public class Address extends PersistentObject {

    // attributes to save in local datastore
    @Persisted
    private String street;
    @Persisted
    private int number;
    @Persisted
    private String place;
    @Persisted
    private String postalCode;

    // we need a constructor for reflection
    public Address() {
        street = "";
        number = 0;
        place = "";
        postalCode = "";
    }

    public Address(String street, int number, String place, String postalCode) {
        this.street = street;
        this.number = number;
        this.place = place;
        this.postalCode = postalCode;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }
}
