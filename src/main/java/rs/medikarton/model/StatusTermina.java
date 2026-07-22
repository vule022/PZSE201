package rs.medikarton.model;

public enum StatusTermina {
    ZAKAZAN,
    POTVRDJEN,
    OTKAZAN,
    REALIZOVAN;

    public boolean moguceOtkazati() {
        return this == ZAKAZAN || this == POTVRDJEN;
    }

    public boolean moguceRealizovati() {
        return this == ZAKAZAN || this == POTVRDJEN;
    }

    public static StatusTermina izTeksta(String vrednost) {
        if (vrednost == null) {
            throw new IllegalArgumentException("Status termina ne sme biti null.");
        }
        return StatusTermina.valueOf(vrednost.trim().toUpperCase());
    }
}
