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
        ArrayList<Class<? extends KintoObject>> typeList = new ArrayList<>();
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
            user1.addHund(hund1);

            kinto.add(meineKatze);

            kinto.add(hund1);

            kinto.add(user1);

            List<? extends KintoObject> list = kinto.findAll(User.class);
            if (list != null) {
                for (KintoObject kintoObject : list) {
                    User user = (User) kintoObject;
                    if (user != null) {
                        System.out.println(user.getMeineKatze().getName());
                        ArrayList<? extends KintoObject> hunde = user.getHunde();
                        if (hunde != null) {
                            if (hunde.size() > 0) {
                                Hund ersterHund = (Hund) hunde.get(0);
                                System.out.println(ersterHund.getName());
                            }

                        }
                    }
                }
            }

        } catch (KintoException exception) {
            exception.printStackTrace();
        }

    }
}
