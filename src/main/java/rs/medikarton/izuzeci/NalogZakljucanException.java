package rs.medikarton.izuzeci;

import java.time.LocalDateTime;

public class NalogZakljucanException extends RuntimeException {

    private final LocalDateTime zakljucanDo;

    public NalogZakljucanException(LocalDateTime zakljucanDo) {
        super("Nalog je privremeno zakljucan do " + zakljucanDo + ".");
        this.zakljucanDo = zakljucanDo;
    }

    public LocalDateTime getZakljucanDo() {
        return zakljucanDo;
    }
}
