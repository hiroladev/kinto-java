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

The library is available from Maven:
```
<dependency>
<groupId>de.hirola</groupId>
<artifactId>kintojava</artifactId>
<version>1.1.1-SNAPSHOT</version>
</dependency>
```

**Sample.java**

```java
    
    import de.hirola.kintojava.model.*;
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

        // create a customer
        Customer customer1 = new Customer();
        customer1.setFirstName("Rick");
        customer1.setLastName("Morris");
        // add a saved kinto object to the customer (1:1 relation)
        customer1.setAddress(address1);

        // add the customer to local datastore
        kinto.add(customer1);

        // create a store
        Store myBookStore = new Store();
        myBookStore.setName("Best Books Store");
        // add a customer to the store
        myBookStore.addCustomer(customer1);

        // add the store to local datastore
        kinto.add(myBookStore);

        // create a book
        Book book1 = new Book();
        book1.setISBN("1-1-1-1-1");
        book1.setTitle("The forrest");
        book1.setPrice(19.00);
        book1.setNumberInStock(100);

        // add the book to local datastore
        kinto.add(book1);

        // add book to the store
        myBookStore.addBook(book1);

        // we have a new book, now we update the store
        kinto.update(myBookStore);

        // show all books
        ArrayList<KintoObject> allBooks = kinto.findAll(Book.class);
        if (allBooks != null) {
            Iterator<KintoObject> iterator = allBooks.iterator();
            while(iterator.hasNext()) {
                Book book = (Book) iterator.next();
                System.out.println(book.getISBN() + " " + book.getTitle());
            }
        }

        // remove the local datastore
        kinto.clearLocalDataStore();

    } catch (KintoException exception) {
        exception.printStackTrace();
    }
}
```


### Notice

The android library from maven ist to old. In Google Maven are only aar files. I created an local (Maven) repo in the root of the project and put the android.jar in. So I can use it for Gradle and Maven.

`mvn deploy:deploy-file -Durl=file:///home/mis/Dev/Java/kinto-java/repo/ -Dfile=/home/mis/Android/Sdk/platforms/android-31/android.jar -DgroupId=android -DartifactId=android -Dpackaging=jar -Dversion=1.0`

![image](https://user-images.githubusercontent.com/48058062/152695845-5258d959-4a8e-4f36-a3d1-74296b64ba7d.png)
