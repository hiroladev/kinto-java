package de.hirola.kintojava;

import de.hirola.kintojava.bookstore.*;
import de.hirola.kintojava.logger.KintoLogger;
import de.hirola.kintojava.logger.LogEntry;
import de.hirola.kintojava.model.KintoObject;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LibraryTest {

    @Test
    void dataStoreTests() {
        // list of books
        Author author1 = new Author("Anne","Welcome", LocalDate.now());
        Author author2 = new Author("Mike","Land", LocalDate.now());
        Book book1 = new Book("4711","Test Book", author1,10.99,100);
        Book book2 = new Book("0815","The 08/15 gun", author2,7.99,50);
        List<Book> books = new ArrayList<>();
        books.add(book1);
        books.add(book2);
        // an address
        Address address = new Address("Street",1, "Place", "081547711");
        // list of customers
        Customer customer1 = new Customer("Adam","Customer",address);
        Customer customer2 = new Customer("Eva","Customer",address);
        List<Customer> customers = new ArrayList<>();
        customers.add(customer1);
        customers.add(customer2);
        // a book store
        Store bookStore =  new Store("My book Store", customers, books);
        // initialize the local datastore
        // add all types for managing by kinto java
        ArrayList<Class<? extends KintoObject>> typeList = new ArrayList<>();
        typeList.add(Author.class);
        typeList.add(Book.class);
        typeList.add(Address.class);
        typeList.add(Customer.class);
        typeList.add(Store.class);

        try {
            // create a kinto java configuration
            KintoConfiguration configuration = new KintoConfiguration.Builder("BookStoreTest")
                    .objectTypes(typeList)
                    .build();
            // create the kinto java instance
            Kinto kinto = Kinto.getInstance(configuration);
            // test logging to file
            KintoLogger logger = kinto.getKintoLogger();
            logger.log(LogEntry.Severity.DEBUG,"LibraryTest");
            // save the objects in local data store
            kinto.add(author1);
            kinto.add(author2);
            kinto.add(book1);
            kinto.add(book2);
            kinto.add(address);
            kinto.add(customer1);
            kinto.add(customer2);
            kinto.add(bookStore);

            // test simple objects
            List<? extends KintoObject> availableBooks = kinto.findAll(Book.class);
            assertEquals(2,availableBooks.size());

            // test 1:1 objects
            // 3 customer with 1 address
            // a customer without store
            Customer customer3 = new Customer("Ben","Alone",address);
            kinto.add(customer3);
            List<? extends KintoObject> allCustomers = kinto.findAll(Customer.class);
            assertEquals(3, allCustomers.size());

            // test 1:m objects
            // 1 book store with 2 books and 2 customers
            List<? extends KintoObject> bookStores = kinto.findAll(Store.class);
            assertEquals(1, bookStores.size());
            List<Book> storeBooks = ((Store) bookStores.get(0)).getBooks();
            assertEquals(2, storeBooks.size());
            List<Customer> storeCustomer = ((Store) bookStores.get(0)).getCustomers();
            assertEquals(2, storeCustomer.size());

            // test update with embedded object
            // customer get new address
            Address newAddress = new Address("Dorfstrasse", 5, "Dorf", "08150");
            kinto.add(newAddress);
            customer1.addAddress(newAddress);
            kinto.update(customer1);

            // remove all data
            // kinto.clearLocalDataStore();

        } catch (KintoException exception) {
            exception.printStackTrace();
            fail();
        }
    }

    @Test
    void dataStoreEmbeddedObjectTest() {
        // list of books with author
        Author author1 = new Author("Anne","Welcome", LocalDate.now());
        Book book1 = new Book("4711","Test Book", author1,10.99,100);
        Book book2 = new Book("0815","The 08/15 gun", author1,7.99,50);
        List<Book> books = new ArrayList<>();
        books.add(book1);
        books.add(book2);

        // an address
        Address address1 = new Address("Street",1, "Place", "081547711");
        Address address2 = new Address("Way",154, "An other Place", "98765");
        // a customer with 2 addresses
        Customer customer1 = new Customer("Adam","Customer",address1);
        customer1.addAddress(address2);
        // list of customers
        List<Customer> customers = new ArrayList<>();
        customers.add(customer1);
        // a book store
        Store bookStore =  new Store("My book Store", customers, books);
        // initialize the local datastore
        // add all types for managing by kinto java
        ArrayList<Class<? extends KintoObject>> typeList = new ArrayList<>();
        typeList.add(Author.class);
        typeList.add(Book.class);
        typeList.add(Address.class);
        typeList.add(Customer.class);
        typeList.add(Store.class);

        try {
            // create a kinto java configuration
            KintoConfiguration configuration = new KintoConfiguration.Builder("BookStoreTest")
                    .objectTypes(typeList)
                    .build();
            // create the kinto java instance
            Kinto kinto = Kinto.getInstance(configuration);
            // test logging to file
            KintoLogger logger = kinto.getKintoLogger();
            logger.log(LogEntry.Severity.DEBUG,"LibraryTest");
            // save the objects in local data store
            kinto.add(address1);
            kinto.add(address2);
            kinto.add(author1);
            kinto.add(book1);
            kinto.add(book2);
            kinto.add(customer1);
            kinto.add(bookStore);

            // make an update
            kinto.update(author1);

            // test objects with embedded objects
            // 1. an embedded object without another embedded object
            List<? extends KintoObject> bookStores = kinto.findAll(Store.class);
            Book bookWithAuthor = (Book) kinto.findByUUID(Book.class, book1.getUUID());
            assertTrue(bookWithAuthor.getAuthor().getLastName().length() > 0, "Book attribute contains no values.");

            // 2. one embedded object with another embedded object
            Store store = (Store) kinto.findAll(Store.class).get(0);
            Author author = store.getBooks().get(0).getAuthor();
            assertTrue(bookWithAuthor.getAuthor().getLastName().length() > 0, "Book attribute contains no values.");

            // 3. an embedded object with a list of kinto objects
            Customer customerWithAddresses = store.getCustomers().get(0);
            List<Address> adressList = customerWithAddresses.getAddressList();
            assertEquals(2, adressList.size());
            for (Address address : adressList) {
                assertFalse(address.getStreet().isEmpty(), "Address has no values");
            }
            // remove all data
            // kinto.clearLocalDataStore();

        } catch (KintoException exception) {
            exception.printStackTrace();
            fail();
        }
    }
}