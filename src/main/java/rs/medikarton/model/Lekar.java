package rs.medikarton.model;

import java.util.Objects;

public class Lekar {

    private Integer id;
    private String brojLicence;
    private String ime;
    private String prezime;
    private String specijalizacija;
    private String email;

    private int trajanjePregledaMin = 20;

    public Lekar() {
    }

    public Lekar(String brojLicence, String ime, String prezime, String specijalizacija, String email) {
        this.brojLicence = brojLicence;
        this.ime = ime;
        this.prezime = prezime;
        this.specijalizacija = specijalizacija;
        this.email = email;
    }

    public String punoIme() {
        return "dr " + ime + " " + prezime;
    }

    public String prikazSaSpecijalizacijom() {
        return punoIme() + " (" + specijalizacija + ")";
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getBrojLicence() {
        return brojLicence;
    }

    public void setBrojLicence(String brojLicence) {
        this.brojLicence = brojLicence;
    }

    public String getIme() {
        return ime;
    }

    public void setIme(String ime) {
        this.ime = ime;
    }

    public String getPrezime() {
        return prezime;
    }

    public void setPrezime(String prezime) {
        this.prezime = prezime;
    }

    public String getSpecijalizacija() {
        return specijalizacija;
    }

    public void setSpecijalizacija(String specijalizacija) {
        this.specijalizacija = specijalizacija;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getTrajanjePregledaMin() {
        return trajanjePregledaMin;
    }

    public void setTrajanjePregledaMin(int trajanjePregledaMin) {
        this.trajanjePregledaMin = trajanjePregledaMin;
    }

    @Override
    public boolean equals(Object drugi) {
        if (this == drugi) {
            return true;
        }
        if (!(drugi instanceof Lekar l)) {
            return false;
        }
        return brojLicence != null && Objects.equals(brojLicence, l.brojLicence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(brojLicence);
    }

    @Override
    public String toString() {
        return "Lekar{id=" + id + ", " + prikazSaSpecijalizacijom() + "}";
    }
}
