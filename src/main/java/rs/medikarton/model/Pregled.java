package rs.medikarton.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Pregled {

    private Integer id;
    private Integer terminId;
    private Integer pacijentId;
    private Integer lekarId;
    private LocalDateTime datumVreme;
    private String anamneza;
    private String dijagnozaMkb;
    private String terapija;

    public Pregled() {
    }

    public Pregled(Integer terminId, Integer pacijentId, Integer lekarId,
                   LocalDateTime datumVreme, String anamneza, String dijagnozaMkb, String terapija) {
        this.terminId = terminId;
        this.pacijentId = pacijentId;
        this.lekarId = lekarId;
        this.datumVreme = datumVreme;
        this.anamneza = anamneza;
        this.dijagnozaMkb = dijagnozaMkb;
        this.terapija = terapija;
    }

    public String kategorijaDijagnoze() {
        if (dijagnozaMkb == null || dijagnozaMkb.isBlank()) {
            return "?";
        }
        return dijagnozaMkb.substring(0, 1).toUpperCase();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTerminId() {
        return terminId;
    }

    public void setTerminId(Integer terminId) {
        this.terminId = terminId;
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

    public LocalDateTime getDatumVreme() {
        return datumVreme;
    }

    public void setDatumVreme(LocalDateTime datumVreme) {
        this.datumVreme = datumVreme;
    }

    public String getAnamneza() {
        return anamneza;
    }

    public void setAnamneza(String anamneza) {
        this.anamneza = anamneza;
    }

    public String getDijagnozaMkb() {
        return dijagnozaMkb;
    }

    public void setDijagnozaMkb(String dijagnozaMkb) {
        this.dijagnozaMkb = dijagnozaMkb;
    }

    public String getTerapija() {
        return terapija;
    }

    public void setTerapija(String terapija) {
        this.terapija = terapija;
    }

    @Override
    public boolean equals(Object drugi) {
        if (this == drugi) {
            return true;
        }
        if (!(drugi instanceof Pregled p)) {
            return false;
        }
        return Objects.equals(id, p.id) && id != null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Pregled{id=" + id + ", pacijent=" + pacijentId + ", mkb=" + dijagnozaMkb
                + ", datum=" + datumVreme + "}";
    }
}
