package rs.medikarton.model;

public enum Uloga {
    PACIJENT,
    LEKAR,
    RECEPCIONER,
    ADMIN;

    public static Uloga izTeksta(String vrednost) {
        if (vrednost == null) {
            throw new IllegalArgumentException("Uloga ne sme biti null.");
        }
        return Uloga.valueOf(vrednost.trim().toUpperCase());
    }
}
