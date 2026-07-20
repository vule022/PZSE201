package rs.medikarton.izuzeci;

public class EntitetNijeNadjenException extends RuntimeException {

    public EntitetNijeNadjenException(String entitet, Object id) {
        super(entitet + " sa identifikatorom " + id + " ne postoji.");
    }
}
