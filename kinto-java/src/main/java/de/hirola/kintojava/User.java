package de.hirola.kintojava;

import de.hirola.kintojava.model.KintoObject;
import de.hirola.kintojava.model.Persisted;

import java.util.ArrayList;

public class User extends KintoObject {
    @Persisted
    private ArrayList<Hund> hunde;
    @Persisted
    private String name;
    @Persisted
    private String vorname;
    @Persisted
    private int jahre;
    @Persisted
    private Katze meineKatze;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVorname() {
        return vorname;
    }

    public void setVorname(String vorname) {
        this.vorname = vorname;
    }

    public int getJahre() {
        return jahre;
    }

    public void setJahre(int jahre) {
        this.jahre = jahre;
    }

    public void addHund( Hund hund) {
        // in list are double allowed
        if (hund != null) {
            if (!hunde.contains(hund)) {
                hunde.add(hund);
            }
        }
    }

    public ArrayList<Hund> getHunde() {
        return hunde;
    }

    public Katze getMeineKatze() {
        return meineKatze;
    }

    public void setMeineKatze(Katze meineKatze) {
        this.meineKatze = meineKatze;
    }
}


class Hund extends KintoObject {
    @Persisted
    private String name;
    @Persisted
    private int jahre;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getJahre() {
        return jahre;
    }

    public void setJahre(int jahre) {
        this.jahre = jahre;
    }
}

class Katze extends KintoObject {
    @Persisted
    private String name;
    @Persisted
    private int jahre;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getJahre() {
        return jahre;
    }

    public void setJahre(int jahre) {
        this.jahre = jahre;
    }
}


