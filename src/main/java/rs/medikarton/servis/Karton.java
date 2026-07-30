package rs.medikarton.servis;

import rs.medikarton.model.LabNalaz;
import rs.medikarton.model.Pacijent;
import rs.medikarton.model.Pregled;
import rs.medikarton.model.Recept;

import java.util.List;

public record Karton(Pacijent pacijent, List<Pregled> pregledi, List<Recept> recepti, List<LabNalaz> nalazi) {

    public Karton {
        pregledi = List.copyOf(pregledi);
        recepti = List.copyOf(recepti);
        nalazi = List.copyOf(nalazi);
    }

    public int brojPregleda() {
        return pregledi.size();
    }

    public List<LabNalaz> nalaziVanOpsega() {
        return nalazi.stream().filter(n -> !n.uReferentnomOpsegu()).toList();
    }

    public String rezime() {
        return pacijent.punoIme() + " - pregleda: " + pregledi.size()
                + ", recepata: " + recepti.size()
                + ", nalaza: " + nalazi.size()
                + (pacijent.imaAlergije() ? " | ALERGIJE: " + pacijent.getAlergije() : "");
    }
}
