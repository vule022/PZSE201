package rs.medikarton.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Termin {

    private Integer id;
    private Integer pacijentId;
    private Integer lekarId;
    private LocalDateTime pocetak;
    private int trajanjeMin = 20;
    private StatusTermina status = StatusTermina.ZAKAZAN;
    private String razlogDolaska;

    public Termin() {
    }

    public Termin(Integer pacijentId, Integer lekarId, LocalDateTime pocetak,
                  int trajanjeMin, String razlogDolaska) {
        this.pacijentId = pacijentId;
        this.lekarId = lekarId;
        this.pocetak = pocetak;
        this.trajanjeMin = trajanjeMin;
        this.razlogDolaska = razlogDolaska;
    }

    public LocalDateTime kraj() {
        if (pocetak == null) {
            throw new IllegalStateException("Terminu nije postavljen pocetak.");
        }
        return pocetak.plusMinutes(trajanjeMin);
    }

    public boolean preklapaSe(Termin drugi) {
        Objects.requireNonNull(drugi, "Drugi termin ne sme biti null.");
        return pocetak.isBefore(drugi.kraj()) && drugi.getPocetak().isBefore(kraj());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public LocalDateTime getPocetak() {
        return pocetak;
    }

    public void setPocetak(LocalDateTime pocetak) {
        this.pocetak = pocetak;
    }

    public int getTrajanjeMin() {
        return trajanjeMin;
    }

    public void setTrajanjeMin(int trajanjeMin) {
        this.trajanjeMin = trajanjeMin;
    }

    public StatusTermina getStatus() {
        return status;
    }

    public void setStatus(StatusTermina status) {
        this.status = status;
    }

    public String getRazlogDolaska() {
        return razlogDolaska;
    }

    public void setRazlogDolaska(String razlogDolaska) {
        this.razlogDolaska = razlogDolaska;
    }

    @Override
    public boolean equals(Object drugi) {
        if (this == drugi) {
            return true;
        }
        if (!(drugi instanceof Termin t)) {
            return false;
        }
        return Objects.equals(id, t.id) && id != null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Termin{id=" + id + ", pacijent=" + pacijentId + ", lekar=" + lekarId
                + ", pocetak=" + pocetak + ", status=" + status + "}";
    }
}
