package rs.medikarton.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Korisnik {

    public static final int MAX_NEUSPELIH_PRIJAVA = 5;
    public static final int MINUTA_ZAKLJUCAVANJA = 15;

    private Integer id;
    private String korisnickoIme;
    private String lozinkaHash;
    private String so;
    private Uloga uloga;
    private boolean aktivan = true;
    private int brojNeuspelihPrijava;
    private LocalDateTime zakljucanDo;
    private Integer pacijentId;
    private Integer lekarId;

    public Korisnik() {
    }

    public Korisnik(String korisnickoIme, String lozinkaHash, String so, Uloga uloga) {
        this.korisnickoIme = korisnickoIme;
        this.lozinkaHash = lozinkaHash;
        this.so = so;
        this.uloga = uloga;
    }

    public boolean jeZakljucan(LocalDateTime sada) {
        Objects.requireNonNull(sada, "Trenutno vreme ne sme biti null.");
        return zakljucanDo != null && sada.isBefore(zakljucanDo);
    }

    public void zabeleziNeuspeluPrijavu(LocalDateTime sada) {
        Objects.requireNonNull(sada, "Trenutno vreme ne sme biti null.");
        brojNeuspelihPrijava++;
        if (brojNeuspelihPrijava >= MAX_NEUSPELIH_PRIJAVA) {
            zakljucanDo = sada.plusMinutes(MINUTA_ZAKLJUCAVANJA);
        }
    }

    public void resetujBrojacPrijava() {
        brojNeuspelihPrijava = 0;
        zakljucanDo = null;
    }

    public int preostaloPokusaja() {
        return Math.max(0, MAX_NEUSPELIH_PRIJAVA - brojNeuspelihPrijava);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getKorisnickoIme() {
        return korisnickoIme;
    }

    public void setKorisnickoIme(String korisnickoIme) {
        this.korisnickoIme = korisnickoIme;
    }

    public String getLozinkaHash() {
        return lozinkaHash;
    }

    public void setLozinkaHash(String lozinkaHash) {
        this.lozinkaHash = lozinkaHash;
    }

    public String getSo() {
        return so;
    }

    public void setSo(String so) {
        this.so = so;
    }

    public Uloga getUloga() {
        return uloga;
    }

    public void setUloga(Uloga uloga) {
        this.uloga = uloga;
    }

    public boolean isAktivan() {
        return aktivan;
    }

    public void setAktivan(boolean aktivan) {
        this.aktivan = aktivan;
    }

    public int getBrojNeuspelihPrijava() {
        return brojNeuspelihPrijava;
    }

    public void setBrojNeuspelihPrijava(int brojNeuspelihPrijava) {
        this.brojNeuspelihPrijava = brojNeuspelihPrijava;
    }

    public LocalDateTime getZakljucanDo() {
        return zakljucanDo;
    }

    public void setZakljucanDo(LocalDateTime zakljucanDo) {
        this.zakljucanDo = zakljucanDo;
    }

    public Integer getPacijentId() {
        return pacijentId;
    }

    public void setPacijentId(Integer pacijentId) {
        this.pacijentId = pacijentId;
    }

    public Integer getLekarId() {
        return lekarId;
    }

    public void setLekarId(Integer lekarId) {
        this.lekarId = lekarId;
    }

    @Override
    public boolean equals(Object drugi) {
        if (this == drugi) {
            return true;
        }
        if (!(drugi instanceof Korisnik k)) {
            return false;
        }
        return korisnickoIme != null && Objects.equals(korisnickoIme, k.korisnickoIme);
    }

    @Override
    public int hashCode() {
        return Objects.hash(korisnickoIme);
    }

    @Override
    public String toString() {
        return "Korisnik{id=" + id + ", korisnickoIme=" + korisnickoIme + ", uloga=" + uloga + "}";
    }
}
