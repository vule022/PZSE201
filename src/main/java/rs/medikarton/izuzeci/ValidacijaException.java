package rs.medikarton.izuzeci;

public class ValidacijaException extends RuntimeException {

    private final String polje;

    public ValidacijaException(String polje, String poruka) {
        super(poruka);
        this.polje = polje;
    }

    public String getPolje() {
        return polje;
    }

    @Override
    public String getMessage() {
        return "[" + polje + "] " + super.getMessage();
    }
}
