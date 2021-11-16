package de.hirola.kintojava.bookstore;

import de.hirola.kintojava.model.KintoObject;
import de.hirola.kintojava.model.Persisted;

import java.util.Objects;

public class Book extends KintoObject {

    // attributes to save in local datastore
    @Persisted
    private String isbn;
    @Persisted
    private String title;
    @Persisted
    private double price;

    // an unsaved attribute
    private int numberInStock;

    // we need a constructor for reflection
    public Book() {
        isbn = "ISBN-";
        title = "The unwritten Book";
        price = 10.99;
        numberInStock = 0;
    }

    public String getISBN() {
        return isbn;
    }

    public void setISBN(String isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getNumberInStock() {
        return numberInStock;
    }

    public void setNumberInStock(int numberInStock) {
        this.numberInStock = numberInStock;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        Book book = (Book) o;
        return isbn.equals(book.isbn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), isbn);
    }
}
