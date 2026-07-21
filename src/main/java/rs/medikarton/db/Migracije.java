package rs.medikarton.db;

import rs.medikarton.izuzeci.PodaciException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class Migracije {

    private static final String[] SEMA = {

            """
            CREATE TABLE IF NOT EXISTS pacijent (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                jmbg            TEXT    NOT NULL UNIQUE,
                ime             TEXT    NOT NULL,
                prezime         TEXT    NOT NULL,
                datum_rodjenja  TEXT    NOT NULL,
                pol             TEXT    NOT NULL CHECK (pol IN ('M','Z')),
                email           TEXT,
                telefon         TEXT,
                adresa          TEXT,
                krvna_grupa     TEXT,
                alergije        TEXT    NOT NULL DEFAULT ''
            )
            """,

            """
            CREATE TABLE IF NOT EXISTS lekar (
                id                     INTEGER PRIMARY KEY AUTOINCREMENT,
                broj_licence           TEXT    NOT NULL UNIQUE,
                ime                    TEXT    NOT NULL,
                prezime                TEXT    NOT NULL,
                specijalizacija        TEXT    NOT NULL,
                email                  TEXT,
                trajanje_pregleda_min  INTEGER NOT NULL DEFAULT 20
            )
            """,

            """
            CREATE TABLE IF NOT EXISTS korisnik (
                id                     INTEGER PRIMARY KEY AUTOINCREMENT,
                korisnicko_ime         TEXT    NOT NULL UNIQUE,
                lozinka_hash           TEXT    NOT NULL,
                so                     TEXT    NOT NULL,
                uloga                  TEXT    NOT NULL
                                       CHECK (uloga IN ('PACIJENT','LEKAR','RECEPCIONER','ADMIN')),
                aktivan                INTEGER NOT NULL DEFAULT 1,
                broj_neuspelih_prijava INTEGER NOT NULL DEFAULT 0,
                zakljucan_do           TEXT,
                pacijent_id            INTEGER REFERENCES pacijent(id) ON DELETE SET NULL,
                lekar_id               INTEGER REFERENCES lekar(id)    ON DELETE SET NULL
            )
            """,

            """
            CREATE TABLE IF NOT EXISTS termin (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                pacijent_id    INTEGER NOT NULL REFERENCES pacijent(id) ON DELETE CASCADE,
                lekar_id       INTEGER NOT NULL REFERENCES lekar(id)    ON DELETE CASCADE,
                pocetak        TEXT    NOT NULL,
                trajanje_min   INTEGER NOT NULL CHECK (trajanje_min BETWEEN 5 AND 240),
                status         TEXT    NOT NULL
                               CHECK (status IN ('ZAKAZAN','POTVRDJEN','OTKAZAN','REALIZOVAN')),
                razlog_dolaska TEXT
            )
            """,

            """
            CREATE TABLE IF NOT EXISTS pregled (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                termin_id     INTEGER UNIQUE REFERENCES termin(id) ON DELETE SET NULL,
                pacijent_id   INTEGER NOT NULL REFERENCES pacijent(id) ON DELETE CASCADE,
                lekar_id      INTEGER NOT NULL REFERENCES lekar(id),
                datum_vreme   TEXT    NOT NULL,
                anamneza      TEXT,
                dijagnoza_mkb TEXT    NOT NULL,
                terapija      TEXT
            )
            """,

            """
            CREATE TABLE IF NOT EXISTS recept (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                pregled_id      INTEGER NOT NULL REFERENCES pregled(id) ON DELETE CASCADE,
                naziv_leka      TEXT    NOT NULL,
                atc_sifra       TEXT    NOT NULL,
                doziranje       TEXT    NOT NULL,
                broj_pakovanja  INTEGER NOT NULL DEFAULT 1 CHECK (broj_pakovanja BETWEEN 1 AND 6),
                datum_izdavanja TEXT    NOT NULL,
                vazi_do         TEXT    NOT NULL
            )
            """,

            """
            CREATE TABLE IF NOT EXISTS lab_nalaz (
                id                INTEGER PRIMARY KEY AUTOINCREMENT,
                pacijent_id       INTEGER NOT NULL REFERENCES pacijent(id) ON DELETE CASCADE,
                pregled_id        INTEGER REFERENCES pregled(id) ON DELETE SET NULL,
                naziv_analize     TEXT    NOT NULL,
                vrednost          REAL    NOT NULL,
                jedinica          TEXT    NOT NULL,
                ref_min           REAL    NOT NULL,
                ref_max           REAL    NOT NULL,
                datum_uzorkovanja TEXT    NOT NULL
            )
            """,

            "CREATE INDEX IF NOT EXISTS ix_termin_lekar_pocetak ON termin(lekar_id, pocetak)",
            "CREATE INDEX IF NOT EXISTS ix_termin_pacijent      ON termin(pacijent_id, pocetak)",
            "CREATE INDEX IF NOT EXISTS ix_pregled_pacijent     ON pregled(pacijent_id, datum_vreme)",
            "CREATE INDEX IF NOT EXISTS ix_recept_pregled       ON recept(pregled_id)",
            "CREATE INDEX IF NOT EXISTS ix_lab_pacijent         ON lab_nalaz(pacijent_id, datum_uzorkovanja)",
            "CREATE INDEX IF NOT EXISTS ix_korisnik_ime         ON korisnik(korisnicko_ime)"
    };

    private static final String[] TABELE_ZA_BRISANJE = {
            "lab_nalaz", "recept", "pregled", "termin", "korisnik", "pacijent", "lekar"
    };

    private Migracije() {
        throw new AssertionError("Migracije se ne instanciraju.");
    }

    public static void kreirajSemu(Connection veza) {
        try (Statement s = veza.createStatement()) {
            for (String naredba : SEMA) {
                s.execute(naredba);
            }
        } catch (SQLException e) {
            throw new PodaciException("Neuspelo kreiranje seme baze.", e);
        }
    }

    public static void isprazni(Connection veza) {
        try (Statement s = veza.createStatement()) {
            s.execute("PRAGMA foreign_keys = OFF");
            for (String tabela : TABELE_ZA_BRISANJE) {
                s.executeUpdate("DELETE FROM " + tabela);
            }
            try {

                s.executeUpdate("DELETE FROM sqlite_sequence");
            } catch (SQLException ignorisano) {

            }
            s.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            throw new PodaciException("Neuspelo praznjenje tabela.", e);
        }
    }

    public static int brojTabela() {
        return TABELE_ZA_BRISANJE.length;
    }
}
