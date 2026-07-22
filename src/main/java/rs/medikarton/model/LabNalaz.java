package rs.medikarton.model;

import java.time.LocalDate;
import java.util.Objects;

public class LabNalaz {

    private Integer id;
    private Integer pacijentId;
    private Integer pregledId;
    private String nazivAnalize;
    private double vrednost;
    private String jedinica;
    private double refMin;
    private double refMax;
    private LocalDate datumUzorkovanja;

    public LabNalaz() {
    }

    public LabNalaz(Integer pacijentId, Integer pregledId, String nazivAnalize, double vrednost,
                    String jedinica, double refMin, double refMax, LocalDate datumUzorkovanja) {
        this.pacijentId = pacijentId;
        this.pregledId = pregledId;
        this.nazivAnalize = nazivAnalize;
        this.vrednost = vrednost;
        this.jedinica = jedinica;
        this.refMin = refMin;
        this.refMax = refMax;
        this.datumUzorkovanja = datumUzorkovanja;
    }

    public boolean uReferentnomOpsegu() {
        return vrednost >= refMin && vrednost <= refMax;
    }

    public String oznaka() {
        if (vrednost < refMin) {
            return "L";
        }
        if (vrednost > refMax) {
            return "H";
        }
        return "N";
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

    public Integer getPregledId() {
        return pregledId;
    }

    public void setPregledId(Integer pregledId) {
        this.pregledId = pregledId;
    }

    public String getNazivAnalize() {
        return nazivAnalize;
    }

    public void setNazivAnalize(String nazivAnalize) {
        this.nazivAnalize = nazivAnalize;
    }

    public double getVrednost() {
        return vrednost;
    }

    public void setVrednost(double vrednost) {
        this.vrednost = vrednost;
    }

    public String getJedinica() {
        return jedinica;
    }

    public void setJedinica(String jedinica) {
        this.jedinica = jedinica;
    }

    public double getRefMin() {
        return refMin;
    }

    public void setRefMin(double refMin) {
        this.refMin = refMin;
    }

    public double getRefMax() {
        return refMax;
    }

    public void setRefMax(double refMax) {
        this.refMax = refMax;
    }

    public LocalDate getDatumUzorkovanja() {
        return datumUzorkovanja;
    }

    public void setDatumUzorkovanja(LocalDate datumUzorkovanja) {
        this.datumUzorkovanja = datumUzorkovanja;
    }

    @Override
    public boolean equals(Object drugi) {
        if (this == drugi) {
            return true;
        }
        if (!(drugi instanceof LabNalaz n)) {
            return false;
        }
        return Objects.equals(id, n.id) && id != null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "LabNalaz{" + nazivAnalize + "=" + vrednost + " " + jedinica + " [" + oznaka() + "]}";
    }
}
