package rs.medikarton.dogadjaj;

import java.util.ArrayList;
import java.util.List;

public class RevizijaPosmatrac implements Posmatrac {

    private final List<Dogadjaj> trag = new ArrayList<>();

    @Override
    public void obavesti(Dogadjaj dogadjaj) {
        trag.add(dogadjaj);
    }

    public List<Dogadjaj> trag() {
        return List.copyOf(trag);
    }

    public int brojZapisa() {
        return trag.size();
    }

    public long brojPoTipu(TipDogadjaja tip) {
        return trag.stream().filter(d -> d.tip() == tip).count();
    }

}
