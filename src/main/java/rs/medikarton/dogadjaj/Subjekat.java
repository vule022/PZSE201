package rs.medikarton.dogadjaj;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Subjekat {

    private final List<Posmatrac> posmatraci = new ArrayList<>();
    private final List<String> greske = new ArrayList<>();

    public void registruj(Posmatrac posmatrac) {
        Objects.requireNonNull(posmatrac, "Posmatrac ne sme biti null.");
        if (!posmatraci.contains(posmatrac)) {
            posmatraci.add(posmatrac);
        }
    }

    public void odjavi(Posmatrac posmatrac) {
        posmatraci.remove(posmatrac);
    }

    public int brojPosmatraca() {
        return posmatraci.size();
    }

    public void objavi(Dogadjaj dogadjaj) {
        Objects.requireNonNull(dogadjaj, "Dogadjaj ne sme biti null.");
        for (Posmatrac posmatrac : List.copyOf(posmatraci)) {
            if (!posmatrac.zanimaMe(dogadjaj.tip())) {
                continue;
            }
            try {
                posmatrac.obavesti(dogadjaj);
            } catch (RuntimeException e) {
                greske.add(posmatrac.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    public List<String> greske() {
        return List.copyOf(greske);
    }

    public void ocistiGreske() {
        greske.clear();
    }
}
