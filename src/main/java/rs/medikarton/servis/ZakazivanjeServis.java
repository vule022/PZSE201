package rs.medikarton.servis;

import rs.medikarton.dao.LekarDao;
import rs.medikarton.dao.PacijentDao;
import rs.medikarton.dao.TerminDao;
import rs.medikarton.dogadjaj.Dogadjaj;
import rs.medikarton.dogadjaj.Subjekat;
import rs.medikarton.dogadjaj.TipDogadjaja;
import rs.medikarton.izuzeci.EntitetNijeNadjenException;
import rs.medikarton.izuzeci.TerminZauzetException;
import rs.medikarton.izuzeci.ValidacijaException;
import rs.medikarton.model.Lekar;
import rs.medikarton.model.Pacijent;
import rs.medikarton.model.StatusTermina;
import rs.medikarton.model.Termin;
import rs.medikarton.util.Validator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ZakazivanjeServis {

    public static final int SATI_ZA_OTKAZIVANJE = 2;

    private static final int PRVI_SAT = 7;
    private static final int POSLEDNJI_SAT = 20;

    private final TerminDao termini;
    private final PacijentDao pacijenti;
    private final LekarDao lekari;
    private final Subjekat dogadjaji;

    public ZakazivanjeServis(TerminDao termini, PacijentDao pacijenti, LekarDao lekari, Subjekat dogadjaji) {
        this.termini = Objects.requireNonNull(termini, "TerminDao je obavezan.");
        this.pacijenti = Objects.requireNonNull(pacijenti, "PacijentDao je obavezan.");
        this.lekari = Objects.requireNonNull(lekari, "LekarDao je obavezan.");
        this.dogadjaji = Objects.requireNonNull(dogadjaji, "Subjekat dogadjaja je obavezan.");
    }

    public synchronized Termin zakazi(int pacijentId, int lekarId, LocalDateTime pocetak,
                                      String razlogDolaska, LocalDateTime sada) {
        Pacijent pacijent = pacijenti.nadjiPoId(pacijentId)
                .orElseThrow(() -> new EntitetNijeNadjenException("Pacijent", pacijentId));
        Lekar lekar = lekari.nadjiPoId(lekarId)
                .orElseThrow(() -> new EntitetNijeNadjenException("Lekar", lekarId));

        Validator.validirajTerminPocetak(pocetak, sada);
        String razlog = Validator.zahtevajDuzinu(razlogDolaska, "razlogDolaska", 3, 300);

        Termin noviTermin = new Termin(pacijentId, lekarId, pocetak,
                lekar.getTrajanjePregledaMin(), razlog);

        if (zavrsavaSePosleRadnogVremena(noviTermin)) {
            throw new ValidacijaException("pocetak", "Termin bi se zavrsio posle radnog vremena klinike.");
        }

        List<Termin> istiDan = termini.aktivniZaLekaraNaDan(lekarId, pocetak.toLocalDate());
        for (Termin postojeci : istiDan) {
            if (postojeci.preklapaSe(noviTermin)) {
                throw new TerminZauzetException(lekarId, pocetak);
            }
            if (Objects.equals(postojeci.getPacijentId(), pacijentId)) {
                throw new ValidacijaException("pacijentId",
                        "Pacijent vec ima aktivan termin kod ovog lekara na dan " + pocetak.toLocalDate() + ".");
            }
        }

        Termin sacuvan = termini.sacuvaj(noviTermin);
        dogadjaji.objavi(new Dogadjaj(TipDogadjaja.TERMIN_ZAKAZAN, pacijent.getEmail(),
                "Zakazan termin kod " + lekar.punoIme() + " za " + pocetak + ".", sada));
        return sacuvan;
    }

    public boolean otkazi(int terminId, LocalDateTime sada) {
        Termin termin = termini.nadjiPoId(terminId)
                .orElseThrow(() -> new EntitetNijeNadjenException("Termin", terminId));

        if (!termin.getStatus().moguceOtkazati()) {
            return false;
        }
        if (sada.isAfter(termin.getPocetak().minusHours(SATI_ZA_OTKAZIVANJE))) {
            throw new ValidacijaException("terminId",
                    "Termin se otkazuje najkasnije " + SATI_ZA_OTKAZIVANJE + " sata pre pocetka.");
        }

        termin.setStatus(StatusTermina.OTKAZAN);
        boolean uspeh = termini.azuriraj(termin);
        if (uspeh) {
            String email = pacijenti.nadjiPoId(termin.getPacijentId())
                    .map(Pacijent::getEmail).orElse(null);
            dogadjaji.objavi(new Dogadjaj(TipDogadjaja.TERMIN_OTKAZAN, email,
                    "Otkazan termin zakazan za " + termin.getPocetak() + ".", sada));
        }
        return uspeh;
    }

    public boolean potvrdi(int terminId) {
        Termin termin = termini.nadjiPoId(terminId)
                .orElseThrow(() -> new EntitetNijeNadjenException("Termin", terminId));
        if (termin.getStatus() != StatusTermina.ZAKAZAN) {
            return false;
        }
        termin.setStatus(StatusTermina.POTVRDJEN);
        return termini.azuriraj(termin);
    }

    public List<LocalDateTime> slobodniTermini(int lekarId, LocalDate dan, LocalDateTime sada) {
        Lekar lekar = lekari.nadjiPoId(lekarId)
                .orElseThrow(() -> new EntitetNijeNadjenException("Lekar", lekarId));

        List<LocalDateTime> slobodni = new ArrayList<>();
        if (dan.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
                || dan.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            return slobodni;
        }

        int korak = lekar.getTrajanjePregledaMin();
        List<Termin> zauzeti = termini.aktivniZaLekaraNaDan(lekarId, dan);

        LocalDateTime kandidat = dan.atTime(PRVI_SAT, 0);
        LocalDateTime krajRadnogVremena = dan.atTime(POSLEDNJI_SAT, 0);

        while (!kandidat.plusMinutes(korak).isAfter(krajRadnogVremena)) {
            Termin probni = new Termin(null, lekarId, kandidat, korak, "provera");
            boolean slobodan = kandidat.isAfter(sada)
                    && zauzeti.stream().noneMatch(t -> t.preklapaSe(probni));
            if (slobodan) {
                slobodni.add(kandidat);
            }
            kandidat = kandidat.plusMinutes(korak);
        }
        return slobodni;
    }

    private boolean zavrsavaSePosleRadnogVremena(Termin termin) {
        LocalDateTime kraj = termin.kraj();
        return kraj.toLocalDate().isAfter(termin.getPocetak().toLocalDate())
                || kraj.isAfter(termin.getPocetak().toLocalDate().atTime(POSLEDNJI_SAT, 0));
    }
}
