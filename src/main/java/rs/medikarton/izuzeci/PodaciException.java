package rs.medikarton.izuzeci;

public class PodaciException extends RuntimeException {

    public PodaciException(String poruka, Throwable uzrok) {
        super(poruka, uzrok);
    }
}
