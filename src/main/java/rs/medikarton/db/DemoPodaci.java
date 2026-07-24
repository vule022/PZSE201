package rs.medikarton.db;

import rs.medikarton.dao.DaoFabrika;
import rs.medikarton.model.Lekar;
import rs.medikarton.model.Pacijent;

import java.time.LocalDate;
import java.util.List;

// PODACI ZA DEMONSTRACIJU I TESTOVE
public final class DemoPodaci {

    private DemoPodaci() {
        throw new AssertionError("DemoPodaci se ne instanciraju.");
    }

    public static void ubaci(DaoFabrika fabrika) {
        if (fabrika.pacijenti().prebroj() > 0) {
            return;
        }

        fabrika.lekari().sacuvaj(new Lekar("LK-100234", "Ana", "Jovic",
                "kardiologija", "ana.jovic@medikarton.rs"));
        fabrika.lekari().sacuvaj(new Lekar("LK-100987", "Marko", "Ilic",
                "opsta medicina", "marko.ilic@medikarton.rs"));
        Lekar pedijatar = new Lekar("LK-101455", "Jelena", "Popovic",
                "pedijatrija", "jelena.popovic@medikarton.rs");
        pedijatar.setTrajanjePregledaMin(30);
        fabrika.lekari().sacuvaj(pedijatar);

        List<Pacijent> pacijenti = List.of(
                napravi("1203985710122", "Nikola", "Petrovic", LocalDate.of(1985, 3, 12), "M",
                        "nikola.petrovic@example.rs", "0631234567", "A+", ""),
                napravi("2507990725157", "Milica", "Stankovic", LocalDate.of(1990, 7, 25), "Z",
                        "milica.stankovic@example.rs", "0642345678", "0-", "penicilin"),
                napravi("0411978710344", "Dragan", "Nikolic", LocalDate.of(1978, 11, 4), "M",
                        "dragan.nikolic@example.rs", "0653456789", "B+", ""),
                napravi("1809002725085", "Sara", "Ilic", LocalDate.of(2002, 9, 18), "Z",
                        "sara.ilic@example.rs", "0664567890", "AB+", "aspirin, jod"),
                napravi("0102968710270", "Zoran", "Markovic", LocalDate.of(1968, 2, 1), "M",
                        "zoran.markovic@example.rs", "0605678901", "A-", "")
        );
        fabrika.pacijenti().sacuvajSve(pacijenti);
    }

    private static Pacijent napravi(String jmbg, String ime, String prezime, LocalDate rodjen,
                                    String pol, String email, String telefon,
                                    String krvnaGrupa, String alergije) {
        Pacijent p = new Pacijent(jmbg, ime, prezime, rodjen, pol, email, telefon);
        p.setKrvnaGrupa(krvnaGrupa);
        p.setAlergije(alergije);
        p.setAdresa("Bulevar oslobodjenja 1, Nis");
        return p;
    }
}
