package rs.medikarton.bezbednost;

import rs.medikarton.izuzeci.NalogZakljucanException;
import rs.medikarton.model.Korisnik;
import rs.medikarton.servis.AutentifikacijaServis;

import java.time.LocalDateTime;
import java.util.List;

public class BruteForceDemo {

    public static final List<String> CEST_RECNIK = List.of(
            "123456", "password", "qwerty", "admin", "letmein", "111111",
            "sifra123", "Pacijent1!", "Klinika2026!", "welcome");

    private final AutentifikacijaServis autentifikacija;

    public BruteForceDemo(AutentifikacijaServis autentifikacija) {
        this.autentifikacija = autentifikacija;
    }

    public record Ishod(int iskoriscenoPokusaja, int velicinaRecnika,
                        boolean pogodjenaLozinka, boolean nalogZakljucan) {

        public boolean napadZaustavljen() {
            return nalogZakljucan && !pogodjenaLozinka;
        }

        public String izvestaj() {
            return "Probano " + iskoriscenoPokusaja + " od " + velicinaRecnika + " lozinki; "
                    + (pogodjenaLozinka ? "lozinka POGODJENA" : "lozinka nije pogodjena")
                    + (nalogZakljucan ? "; nalog zakljucan posle "
                            + Korisnik.MAX_NEUSPELIH_PRIJAVA + " promasaja" : "");
        }
    }

    public Ishod napadni(String korisnickoIme, List<String> recnik, LocalDateTime sada) {
        int pokusaja = 0;
        for (String kandidat : recnik) {
            pokusaja++;
            try {
                if (autentifikacija.prijava(korisnickoIme, kandidat, sada).isPresent()) {
                    return new Ishod(pokusaja, recnik.size(), true, false);
                }
            } catch (NalogZakljucanException e) {
                return new Ishod(pokusaja - 1, recnik.size(), false, true);
            }
        }
        return new Ishod(pokusaja, recnik.size(), false, false);
    }
}
