package rs.medikarton.servis;

import rs.medikarton.dao.KorisnikDao;
import rs.medikarton.dogadjaj.Dogadjaj;
import rs.medikarton.dogadjaj.Subjekat;
import rs.medikarton.dogadjaj.TipDogadjaja;
import rs.medikarton.izuzeci.NalogZakljucanException;
import rs.medikarton.izuzeci.ValidacijaException;
import rs.medikarton.model.Korisnik;
import rs.medikarton.model.Uloga;
import rs.medikarton.util.LozinkaServis;
import rs.medikarton.util.Validator;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

public class AutentifikacijaServis {

    private final KorisnikDao korisnici;
    private final Subjekat dogadjaji;

    public AutentifikacijaServis(KorisnikDao korisnici, Subjekat dogadjaji) {
        this.korisnici = Objects.requireNonNull(korisnici, "KorisnikDao je obavezan.");
        this.dogadjaji = Objects.requireNonNull(dogadjaji, "Subjekat dogadjaja je obavezan.");
    }

    public Korisnik registruj(String korisnickoIme, String lozinka, Uloga uloga) {
        String ime = Validator.zahtevajDuzinu(korisnickoIme, "korisnickoIme", 3, 32);
        if (!ime.matches("^[a-z0-9._]+$")) {
            throw new ValidacijaException("korisnickoIme",
                    "Dozvoljena su samo mala slova, cifre, tacka i donja crta.");
        }
        Validator.validirajLozinku(lozinka);
        Objects.requireNonNull(uloga, "Uloga je obavezna.");

        if (korisnici.nadjiPoKorisnickomImenu(ime).isPresent()) {
            throw new ValidacijaException("korisnickoIme", "Korisnicko ime je vec zauzeto.");
        }

        String so = LozinkaServis.generisiSo();
        Korisnik korisnik = new Korisnik(ime, LozinkaServis.hash(lozinka, so), so, uloga);
        return korisnici.sacuvaj(korisnik);
    }

    public Optional<Korisnik> prijava(String korisnickoIme, String lozinka, LocalDateTime sada) {
        Objects.requireNonNull(sada, "Trenutno vreme je obavezno.");

        Optional<Korisnik> mozdaKorisnik = korisnici.nadjiPoKorisnickomImenu(
                korisnickoIme == null ? "" : korisnickoIme.trim());

        if (mozdaKorisnik.isEmpty()) {
            objavi(TipDogadjaja.NEUSPELA_PRIJAVA, korisnickoIme, "Neuspela prijava: nepoznat nalog.", sada);
            return Optional.empty();
        }

        Korisnik korisnik = mozdaKorisnik.get();

        if (!korisnik.isAktivan()) {
            return Optional.empty();
        }
        if (korisnik.jeZakljucan(sada)) {
            throw new NalogZakljucanException(korisnik.getZakljucanDo());
        }

        boolean ispravna = LozinkaServis.proveri(lozinka, korisnik.getSo(), korisnik.getLozinkaHash());
        if (!ispravna) {
            korisnik.zabeleziNeuspeluPrijavu(sada);
            korisnici.azuriraj(korisnik);
            objavi(TipDogadjaja.NEUSPELA_PRIJAVA, korisnik.getKorisnickoIme(),
                    "Neuspela prijava. Preostalo pokusaja: " + korisnik.preostaloPokusaja(), sada);
            if (korisnik.jeZakljucan(sada)) {
                objavi(TipDogadjaja.NALOG_ZAKLJUCAN, korisnik.getKorisnickoIme(),
                        "Nalog je zakljucan do " + korisnik.getZakljucanDo() + ".", sada);
            }
            return Optional.empty();
        }

        if (korisnik.getBrojNeuspelihPrijava() > 0 || korisnik.getZakljucanDo() != null) {
            korisnik.resetujBrojacPrijava();
            korisnici.azuriraj(korisnik);
        }
        return Optional.of(korisnik);
    }

    public boolean promeniLozinku(String korisnickoIme, String staraLozinka, String novaLozinka) {
        Korisnik korisnik = korisnici.nadjiPoKorisnickomImenu(korisnickoIme)
                .orElseThrow(() -> new ValidacijaException("korisnickoIme", "Nalog ne postoji."));

        if (!LozinkaServis.proveri(staraLozinka, korisnik.getSo(), korisnik.getLozinkaHash())) {
            return false;
        }
        Validator.validirajLozinku(novaLozinka);
        if (LozinkaServis.proveri(novaLozinka, korisnik.getSo(), korisnik.getLozinkaHash())) {
            throw new ValidacijaException("lozinka", "Nova lozinka mora da se razlikuje od stare.");
        }

        String novaSo = LozinkaServis.generisiSo();
        korisnik.setSo(novaSo);
        korisnik.setLozinkaHash(LozinkaServis.hash(novaLozinka, novaSo));
        korisnik.resetujBrojacPrijava();
        return korisnici.azuriraj(korisnik);
    }

    public boolean otkljucaj(String korisnickoIme) {
        Optional<Korisnik> mozdaKorisnik = korisnici.nadjiPoKorisnickomImenu(korisnickoIme);
        if (mozdaKorisnik.isEmpty()) {
            return false;
        }
        Korisnik korisnik = mozdaKorisnik.get();
        korisnik.resetujBrojacPrijava();
        return korisnici.azuriraj(korisnik);
    }

    private void objavi(TipDogadjaja tip, String primalac, String poruka, LocalDateTime sada) {
        dogadjaji.objavi(new Dogadjaj(tip, primalac, poruka, sada));
    }
}
