package rs.medikarton.dogadjaj;

@FunctionalInterface
public interface Posmatrac {

    void obavesti(Dogadjaj dogadjaj);

    default boolean zanimaMe(TipDogadjaja tip) {
        return true;
    }
}
