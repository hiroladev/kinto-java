package de.hirola.kintojava.bookstore;

import de.hirola.kintojava.Kinto;
import de.hirola.kintojava.KintoConfiguration;
import de.hirola.kintojava.KintoException;
import de.hirola.kintojava.logger.KintoLogger;
import de.hirola.kintojava.logger.LogEntry;
import de.hirola.kintojava.model.KintoObject;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LibraryTest {

    @Test
    void dataStoreTests() {
        // list of books
        Book book1 = new Book("4711","Test Book",10.99,100);
        Book book2 = new Book("0815","The 08/15 gun",7.99,50);
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

            // remove all data
            kinto.clearLocalDataStore();

        } catch (KintoException exception) {
            exception.printStackTrace();
        }
    }

}