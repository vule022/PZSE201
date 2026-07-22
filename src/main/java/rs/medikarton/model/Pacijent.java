package rs.medikarton.model;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

public class Pacijent {

    private Integer id;
    private String jmbg;
    private String ime;
    private String prezime;
    private LocalDate datumRodjenja;
    private String pol;
    private String email;
    private String telefon;
    private String adresa;
    private String krvnaGrupa;
    private String alergije;

    public Pacijent() {
    }

    public Pacijent(String jmbg, String ime, String prezime, LocalDate datumRodjenja,
                    String pol, String email, String telefon) {
        this.jmbg = jmbg;
        this.ime = ime;
        this.prezime = prezime;
        this.datumRodjenja = datumRodjenja;
        this.pol = pol;
        this.email = email;
        this.telefon = telefon;
        this.alergije = "";
    }

    public String punoIme() {
        return ime + " " + prezime;
    }

    //racunamo starost pacijenta
    public int starost(LocalDate naDan) {
        Objects.requireNonNull(naDan, "Datum za racunanje starosti ne sme biti null.");
        if (datumRodjenja == null) {
            throw new IllegalStateException("Pacijentu nije postavljen datum rodjenja.");
        }
        if (naDan.isBefore(datumRodjenja)) {
            throw new IllegalArgumentException("Datum ne sme biti pre datuma rodjenja.");
        }
        return Period.between(datumRodjenja, naDan).getYears();
    }

    //proveravamo na sta je pacijent alergican
    public boolean imaAlergije() {
        return alergije != null && !alergije.isBlank();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getJmbg() {
        return jmbg;
    }

    public void setJmbg(String jmbg) {
        this.jmbg = jmbg;
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

    public LocalDate getDatumRodjenja() {
        return datumRodjenja;
    }

    public void setDatumRodjenja(LocalDate datumRodjenja) {
        this.datumRodjenja = datumRodjenja;
    }

    public String getPol() {
        return pol;
    }

    public void setPol(String pol) {
        this.pol = pol;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefon() {
        return telefon;
    }

    public void setTelefon(String telefon) {
        this.telefon = telefon;
    }

    public String getAdresa() {
        return adresa;
    }

    public void setAdresa(String adresa) {
        this.adresa = adresa;
    }

    public String getKrvnaGrupa() {
        return krvnaGrupa;
    }

    public void setKrvnaGrupa(String krvnaGrupa) {
        this.krvnaGrupa = krvnaGrupa;
    }

    public String getAlergije() {
        return alergije;
    }

    public void setAlergije(String alergije) {
        this.alergije = alergije;
    }

    @Override
    public boolean equals(Object drugi) {
        if (this == drugi) {
            return true;
        }
        if (!(drugi instanceof Pacijent p)) {
            return false;
        }
        return jmbg != null && Objects.equals(jmbg, p.jmbg);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jmbg);
    }

    @Override
    public String toString() {
        return "Pacijent{id=" + id + ", " + punoIme() + ", jmbg=" + jmbg + "}";
    }
}
