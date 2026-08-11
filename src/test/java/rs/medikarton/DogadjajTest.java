package rs.medikarton;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rs.medikarton.dogadjaj.Dogadjaj;
import rs.medikarton.dogadjaj.EmailPosmatrac;
import rs.medikarton.dogadjaj.Posmatrac;
import rs.medikarton.dogadjaj.RevizijaPosmatrac;
import rs.medikarton.dogadjaj.Subjekat;
import rs.medikarton.dogadjaj.TipDogadjaja;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Obavestenja (sablon Observer)")
class DogadjajTest {

    private static final LocalDateTime SADA = LocalDateTime.of(2026, 9, 7, 8, 0);

    private Dogadjaj dogadjaj(TipDogadjaja tip) {
        return new Dogadjaj(tip, "pacijent@example.rs", "poruka", SADA);
    }

    @Test
    void dogadjajZahtevaTipIVreme() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Dogadjaj(null, "x", "y", SADA)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new Dogadjaj(TipDogadjaja.TERMIN_ZAKAZAN, "x", "y", null)));
    }

    @Test
    void zapisZaDnevnikSadrziSveDelove() {
        String zapis = dogadjaj(TipDogadjaja.TERMIN_ZAKAZAN).zaDnevnik();
        assertAll(
                () -> assertTrue(zapis.contains("TERMIN_ZAKAZAN")),
                () -> assertTrue(zapis.contains("pacijent@example.rs")),
                () -> assertTrue(zapis.contains("poruka")),
                () -> assertTrue(new Dogadjaj(TipDogadjaja.NALOG_ZAKLJUCAN, null, "p", SADA)
                        .zaDnevnik().contains("-")));
    }

    @Test
    @DisplayName("svi registrovani posmatraci dobijaju dogadjaj")
    void obavestavanjeSvih() {
        Subjekat subjekat = new Subjekat();
        RevizijaPosmatrac revizija = new RevizijaPosmatrac();
        EmailPosmatrac email = new EmailPosmatrac();
        subjekat.registruj(revizija);
        subjekat.registruj(email);

        subjekat.objavi(dogadjaj(TipDogadjaja.TERMIN_ZAKAZAN));

        assertAll(
                () -> assertEquals(2, subjekat.brojPosmatraca()),
                () -> assertEquals(1, revizija.brojZapisa()),
                () -> assertEquals(1, email.brojPoslatih()));
    }

    @Test
    @DisplayName("isti posmatrac se ne registruje dvaput")
    void dvostrukaRegistracija() {
        Subjekat subjekat = new Subjekat();
        RevizijaPosmatrac revizija = new RevizijaPosmatrac();
        subjekat.registruj(revizija);
        subjekat.registruj(revizija);

        subjekat.objavi(dogadjaj(TipDogadjaja.TERMIN_ZAKAZAN));
        assertAll(
                () -> assertEquals(1, subjekat.brojPosmatraca()),
                () -> assertEquals(1, revizija.brojZapisa()));
    }

    @Test
    void odjavljenPosmatracViseNeDobijaDogadjaje() {
        Subjekat subjekat = new Subjekat();
        RevizijaPosmatrac revizija = new RevizijaPosmatrac();
        subjekat.registruj(revizija);
        subjekat.objavi(dogadjaj(TipDogadjaja.TERMIN_ZAKAZAN));
        subjekat.odjavi(revizija);
        subjekat.objavi(dogadjaj(TipDogadjaja.TERMIN_OTKAZAN));

        assertAll(
                () -> assertEquals(1, revizija.brojZapisa()),
                () -> assertEquals(0, subjekat.brojPosmatraca()));
    }

    @Test
    @DisplayName("e-posta ignorise bezbednosne dogadjaje i primaoce bez adrese")
    void filterKanalaEposte() {
        Subjekat subjekat = new Subjekat();
        EmailPosmatrac email = new EmailPosmatrac();
        RevizijaPosmatrac revizija = new RevizijaPosmatrac();
        subjekat.registruj(email);
        subjekat.registruj(revizija);

        subjekat.objavi(dogadjaj(TipDogadjaja.TERMIN_ZAKAZAN));
        subjekat.objavi(dogadjaj(TipDogadjaja.NEUSPELA_PRIJAVA));
        subjekat.objavi(new Dogadjaj(TipDogadjaja.RECEPT_IZDAT, null, "bez adrese", SADA));
        subjekat.objavi(new Dogadjaj(TipDogadjaja.RECEPT_IZDAT, "  ", "prazna adresa", SADA));

        assertAll(
                () -> assertEquals(1, email.brojPoslatih()),
                () -> assertEquals(4, revizija.brojZapisa(), "revizija belezi bas sve"),
                () -> assertTrue(email.poslate().get(0).contains("pacijent@example.rs")));
    }

    @Test
    @DisplayName("kvar jednog posmatraca ne prekida obavestavanje ostalih")
    void kvarPosmatracaNeRusiTok() {
        Subjekat subjekat = new Subjekat();
        RevizijaPosmatrac revizija = new RevizijaPosmatrac();
        Posmatrac pokvaren = d -> {
            throw new IllegalStateException("SMS provajder nedostupan");
        };

        subjekat.registruj(pokvaren);
        subjekat.registruj(revizija);
        subjekat.objavi(dogadjaj(TipDogadjaja.TERMIN_ZAKAZAN));

        assertAll(
                () -> assertEquals(1, revizija.brojZapisa(), "drugi posmatrac je ipak obavesten"),
                () -> assertEquals(1, subjekat.greske().size()),
                () -> assertTrue(subjekat.greske().get(0).contains("SMS provajder nedostupan")));

        subjekat.ocistiGreske();
        assertTrue(subjekat.greske().isEmpty());
    }

    @Test
    @DisplayName("posmatrac koji se sam odjavi tokom obavestavanja ne izaziva gresku")
    void izmenaListeTokomObavestavanja() {
        Subjekat subjekat = new Subjekat();
        List<String> primljeno = new ArrayList<>();
        Posmatrac samoJednom = new Posmatrac() {
            @Override
            public void obavesti(Dogadjaj d) {
                primljeno.add(d.poruka());
                subjekat.odjavi(this);
            }
        };
        subjekat.registruj(samoJednom);

        subjekat.objavi(dogadjaj(TipDogadjaja.TERMIN_ZAKAZAN));
        subjekat.objavi(dogadjaj(TipDogadjaja.TERMIN_OTKAZAN));

        assertEquals(1, primljeno.size());
    }

    @Test
    void revizijaBrojiPoTipuIVracaNepromenljivTrag() {
        RevizijaPosmatrac revizija = new RevizijaPosmatrac();
        revizija.obavesti(dogadjaj(TipDogadjaja.TERMIN_ZAKAZAN));
        revizija.obavesti(dogadjaj(TipDogadjaja.TERMIN_ZAKAZAN));
        revizija.obavesti(dogadjaj(TipDogadjaja.RECEPT_IZDAT));

        assertAll(
                () -> assertEquals(3, revizija.brojZapisa()),
                () -> assertEquals(3, revizija.trag().size()),
                () -> assertEquals(2, revizija.brojPoTipu(TipDogadjaja.TERMIN_ZAKAZAN)),
                () -> assertEquals(0, revizija.brojPoTipu(TipDogadjaja.NALOG_ZAKLJUCAN)));

        assertThrows(UnsupportedOperationException.class, () -> revizija.trag().clear());
        assertEquals(3, revizija.brojZapisa());
    }

    @Test
    void subjekatOdbijaNullVrednosti() {
        Subjekat subjekat = new Subjekat();
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> subjekat.registruj(null)),
                () -> assertThrows(NullPointerException.class, () -> subjekat.objavi(null)));
    }

    @Test
    @DisplayName("podrazumevano posmatraca zanima svaki tip dogadjaja")
    void podrazumevaniFilter() {
        Posmatrac praznPosmatrac = d -> { };
        assertAll(
                () -> assertTrue(praznPosmatrac.zanimaMe(TipDogadjaja.NEUSPELA_PRIJAVA)),
                () -> assertFalse(new EmailPosmatrac().zanimaMe(TipDogadjaja.NEUSPELA_PRIJAVA)),
                () -> assertTrue(new EmailPosmatrac().zanimaMe(TipDogadjaja.TERMIN_ZAKAZAN)));
    }
}
