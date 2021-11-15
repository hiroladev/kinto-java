# kinto-java
A java library to use with [kinto](https://github.com/Kinto/kinto).

- simple OR-Mapper
- to use on android (mobile) and desktop
- use and save data on different devices, sync with a server / central database
- Offline-First-Strategy / local storage with sqlite
- simple the last (newest) update wins strategy
- simple auth with user and password
- Logging to file and (remote) in kinto bucket

Usage
============

You can find a sample in package de.hirola.kintojava.bookstore.StoreTest.

1.  Download the library kinto-jar-(VERSION).jar from XX and append the jar to the classpath.
2.  Download the library sqlite-jdbc-(VERSION).jar from the [download page](https://github.com/xerial/sqlite-jdbc/releases) and add the file to the classpath.


**Sample.java**

```java
    
    import de.hirola.kintojava.model.*;
    
    // create the classes for yor app
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
      ... some funcs
    }
    
    public class Customer extends KintoObject {
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
    }
    
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
      ... some funcs
    }
    
    // create the library logger
    LoggerConfiguration loggerConfiguration = new LoggerConfiguration.Builder("kintojava-logs")
          .logggingDestination(LoggerConfiguration.LOGGING_DESTINATION.CONSOLE 
                              + LoggerConfiguration.LOGGING_DESTINATION.FILE)
          .build();
    Logger logger = Logger.init(loggerConfiguration);
    
    // add all types for managing by kinto java
    ArrayList<Class<? extends KintoObject>> typeList = new ArrayList<>();
    typeList.add(Address.class);
    typeList.add(Book.class);
    typeList.add(Customer.class);
    typeList.add(Store.class);
    
    try {
        // create a kinto java configuration
        KintoConfiguration configuration = new KintoConfiguration.Builder("StoreTest")
                .objectTypes(typeList)
                .build();
                
        // create the kinto java instance
        Kinto kinto = Kinto.getInstance(configuration);

        // create address samples
        Address address1 = new Address();
        address1.setStreet("Old Street");
        address1.setNumber(5);
        address1.setPlace("New Town");
        address1.setPostalCode("12345");
        Address address2 = new Address();
        address2.setStreet("New Street");
        address2.setNumber(50);
        address2.setPlace("Sample Town");
        address2.setPostalCode("47110");

        // add the addresses to local datastore
        kinto.add(address1);
        kinto.add(address2);

      } catch (KintoException exception) {
          exception.printStackTrace();
      }
  }
  
  // create a customer
  Customer customer1 = new Customer();
  customer1.setFirstName("Rick");
  customer1.setLastName("Morris");
  // add a saved kinto object to the customer (1:1 relation)
  customer1.setAddress(address1);

  // add the customer to local datastore
  kinto.add(customer1);
  
  // create a book
  Book book1 = new Book();
  book1.setIsbn("1-1-1-1-1");
  book1.setTitle("The forrest");
  book1.setPrice(19.00);
  // attribute without saving in local datastore
  book1.setNumberInStock(100);

  // add the book to local datastore
  kinto.add(book1);

  // add book to the store
  myBookStore.addBook(book1);

  // update store
  kinto.update(myBookStore);

```    

