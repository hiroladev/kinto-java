package de.hirola.kintojava.bookstore;

import de.hirola.kintojava.model.KintoObject;
import de.hirola.kintojava.model.Persisted;

public class Address extends KintoObject {

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
