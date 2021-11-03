package de.hirola.kintojava;

import de.hirola.kintojava.logger.Logger;
import de.hirola.kintojava.logger.LoggerConfiguration;
import de.hirola.kintojava.model.KintoObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TestClass {

    public static void main(String[] args) {

        LoggerConfiguration loggerConfiguration = new LoggerConfiguration.Builder("kintojava-logs")
                .logggingDestination(LoggerConfiguration.LOGGING_DESTINATION.CONSOLE + LoggerConfiguration.LOGGING_DESTINATION.FILE)
                .build();
        Logger logger = Logger.init(loggerConfiguration);
        ArrayList<Class<? extends KintoObject>> typeList = new ArrayList<Class<? extends KintoObject>>();
        typeList.add(User.class);
        typeList.add(Hund.class);
        typeList.add(Katze.class);
        try {
            KintoConfiguration configuration = new KintoConfiguration.Builder("Test")
                    .objectTypes(typeList)
                    .kintoServer("dev.hirola.de")
                    .build();
            Kinto kinto = Kinto.getInstance(configuration);

            Hund hund1 = new Hund();
            hund1.setName("Wuffi");
            hund1.setJahre(5);

            Katze meineKatze = new Katze();
            meineKatze.setName("Lia");
            meineKatze.setJahre(10);

            User user1 = new User();
            user1.setVorname("Hans");
            user1.setName("Wurst");
            user1.setMeineKatze(meineKatze);

            kinto.add(meineKatze);

            kinto.add(user1);

            List<KintoObject> list = kinto.findAll(User.class);
            if (list != null) {
                Iterator<KintoObject> iterator = list.iterator();
                while (iterator.hasNext()) {
                    User user = (User) iterator.next();
                    System.out.println(user.getName());
                }
            }

        } catch (KintoException exception) {
            exception.printStackTrace();
        }

    }
}
