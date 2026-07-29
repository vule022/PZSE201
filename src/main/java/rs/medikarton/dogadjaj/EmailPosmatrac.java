package rs.medikarton.dogadjaj;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class EmailPosmatrac implements Posmatrac {

    private static final Set<TipDogadjaja> ZANIMLJIVI = EnumSet.of(
            TipDogadjaja.TERMIN_ZAKAZAN,
            TipDogadjaja.TERMIN_OTKAZAN,
            TipDogadjaja.RECEPT_IZDAT,
            TipDogadjaja.NALAZ_VAN_OPSEGA);

    private final List<String> poslate = new ArrayList<>();

    @Override
    public boolean zanimaMe(TipDogadjaja tip) {
        return ZANIMLJIVI.contains(tip);
    }

    @Override
    public void obavesti(Dogadjaj dogadjaj) {
        if (dogadjaj.primalac() == null || dogadjaj.primalac().isBlank()) {

            return;
        }
        poslate.add("Za: " + dogadjaj.primalac() + " | " + dogadjaj.poruka());
    }

    public List<String> poslate() {
        return List.copyOf(poslate);
    }

    public int brojPoslatih() {
        return poslate.size();
    }
}
