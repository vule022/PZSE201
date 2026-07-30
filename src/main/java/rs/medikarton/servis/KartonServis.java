package rs.medikarton.servis;

import rs.medikarton.dao.LabNalazDao;
import rs.medikarton.dao.PacijentDao;
import rs.medikarton.dao.PregledDao;
import rs.medikarton.dao.ReceptDao;
import rs.medikarton.dao.TerminDao;
import rs.medikarton.dogadjaj.Dogadjaj;
import rs.medikarton.dogadjaj.Subjekat;
import rs.medikarton.dogadjaj.TipDogadjaja;
import rs.medikarton.izuzeci.EntitetNijeNadjenException;
import rs.medikarton.izuzeci.ValidacijaException;
import rs.medikarton.model.LabNalaz;
import rs.medikarton.model.Korisnik;
import rs.medikarton.model.Pacijent;
import rs.medikarton.model.Pregled;
import rs.medikarton.model.Recept;
import rs.medikarton.model.StatusTermina;
import rs.medikarton.model.Termin;
import rs.medikarton.util.Validator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class KartonServis {

    public static final int DANA_VAZENJA_RECEPTA = 30;

    private final PregledDao pregledi;
    private final ReceptDao recepti;
    private final LabNalazDao nalazi;
    private final TerminDao termini;
    private final PacijentDao pacijenti;
    private final Subjekat dogadjaji;

    public KartonServis(PregledDao pregledi, ReceptDao recepti, LabNalazDao nalazi,
                        TerminDao termini, PacijentDao pacijenti, Subjekat dogadjaji) {
        this.pregledi = Objects.requireNonNull(pregledi, "PregledDao je obavezan.");
        this.recepti = Objects.requireNonNull(recepti, "ReceptDao je obavezan.");
        this.nalazi = Objects.requireNonNull(nalazi, "LabNalazDao je obavezan.");
        this.termini = Objects.requireNonNull(termini, "TerminDao je obavezan.");
        this.pacijenti = Objects.requireNonNull(pacijenti, "PacijentDao je obavezan.");
        this.dogadjaji = Objects.requireNonNull(dogadjaji, "Subjekat dogadjaja je obavezan.");
    }

    public Pregled evidentirajPregled(int terminId, String anamneza, String dijagnozaMkb,
                                      String terapija, LocalDateTime sada) {
        Objects.requireNonNull(sada, "Vreme pregleda je obavezno.");
        Termin termin = termini.nadjiPoId(terminId)
                .orElseThrow(() -> new EntitetNijeNadjenException("Termin", terminId));

        if (!termin.getStatus().moguceRealizovati()) {
            throw new ValidacijaException("terminId",
                    "Pregled se ne moze evidentirati nad terminom u statusu " + termin.getStatus() + ".");
        }

        String sifra = Validator.validirajMkb(dijagnozaMkb);
        String tekstAnamneze = Validator.zahtevajDuzinu(anamneza, "anamneza", 10, 4000);

        Pregled pregled = new Pregled(terminId, termin.getPacijentId(), termin.getLekarId(),
                sada, tekstAnamneze, sifra, terapija);
        Pregled sacuvan = pregledi.sacuvajIRealizujTermin(pregled);
        termin.setStatus(StatusTermina.REALIZOVAN);

        dogadjaji.objavi(new Dogadjaj(TipDogadjaja.PREGLED_EVIDENTIRAN, emailPacijenta(termin.getPacijentId()),
                "Evidentiran pregled, dijagnoza " + sifra + ".", sada));
        return sacuvan;
    }

    public Recept izdajRecept(int pregledId, int lekarId, String nazivLeka, String atcSifra,
                              String doziranje, int brojPakovanja, LocalDate danas) {
        Pregled pregled = pregledi.nadjiPoId(pregledId)
                .orElseThrow(() -> new EntitetNijeNadjenException("Pregled", pregledId));

        if (pregled.getLekarId() != lekarId) {
            throw new ValidacijaException("lekarId",
                    "Recept moze da izda samo lekar koji je obavio pregled.");
        }

        String naziv = Validator.zahtevajDuzinu(nazivLeka, "nazivLeka", 2, 120);
        String atc = Validator.validirajAtc(atcSifra);
        String doza = Validator.zahtevajDuzinu(doziranje, "doziranje", 3, 200);
        Validator.zahtevajUOpsegu(brojPakovanja, "brojPakovanja", 1, 6);
        Objects.requireNonNull(danas, "Datum izdavanja je obavezan.");

        proveriAlergiju(pregled.getPacijentId(), naziv);

        Recept recept = new Recept(pregledId, naziv, atc, doza, brojPakovanja,
                danas, danas.plusDays(DANA_VAZENJA_RECEPTA));
        Recept sacuvan = recepti.sacuvaj(recept);

        dogadjaji.objavi(new Dogadjaj(TipDogadjaja.RECEPT_IZDAT, emailPacijenta(pregled.getPacijentId()),
                "Izdat elektronski recept za " + naziv + ", vazi do " + recept.getVaziDo() + ".",
                danas.atStartOfDay()));
        return sacuvan;
    }

    public LabNalaz dodajNalaz(int pacijentId, Integer pregledId, String nazivAnalize, double vrednost,
                               String jedinica, double refMin, double refMax, LocalDate datumUzorkovanja) {
        if (pacijenti.nadjiPoId(pacijentId).isEmpty()) {
            throw new EntitetNijeNadjenException("Pacijent", pacijentId);
        }
        if (pregledId != null) {
            Pregled pregled = pregledi.nadjiPoId(pregledId)
                    .orElseThrow(() -> new EntitetNijeNadjenException("Pregled", pregledId));
            if (pregled.getPacijentId() != pacijentId) {
                throw new ValidacijaException("pregledId",
                        "Laboratorijski nalaz moze da se veze samo za pregled istog pacijenta.");
            }
        }
        String naziv = Validator.zahtevajDuzinu(nazivAnalize, "nazivAnalize", 2, 80);
        String jed = Validator.zahtevajDuzinu(jedinica, "jedinica", 1, 20);
        if (refMin >= refMax) {
            throw new ValidacijaException("refMin", "Donja referentna granica mora biti manja od gornje.");
        }
        Validator.zahtevajUOpsegu(vrednost, "vrednost", -1_000d, 1_000_000d);
        Objects.requireNonNull(datumUzorkovanja, "Datum uzorkovanja je obavezan.");

        LabNalaz nalaz = new LabNalaz(pacijentId, pregledId, naziv, vrednost, jed,
                refMin, refMax, datumUzorkovanja);
        LabNalaz sacuvan = nalazi.sacuvaj(nalaz);

        if (!sacuvan.uReferentnomOpsegu()) {
            dogadjaji.objavi(new Dogadjaj(TipDogadjaja.NALAZ_VAN_OPSEGA, emailPacijenta(pacijentId),
                    "Nalaz " + naziv + " je van referentnog opsega (" + sacuvan.oznaka() + ").",
                    datumUzorkovanja.atStartOfDay()));
        }
        return sacuvan;
    }

    public Karton dohvatiKarton(Korisnik prijavljeniKorisnik, int pacijentId, LocalDateTime sada) {
        Objects.requireNonNull(sada, "Vreme pristupa je obavezno.");
        if (!smePristupiti(prijavljeniKorisnik, pacijentId)) {
            throw new ValidacijaException("pristup", "Korisnik nema pravo pristupa ovom kartonu.");
        }
        Pacijent pacijent = pacijenti.nadjiPoId(pacijentId)
                .orElseThrow(() -> new EntitetNijeNadjenException("Pacijent", pacijentId));
        List<Pregled> istorija = pregledi.zaPacijenta(pacijentId);
        List<Recept> sviRecepti = recepti.zaPacijenta(pacijentId);
        Karton karton = new Karton(pacijent, istorija, sviRecepti, nalazi.zaPacijenta(pacijentId));
        dogadjaji.objavi(new Dogadjaj(TipDogadjaja.KARTON_OTVOREN,
                prijavljeniKorisnik.getKorisnickoIme(),
                "Otvoren karton pacijenta " + pacijentId + ".", sada));
        return karton;
    }

    public boolean smePristupiti(Korisnik korisnik, int pacijentId) {
        if (korisnik == null || !korisnik.isAktivan() || korisnik.getUloga() == null) {
            return false;
        }
        return switch (korisnik.getUloga()) {
            case ADMIN, LEKAR -> true;
            case PACIJENT -> Objects.equals(korisnik.getPacijentId(), pacijentId);
            case RECEPCIONER -> false;
        };
    }

    private void proveriAlergiju(int pacijentId, String nazivLeka) {
        Pacijent pacijent = pacijenti.nadjiPoId(pacijentId)
                .orElseThrow(() -> new EntitetNijeNadjenException("Pacijent", pacijentId));
        if (!pacijent.imaAlergije()) {
            return;
        }
        String alergije = pacijent.getAlergije().toLowerCase();
        String lek = nazivLeka.toLowerCase();
        for (String stavka : alergije.split("[,;]")) {
            String ocisceno = stavka.trim();
            if (!ocisceno.isEmpty() && lek.contains(ocisceno)) {
                throw new ValidacijaException("nazivLeka",
                        "Pacijent je alergican na '" + ocisceno + "' - recept nije izdat.");
            }
        }
    }

    private String emailPacijenta(int pacijentId) {
        return pacijenti.nadjiPoId(pacijentId).map(Pacijent::getEmail).orElse(null);
    }
}
