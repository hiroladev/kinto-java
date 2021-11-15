package de.hirola.kintojava.bookstore;

import de.hirola.kintojava.Kinto;
import de.hirola.kintojava.KintoConfiguration;
import de.hirola.kintojava.KintoException;
import de.hirola.kintojava.logger.Logger;
import de.hirola.kintojava.logger.LoggerConfiguration;
import de.hirola.kintojava.model.KintoObject;

import java.util.ArrayList;

public class StoreTest {

    public static void main(String[] args) {

        // create the library logger
        LoggerConfiguration loggerConfiguration = new LoggerConfiguration.Builder("kintojava-logs")
                .logggingDestination(LoggerConfiguration.LOGGING_DESTINATION.CONSOLE + LoggerConfiguration.LOGGING_DESTINATION.FILE)
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
}
