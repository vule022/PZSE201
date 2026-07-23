package rs.medikarton.dao;

import rs.medikarton.izuzeci.PodaciException;
import rs.medikarton.izuzeci.ValidacijaException;
import rs.medikarton.model.Pregled;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PregledDao extends OsnovniDao<Pregled> {

    private static final String KOLONE =
            "id, termin_id, pacijent_id, lekar_id, datum_vreme, anamneza, dijagnoza_mkb, terapija";

    public PregledDao(Connection veza) {
        super(veza);
    }

    @Override
    protected String tabela() {
        return "pregled";
    }

    @Override
    protected String kolone() {
        return KOLONE;
    }

    @Override
    protected String redosled() {
        return "datum_vreme DESC";
    }

    @Override
    public Pregled sacuvaj(Pregled p) {
        String sql = """
                INSERT INTO pregled
                    (termin_id, pacijent_id, lekar_id, datum_vreme, anamneza, dijagnoza_mkb, terapija)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = pripremiSaKljucem(sql)) {
            popuni(ps, p);
            p.setId(izvrsiInsert(ps));
            return p;
        } catch (SQLException e) {
            throw new PodaciException("Neuspelo cuvanje pregleda.", e);
        }
    }

    public Pregled sacuvajIRealizujTermin(Pregled p) {
        boolean transakcijaPokrenuta = false;
        try {
            if (!veza.getAutoCommit()) {
                throw new IllegalStateException("Veza je vec u aktivnoj transakciji.");
            }
            veza.setAutoCommit(false);
            transakcijaPokrenuta = true;

            String sql = """
                    UPDATE termin SET status = 'REALIZOVAN'
                    WHERE id = ? AND status IN ('ZAKAZAN','POTVRDJEN')
                    """;
            try (PreparedStatement ps = veza.prepareStatement(sql)) {
                ps.setInt(1, p.getTerminId());
                if (ps.executeUpdate() != 1) {
                    throw new ValidacijaException("terminId",
                            "Termin vise nije u stanju iz kog pregled moze da se evidentira.");
                }
            }

            Pregled sacuvan = sacuvaj(p);
            veza.commit();
            return sacuvan;
        } catch (SQLException e) {
            if (transakcijaPokrenuta) {
                ponistiTransakciju(e);
            }
            throw new PodaciException("Neuspelo evidentiranje pregleda u transakciji.", e);
        } catch (RuntimeException e) {
            if (transakcijaPokrenuta) {
                ponistiTransakciju(e);
            }
            throw e;
        } finally {
            try {
                if (transakcijaPokrenuta) {
                    veza.setAutoCommit(true);
                }
            } catch (SQLException e) {
                throw new PodaciException("Neuspelo vracanje autoCommit rezima.", e);
            }
        }
    }

    private void ponistiTransakciju(Exception uzrok) {
        try {
            veza.rollback();
        } catch (SQLException rollbackGreska) {
            uzrok.addSuppressed(rollbackGreska);
        }
    }

    @Override
    public boolean azuriraj(Pregled p) {
        if (p.getId() == null) {
            throw new IllegalArgumentException("Pregled bez identifikatora ne moze da se azurira.");
        }
        String sql = """
                UPDATE pregled SET termin_id = ?, pacijent_id = ?, lekar_id = ?, datum_vreme = ?,
                                   anamneza = ?, dijagnoza_mkb = ?, terapija = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = veza.prepareStatement(sql)) {
            popuni(ps, p);
            ps.setInt(8, p.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new PodaciException("Neuspelo azuriranje pregleda (id=" + p.getId() + ").", e);
        }
    }

    public List<Pregled> zaPacijenta(int pacijentId) {
        String sql = "SELECT " + KOLONE + " FROM pregled WHERE pacijent_id = ? ORDER BY datum_vreme DESC";
        return lista(sql, ps -> ps.setInt(1, pacijentId),
                "Neuspelo citanje pregleda pacijenta (id=" + pacijentId + ").");
    }

    public Map<String, Integer> brojPregledaPoDijagnozi(int minBroj) {
        String sql = """
                SELECT dijagnoza_mkb, COUNT(*) AS broj
                FROM pregled
                GROUP BY dijagnoza_mkb
                HAVING COUNT(*) >= ?
                ORDER BY broj DESC, dijagnoza_mkb
                """;
        Map<String, Integer> rezultat = new LinkedHashMap<>();
        try (PreparedStatement ps = veza.prepareStatement(sql)) {
            ps.setInt(1, minBroj);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rezultat.put(rs.getString("dijagnoza_mkb"), rs.getInt("broj"));
                }
            }
            return rezultat;
        } catch (SQLException e) {
            throw new PodaciException("Neuspelo racunanje izvestaja po dijagnozama.", e);
        }
    }

    private static void popuni(PreparedStatement ps, Pregled p) throws SQLException {
        postaviCeoBrojIliNull(ps, 1, p.getTerminId());
        ps.setInt(2, p.getPacijentId());
        ps.setInt(3, p.getLekarId());
        ps.setString(4, tekst(p.getDatumVreme()));
        ps.setString(5, p.getAnamneza());
        ps.setString(6, p.getDijagnozaMkb());
        ps.setString(7, p.getTerapija());
    }

    @Override
    protected Pregled procitaj(ResultSet rs) throws SQLException {
        Pregled p = new Pregled();
        p.setId(rs.getInt("id"));
        p.setTerminId(celiBrojIliNull(rs, "termin_id"));
        p.setPacijentId(rs.getInt("pacijent_id"));
        p.setLekarId(rs.getInt("lekar_id"));
        p.setDatumVreme(vreme(rs.getString("datum_vreme")));
        p.setAnamneza(rs.getString("anamneza"));
        p.setDijagnozaMkb(rs.getString("dijagnoza_mkb"));
        p.setTerapija(rs.getString("terapija"));
        return p;
    }
}
