package rs.medikarton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rs.medikarton.bezbednost.RanjiviPacijentDao;
import rs.medikarton.bezbednost.SqlInjectionDemo;
import rs.medikarton.dogadjaj.Subjekat;
import rs.medikarton.model.Uloga;
import rs.medikarton.servis.AutentifikacijaServis;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SQL injection - napad i odbrana")
class SqlInjectionTest extends OsnovniTest {

    private SqlInjectionDemo demo;
    private RanjiviPacijentDao ranjivi;

    @BeforeEach
    void pripremiPodatke() {
        dodajPacijenta(JMBG_NIKOLA, "Nikola", "Petrovic");
        dodajPacijenta(JMBG_MILICA, "Milica", "Stankovic");
        dodajPacijenta(JMBG_DRAGAN, "Dragan", "Nikolic");

        AutentifikacijaServis autentifikacija =
                new AutentifikacijaServis(fabrika.korisnici(), new Subjekat());
        autentifikacija.registruj("ana.jovic", "Klinika-2026!", Uloga.LEKAR);
        autentifikacija.registruj("admin", "Admin-Lozinka-1!", Uloga.ADMIN);

        demo = new SqlInjectionDemo(veza);
        ranjivi = new RanjiviPacijentDao(veza);
    }

    @Test
    @DisplayName("tautologija: ranjivi upit vraca ceo registar, bezbedni nijedan red")
    void tautologija() {
        SqlInjectionDemo.Ishod ishod = demo.tautologija();
        assertAll(
                () -> assertEquals(3, ishod.ukupnoUBazi()),
                () -> assertEquals(3, ishod.redovaRanjivo(), "napadac je procitao sve pacijente"),
                () -> assertEquals(0, ishod.redovaBezbedno(), "pripremljen upit trazi bukvalno to prezime"),
                () -> assertTrue(ishod.napadUspeo()),
                () -> assertTrue(ishod.izvestaj().contains("NAPAD USPEO")));
    }

    @Test
    @DisplayName("UNION: kroz ekran za pacijente napadac cita tabelu naloga")
    void unionNapad() {
        SqlInjectionDemo.Ishod ishod = demo.union();
        List<String> procitano = demo.procitaniNaloziUnionNapadom();

        assertAll(
                () -> assertEquals(2, ishod.redovaRanjivo(), "dva naloga iz tabele korisnik"),
                () -> assertEquals(0, ishod.redovaBezbedno()),
                () -> assertTrue(ishod.napadUspeo()),
                () -> assertTrue(procitano.stream().anyMatch(r -> r.contains("ana.jovic"))),
                () -> assertTrue(procitano.stream().anyMatch(r -> r.contains("ADMIN"))));
    }

    @Test
    @DisplayName("komentar: napadac se prijavljuje kao admin bez znanja lozinke")
    void zaobilazenjePrijaveKomentarom() {
        assertAll(
                () -> assertTrue(demo.prijavaBezLozinkeProlaziKrozRanjivu("admin"),
                        "ranjiva prijava propusta napadaca"),
                () -> assertFalse(demo.prijavaBezLozinkeProlaziKrozBezbednu("admin"),
                        "bezbedna prijava trazi nalog imena \"admin' --\", koji ne postoji"),
                () -> assertFalse(ranjivi.prijava("admin", "pogresan-hash"),
                        "bez napada ni ranjiva verzija ne pusta pogresnu lozinku"));
    }

    @Test
    @DisplayName("bezbedni upit i dalje normalno radi za obican unos")
    void bezbedniUpitRadiZaObicanUnos() {
        assertAll(
                () -> assertEquals(1, fabrika.pacijenti().pretraziPoPrezimenu("Petrovic").size()),
                () -> assertEquals(1, fabrika.pacijenti().pretraziPoPrezimenu("Nikolic").size()),
                () -> assertEquals(3, ranjivi.pretraziPoPrezimenu("").size()));
    }

    @Test
    @DisplayName("apostrof u stvarnom prezimenu ne lomi bezbedni upit")
    void apostrofUPodacimaNijeProblem() {
        var pacijent = dodajPacijenta("1809002725085", "Sara", "D'Angelo");
        assertAll(
                () -> assertEquals(1, fabrika.pacijenti().pretraziPoPrezimenu("D'Angelo").size()),
                () -> assertEquals("D'Angelo",
                        fabrika.pacijenti().nadjiPoId(pacijent.getId()).orElseThrow().getPrezime()));
    }

    @Test
    @DisplayName("izvestaj obuhvata sva tri napada")
    void punIzvestaj() {
        String izvestaj = demo.izvestaj("admin");
        assertAll(
                () -> assertTrue(izvestaj.contains("Tautologija")),
                () -> assertTrue(izvestaj.contains("UNION")),
                () -> assertTrue(izvestaj.contains("Komentar")),
                () -> assertTrue(izvestaj.contains("PRIJAVLJEN BEZ LOZINKE")));
    }
}
