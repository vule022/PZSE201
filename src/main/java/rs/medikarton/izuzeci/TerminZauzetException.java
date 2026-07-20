package rs.medikarton.izuzeci;

import java.time.LocalDateTime;

public class TerminZauzetException extends RuntimeException {

    public TerminZauzetException(Integer lekarId, LocalDateTime pocetak) {
        super("Lekar " + lekarId + " vec ima zakazan termin koji se preklapa sa " + pocetak + ".");
    }
}
