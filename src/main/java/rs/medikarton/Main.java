package rs.medikarton;

import rs.medikarton.bezbednost.BruteForceDemo;
import rs.medikarton.bezbednost.SqlInjectionDemo;
import rs.medikarton.dao.DaoFabrika;
import rs.medikarton.db.BazaVeza;
import rs.medikarton.db.DemoPodaci;
import rs.medikarton.dogadjaj.EmailPosmatrac;
import rs.medikarton.dogadjaj.RevizijaPosmatrac;
import rs.medikarton.dogadjaj.Subjekat;
import rs.medikarton.izuzeci.TerminZauzetException;
import rs.medikarton.izuzeci.ValidacijaException;
import rs.medikarton.model.Korisnik;
import rs.medikarton.model.Lekar;
import rs.medikarton.model.Pacijent;
import rs.medikarton.model.Pregled;
import rs.medikarton.model.Termin;
import rs.medikarton.model.Uloga;
import rs.medikarton.servis.AutentifikacijaServis;
import rs.medikarton.servis.Karton;
import rs.medikarton.servis.KartonServis;
import rs.medikarton.servis.ZakazivanjeServis;

import java.time.LocalDateTime;
import java.util.List;

public final class Main {

    public static void main(String[] args) {
        BazaVeza.instanca(BazaVeza.MEMORIJSKI_URL);
        DaoFabrika fabrika = DaoFabrika.memorijska();
        DemoPodaci.ubaci(fabrika);

        Subjekat dogadjaji = new Subjekat();
        EmailPosmatrac email = new EmailPosmatrac();
        RevizijaPosmatrac revizija = new RevizijaPosmatrac();
        dogadjaji.registruj(email);
        dogadjaji.registruj(revizija);

        AutentifikacijaServis autentifikacija =
                new AutentifikacijaServis(fabrika.korisnici(), dogadjaji);
        ZakazivanjeServis zakazivanje = new ZakazivanjeServis(
                fabrika.termini(), fabrika.pacijenti(), fabrika.lekari(), dogadjaji);
        KartonServis karton = new KartonServis(fabrika.pregledi(), fabrika.recepti(),
                fabrika.nalazi(), fabrika.termini(), fabrika.pacijenti(), dogadjaji);

        LocalDateTime sada = LocalDateTime.of(2026, 9, 7, 8, 0);
        LocalDateTime terminVreme = LocalDateTime.of(2026, 9, 8, 9, 0);

        naslov("1. Registracija i prijava");
        Korisnik lekarNalog = autentifikacija.registruj("ana.jovic", "Klinika-2026!", Uloga.LEKAR);
        lekarNalog.setLekarId(1);
        fabrika.korisnici().azuriraj(lekarNalog);
        autentifikacija.registruj("admin", "Uprava-Klinike-1!", Uloga.ADMIN);
        System.out.println("  Kreiran nalog: " + lekarNalog);
        System.out.println("  Prijava sa tacnom lozinkom: "
                + autentifikacija.prijava("ana.jovic", "Klinika-2026!", sada).isPresent());
        System.out.println("  Prijava sa netacnom lozinkom: "
                + autentifikacija.prijava("ana.jovic", "pogresna", sada).isPresent());

        autentifikacija.prijava("ana.jovic", "Klinika-2026!", sada);
        try {
            autentifikacija.registruj("bob", "kratka", Uloga.PACIJENT);
        } catch (ValidacijaException e) {
            System.out.println("  Slaba lozinka odbijena -> " + e.getMessage());
        }

        naslov("2. Zakazivanje termina");
        Pacijent pacijent = fabrika.pacijenti().nadjiPoJmbg("1203985710122").orElseThrow();
        Lekar lekar = fabrika.lekari().nadjiPoId(1).orElseThrow();
        Termin termin = zakazivanje.zakazi(pacijent.getId(), lekar.getId(), terminVreme,
                "Kontrola krvnog pritiska", sada);
        System.out.println("  " + termin);
        try {
            zakazivanje.zakazi(3, lekar.getId(), terminVreme.plusMinutes(10), "Preklapanje", sada);
        } catch (TerminZauzetException e) {
            System.out.println("  Preklapanje odbijeno -> " + e.getMessage());
        }
        List<LocalDateTime> slobodni =
                zakazivanje.slobodniTermini(lekar.getId(), terminVreme.toLocalDate(), sada);
        System.out.println("  Slobodnih termina tog dana: " + slobodni.size()
                + " (prvi: " + slobodni.get(0) + ")");

        naslov("3. Pregled, recept i laboratorijski nalaz");
        zakazivanje.potvrdi(termin.getId());
        Pregled pregled = karton.evidentirajPregled(termin.getId(),
                "Pacijent navodi glavobolju i povisen pritisak u poslednjih mesec dana.",
                "I10", "Nefarmakoloske mere, kontrola za mesec dana", terminVreme);
        System.out.println("  " + pregled);
        System.out.println("  " + karton.izdajRecept(pregled.getId(), lekar.getId(),
                "Ramipril 5mg", "C09AA05", "1x1 tableta ujutru", 2, terminVreme.toLocalDate()));
        System.out.println("  " + karton.dodajNalaz(pacijent.getId(), pregled.getId(),
                "Holesterol ukupni", 7.4, "mmol/L", 3.0, 5.2, terminVreme.toLocalDate()));

        naslov("4. Zdravstveni karton pacijenta");
        Karton kartonPacijenta = karton.dohvatiKarton(lekarNalog, pacijent.getId(), sada);
        System.out.println("  " + kartonPacijenta.rezime());
        System.out.println("  Nalaza van referentnog opsega: "
                + kartonPacijenta.nalaziVanOpsega().size());
        System.out.println("  Recepcioner sme da otvori karton: "
                + karton.smePristupiti(nalog("recepcija", Uloga.RECEPCIONER), pacijent.getId()));

        naslov("5. Obavestenja (sablon Observer)");
        email.poslate().forEach(p -> System.out.println("  e-posta: " + p));
        System.out.println("  Zapisa u revizionom tragu: " + revizija.brojZapisa());

        naslov("6. SQL injection - ranjiva naspram bezbedne implementacije");
        SqlInjectionDemo injection = new SqlInjectionDemo(fabrika.veza());
        System.out.println(injection.izvestaj("ana.jovic").indent(2).stripTrailing());
        System.out.println("  Napadac UNION napadom cita tabelu naloga: "
                + injection.procitaniNaloziUnionNapadom());

        naslov("7. Brute Force - napad recnikom na nalog 'ana.jovic'");
        BruteForceDemo bruteForce = new BruteForceDemo(autentifikacija);
        System.out.println("  " + bruteForce
                .napadni("ana.jovic", BruteForceDemo.CEST_RECNIK, sada)
                .izvestaj());
        System.out.println("  Posle isteka kazne (15 min) prijava ponovo radi: "
                + autentifikacija.prijava("ana.jovic", "Klinika-2026!",
                        sada.plusMinutes(16)).isPresent());

        naslov("8. Izvestaj po dijagnozama (agregacija u bazi)");
        System.out.println("  " + fabrika.pregledi().brojPregledaPoDijagnozi(1));

        BazaVeza.ponistiInstancu();
    }

    //Ispisivanje naslova
    private static void naslov(String tekst) {
        System.out.println();
        System.out.println("=== " + tekst + " " + "=".repeat(Math.max(0, 66 - tekst.length())));
    }

    //Pronalazi korisnika
    private static Korisnik nalog(String korisnickoIme, Uloga uloga) {
        return new Korisnik(korisnickoIme, "demo-hash", "demo-so", uloga);
    }

    private Main() {
    }
}
