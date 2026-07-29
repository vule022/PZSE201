package rs.medikarton.dogadjaj;

import java.time.LocalDateTime;

public record Dogadjaj(TipDogadjaja tip, String primalac, String poruka, LocalDateTime vreme) {

    public Dogadjaj {
        if (tip == null) {
            throw new IllegalArgumentException("Tip dogadjaja je obavezan.");
        }
        if (vreme == null) {
            throw new IllegalArgumentException("Vreme dogadjaja je obavezno.");
        }
    }

    public String zaDnevnik() {
        return "[" + vreme + "] " + tip + " -> " + (primalac == null ? "-" : primalac) + ": " + poruka;
    }
}
