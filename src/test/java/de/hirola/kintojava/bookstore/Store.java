package de.hirola.kintojava.bookstore;

import de.hirola.kintojava.model.Persisted;
import de.hirola.kintojava.model.PersistentObject;

import java.util.ArrayList;
import java.util.List;

public class Store extends PersistentObject {

    // attributes to save in local datastore
    @Persisted
    private String name;
    // 1:m relations - many customers
    @Persisted
    private List<Customer> customers;
    // 1:m relations - many books
    @Persisted
    private List<Book> books;

    // we need a constructor for reflection
    public Store() {
        name = "My Shop";
        customers = new ArrayList<>();
        books = new ArrayList<>();
    }

    public Store(String name, List<Customer> customers, List<Book> books) {
        this.name = name;
        this.customers = customers;
        this.books = books;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Customer> getCustomers() {
        return customers;
    }

    public List<Book> getBooks() {
        return books;
    }

    public void addCustomer(Customer customer) {
        if (customer != null) {
            if (!customers.contains(customer)) {
                customers.add(customer);
            }
        }
    }

    public void addBook(Book book) {
        if (book != null) {
            if (!books.contains(book)) {
                books.add(book);
            }
        }
    }
}
