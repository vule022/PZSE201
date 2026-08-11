package rs.medikarton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rs.medikarton.bezbednost.BruteForceDemo;
import rs.medikarton.dogadjaj.RevizijaPosmatrac;
import rs.medikarton.dogadjaj.Subjekat;
import rs.medikarton.dogadjaj.TipDogadjaja;
import rs.medikarton.izuzeci.NalogZakljucanException;
import rs.medikarton.izuzeci.ValidacijaException;
import rs.medikarton.model.Korisnik;
import rs.medikarton.model.Uloga;
import rs.medikarton.servis.AutentifikacijaServis;
import rs.medikarton.util.LozinkaServis;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Autentifikacija i zastita naloga")
class AutentifikacijaServisTest extends OsnovniTest {

    private static final LocalDateTime SADA = LocalDateTime.of(2026, 9, 7, 8, 0);
    private static final String LOZINKA = "Klinika-2026!";

    private AutentifikacijaServis servis;
    private RevizijaPosmatrac revizija;

    @BeforeEach
    void pripremiServis() {
        Subjekat dogadjaji = new Subjekat();
        revizija = new RevizijaPosmatrac();
        dogadjaji.registruj(revizija);
        servis = new AutentifikacijaServis(fabrika.korisnici(), dogadjaji);
    }

    @Test
    @DisplayName("lozinka se nikada ne cuva u citljivom obliku")
    void lozinkaSeNeCuvaOtvoreno() {
        Korisnik k = servis.registruj("ana.jovic", LOZINKA, Uloga.LEKAR);
        assertAll(
                () -> assertNotEquals(LOZINKA, k.getLozinkaHash()),
                () -> assertFalse(k.getLozinkaHash().contains(LOZINKA)),
                () -> assertNotNull(k.getSo()),
                () -> assertTrue(LozinkaServis.brojIteracija() >= 100_000));
    }

    @Test
    @DisplayName("ista lozinka kod dva korisnika daje razlicite hasheve (so)")
    void razliciteSoliDajuRazliciteHasheve() {
        Korisnik prvi = servis.registruj("ana.jovic", LOZINKA, Uloga.LEKAR);
        Korisnik drugi = servis.registruj("marko.ilic", LOZINKA, Uloga.LEKAR);
        assertAll(
                () -> assertNotEquals(prvi.getSo(), drugi.getSo()),
                () -> assertNotEquals(prvi.getLozinkaHash(), drugi.getLozinkaHash()));
    }

    @Test
    void proveraLozinkeRadiIOdbijaPogresnu() {
        String so = LozinkaServis.generisiSo();
        String hash = LozinkaServis.hash(LOZINKA, so);
        assertAll(
                () -> assertTrue(LozinkaServis.proveri(LOZINKA, so, hash)),
                () -> assertFalse(LozinkaServis.proveri("pogresna", so, hash)),
                () -> assertFalse(LozinkaServis.proveri(null, so, hash)),
                () -> assertFalse(LozinkaServis.proveri(LOZINKA, null, hash)),
                () -> assertFalse(LozinkaServis.proveri(LOZINKA, so, null)));
    }

