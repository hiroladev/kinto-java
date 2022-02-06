package de.hirola.kintojava.bookstore;

import de.hirola.kintojava.model.Persisted;
import de.hirola.kintojava.model.PersistentObject;

import java.time.LocalDate;

public class Author extends PersistentObject {

    // attributes to save in local datastore
    @Persisted
    private final String firstName;
    @Persisted
    private final String lastName;
    @Persisted
    private final LocalDate birthday;

    // we need a constructor for reflection
    public Author() {
        firstName = "";
        lastName = "";
        birthday = LocalDate.now();
    }

    public Author(String firstName, String lastName, LocalDate birthday) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthday = birthday;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getBirthday() {
        return birthday;
    }
}
