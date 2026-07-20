package rs.medikarton.model;

import java.time.LocalDate;
import java.util.Objects;

public class Recept {

    private Integer id;
    private Integer pregledId;
    private String nazivLeka;
    private String atcSifra;
    private String doziranje;
    private int brojPakovanja = 1;
    private LocalDate datumIzdavanja;
    private LocalDate vaziDo;

    public Recept() {
    }

    public Recept(Integer pregledId, String nazivLeka, String atcSifra, String doziranje,
                  int brojPakovanja, LocalDate datumIzdavanja, LocalDate vaziDo) {
        this.pregledId = pregledId;
        this.nazivLeka = nazivLeka;
        this.atcSifra = atcSifra;
        this.doziranje = doziranje;
        this.brojPakovanja = brojPakovanja;
        this.datumIzdavanja = datumIzdavanja;
        this.vaziDo = vaziDo;
    }

    public boolean vaziNaDan(LocalDate dan) {
        Objects.requireNonNull(dan, "Dan ne sme biti null.");
        if (datumIzdavanja == null || vaziDo == null) {
            throw new IllegalStateException("Receptu nisu postavljeni datumi vazenja.");
        }
        return !dan.isBefore(datumIzdavanja) && !dan.isAfter(vaziDo);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getPregledId() {
        return pregledId;
    }

    public void setPregledId(Integer pregledId) {
        this.pregledId = pregledId;
    }

    public String getNazivLeka() {
        return nazivLeka;
    }

    public void setNazivLeka(String nazivLeka) {
        this.nazivLeka = nazivLeka;
    }

    public String getAtcSifra() {
        return atcSifra;
    }

    public void setAtcSifra(String atcSifra) {
        this.atcSifra = atcSifra;
    }

    public String getDoziranje() {
        return doziranje;
    }

    public void setDoziranje(String doziranje) {
        this.doziranje = doziranje;
    }

    public int getBrojPakovanja() {
        return brojPakovanja;
    }

    public void setBrojPakovanja(int brojPakovanja) {
        this.brojPakovanja = brojPakovanja;
    }

    public LocalDate getDatumIzdavanja() {
        return datumIzdavanja;
    }

    public void setDatumIzdavanja(LocalDate datumIzdavanja) {
        this.datumIzdavanja = datumIzdavanja;
    }

    public LocalDate getVaziDo() {
        return vaziDo;
    }

    public void setVaziDo(LocalDate vaziDo) {
        this.vaziDo = vaziDo;
    }

    @Override
    public boolean equals(Object drugi) {
        if (this == drugi) {
            return true;
        }
        if (!(drugi instanceof Recept r)) {
            return false;
        }
        return Objects.equals(id, r.id) && id != null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Recept{id=" + id + ", lek=" + nazivLeka + " (" + atcSifra + "), vaziDo=" + vaziDo + "}";
    }
}