    @Test
    void hashOdbijaPrazneArgumente() {
        String so = LozinkaServis.generisiSo();
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> LozinkaServis.hash("", so)),
                () -> assertThrows(IllegalArgumentException.class, () -> LozinkaServis.hash(null, so)),
                () -> assertThrows(IllegalArgumentException.class, () -> LozinkaServis.hash(LOZINKA, "")),
                () -> assertThrows(IllegalArgumentException.class, () -> LozinkaServis.hash(LOZINKA, null)));
    }

    @Test
    void registracijaOdbijaLosePodatke() {
        servis.registruj("ana.jovic", LOZINKA, Uloga.LEKAR);
        assertAll(
                () -> assertThrows(ValidacijaException.class,
                        () -> servis.registruj("ana.jovic", LOZINKA, Uloga.LEKAR)),
                () -> assertThrows(ValidacijaException.class,
                        () -> servis.registruj("Ana Jovic", LOZINKA, Uloga.LEKAR)),
                () -> assertThrows(ValidacijaException.class,
                        () -> servis.registruj("ab", LOZINKA, Uloga.LEKAR)),
                () -> assertThrows(ValidacijaException.class,
                        () -> servis.registruj("marko", "slaba", Uloga.LEKAR)),
                () -> assertThrows(NullPointerException.class,
                        () -> servis.registruj("marko", LOZINKA, null)));
    }

    @Test
    void prijavaSaTacnimPodacimaUspeva() {
        servis.registruj("ana.jovic", LOZINKA, Uloga.LEKAR);
        assertTrue(servis.prijava("ana.jovic", LOZINKA, SADA).isPresent());
    }

    @Test
    @DisplayName("nepoznat nalog i pogresna lozinka daju isti (prazan) odgovor")
    void neuspelePrijave() {
        servis.registruj("ana.jovic", LOZINKA, Uloga.LEKAR);
        assertAll(
                () -> assertTrue(servis.prijava("ne.postoji", LOZINKA, SADA).isEmpty()),
                () -> assertTrue(servis.prijava("ana.jovic", "pogresna", SADA).isEmpty()),
                () -> assertTrue(servis.prijava(null, LOZINKA, SADA).isEmpty()));
    }

    @Test
    void deaktiviranNalogNeMozeDaSePrijavi() {
        Korisnik k = servis.registruj("ana.jovic", LOZINKA, Uloga.LEKAR);
        k.setAktivan(false);
        fabrika.korisnici().azuriraj(k);
        assertTrue(servis.prijava("ana.jovic", LOZINKA, SADA).isEmpty());
    }

    @Test
    @DisplayName("posle 5 promasaja nalog se zakljucava")
    void zakljucavanjePosle5Promasaja() {
        servis.registruj("ana.jovic", LOZINKA, Uloga.LEKAR);
        for (int i = 0; i < 5; i++) {
            assertTrue(servis.prijava("ana.jovic", "pogresna" + i, SADA).isEmpty());
        }

        NalogZakljucanException greska = assertThrows(NalogZakljucanException.class,
                () -> servis.prijava("ana.jovic", "pogresna6", SADA));
        assertAll(
                () -> assertEquals(SADA.plusMinutes(15), greska.getZakljucanDo()),
                () -> assertEquals(1, revizija.brojPoTipu(TipDogadjaja.NALOG_ZAKLJUCAN)),
                () -> assertEquals(5, revizija.brojPoTipu(TipDogadjaja.NEUSPELA_PRIJAVA)));
    }

    @Test
    @DisplayName("zakljucan nalog odbija i TACNU lozinku dok kazna traje")
    void zakljucanNalogOdbijaITacnuLozinku() {
        servis.registruj("ana.jovic", LOZINKA, Uloga.LEKAR);
        for (int i = 0; i < 5; i++) {
            servis.prijava("ana.jovic", "pogresna", SADA);
        }
        assertThrows(NalogZakljucanException.class, () -> servis.prijava("ana.jovic", LOZINKA, SADA));
    }

    @Test
    @DisplayName("posle isteka 15 minuta prijava ponovo radi")
    void kaznaIstice() {
        servis.registruj("ana.jovic", LOZINKA, Uloga.LEKAR);
        for (int i = 0; i < 5; i++) {
            servis.prijava("ana.jovic", "pogresna", SADA);
        }
        assertAll(
                () -> assertThrows(NalogZakljucanException.class,
                        () -> servis.prijava("ana.jovic", LOZINKA, SADA.plusMinutes(14))),
                () -> assertTrue(servis.prijava("ana.jovic", LOZINKA, SADA.plusMinutes(16)).isPresent()));
    }

    @Test
    @DisplayName("uspesna prijava ponistava brojac promasaja")
    void uspesnaPrijavaResetujeBrojac() {
        servis.registruj("ana.jovic", LOZINKA, Uloga.LEKAR);
        servis.prijava("ana.jovic", "pogresna", SADA);
        servis.prijava("ana.jovic", "pogresna", SADA);
        servis.prijava("ana.jovic", LOZINKA, SADA);

        Korisnik posle = fabrika.korisnici().nadjiPoKorisnickomImenu("ana.jovic").orElseThrow();
        assertEquals(0, posle.getBrojNeuspelihPrijava());
    }

    @Test
    @DisplayName("napad recnikom staje na petom pokusaju, bez obzira na velicinu recnika")
    void napadRecnikomJeZaustavljen() {
        servis.registruj("ana.jovic", LOZINKA, Uloga.LEKAR);
        BruteForceDemo napad = new BruteForceDemo(servis);

        BruteForceDemo.Ishod ishod = napad.napadni("ana.jovic", BruteForceDemo.CEST_RECNIK, SADA);
        assertAll(
                () -> assertTrue(ishod.napadZaustavljen()),
                () -> assertFalse(ishod.pogodjenaLozinka()),
                () -> assertTrue(ishod.nalogZakljucan()),
                () -> assertEquals(5, ishod.iskoriscenoPokusaja()),
                () -> assertEquals(10, ishod.velicinaRecnika()),
                () -> assertTrue(ishod.izvestaj().contains("zakljucan")));
    }

    @Test
    @DisplayName("slaba lozinka iz recnika bi bila pogodjena - zato politika lozinke postoji")
    void slabaLozinkaBiBilaPogodjena() {

        String so = LozinkaServis.generisiSo();
        Korisnik slab = new Korisnik("slab.nalog", LozinkaServis.hash("qwerty", so), so, Uloga.PACIJENT);
        fabrika.korisnici().sacuvaj(slab);

        BruteForceDemo.Ishod ishod = new BruteForceDemo(servis)
                .napadni("slab.nalog", BruteForceDemo.CEST_RECNIK, SADA);
        assertAll(
                () -> assertTrue(ishod.pogodjenaLozinka()),
                () -> assertEquals(3, ishod.iskoriscenoPokusaja(), "qwerty je treci u recniku"),
                () -> assertFalse(ishod.napadZaustavljen()));
    }

    @Test
    @DisplayName("kratak recnik se iscrpi pre nego sto se nalog zakljuca")
    void kratakRecnikSeIscrpi() {
        servis.registruj("ana.jovic", LOZINKA, Uloga.LEKAR);
        BruteForceDemo.Ishod ishod = new BruteForceDemo(servis)
                .napadni("ana.jovic", List.of("aaa", "bbb"), SADA);
        assertAll(
                () -> assertEquals(2, ishod.iskoriscenoPokusaja()),
                () -> assertFalse(ishod.nalogZakljucan()),
                () -> assertFalse(ishod.napadZaustavljen()));
    }

    @Test
    void promenaLozinke() {
        servis.registruj("ana.jovic", LOZINKA, Uloga.LEKAR);
        String nova = "Nova-Lozinka-2026!";

        assertAll(
                () -> assertFalse(servis.promeniLozinku("ana.jovic", "pogresna", nova)),
                () -> assertTrue(servis.promeniLozinku("ana.jovic", LOZINKA, nova)),
                () -> assertTrue(servis.prijava("ana.jovic", nova, SADA).isPresent()),
                () -> assertTrue(servis.prijava("ana.jovic", LOZINKA, SADA).isEmpty()));
    }

    @Test
    void promenaLozinkeOdbijaIstuISlabuLozinku() {
        servis.registruj("ana.jovic", LOZINKA, Uloga.LEKAR);
        assertAll(
                () -> assertThrows(ValidacijaException.class,
                        () -> servis.promeniLozinku("ana.jovic", LOZINKA, LOZINKA)),
                () -> assertThrows(ValidacijaException.class,
                        () -> servis.promeniLozinku("ana.jovic", LOZINKA, "slaba")),
                () -> assertThrows(ValidacijaException.class,
                        () -> servis.promeniLozinku("ne.postoji", LOZINKA, "Nova-2026!x")));
    }

    @Test
    void administrativnoOtkljucavanje() {
        servis.registruj("ana.jovic", LOZINKA, Uloga.LEKAR);
        for (int i = 0; i < 5; i++) {
            servis.prijava("ana.jovic", "pogresna", SADA);
        }
        assertAll(
                () -> assertTrue(servis.otkljucaj("ana.jovic")),
                () -> assertTrue(servis.prijava("ana.jovic", LOZINKA, SADA).isPresent()),
                () -> assertFalse(servis.otkljucaj("ne.postoji")));
    }

    @Test
    void servisOdbijaNullZavisnosti() {
        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> new AutentifikacijaServis(null, new Subjekat())),
                () -> assertThrows(NullPointerException.class,
                        () -> new AutentifikacijaServis(fabrika.korisnici(), null)),
                () -> assertThrows(NullPointerException.class,
                        () -> servis.prijava("ana.jovic", LOZINKA, null)));
    }
}
