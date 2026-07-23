package rs.medikarton.dao;

import rs.medikarton.izuzeci.PodaciException;
import rs.medikarton.model.LabNalaz;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class LabNalazDao extends OsnovniDao<LabNalaz> {

    private static final String KOLONE = """
            id, pacijent_id, pregled_id, naziv_analize, vrednost, jedinica,
            ref_min, ref_max, datum_uzorkovanja
            """;

    public LabNalazDao(Connection veza) {
        super(veza);
    }

    @Override
    protected String tabela() {
        return "lab_nalaz";
    }

    @Override
    protected String kolone() {
        return KOLONE;
    }

    @Override
    protected String redosled() {
        return "datum_uzorkovanja DESC";
    }

    @Override
    public LabNalaz sacuvaj(LabNalaz n) {
        String sql = """
                INSERT INTO lab_nalaz
                    (pacijent_id, pregled_id, naziv_analize, vrednost, jedinica,
                     ref_min, ref_max, datum_uzorkovanja)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = pripremiSaKljucem(sql)) {
            popuni(ps, n);
            n.setId(izvrsiInsert(ps));
            return n;
        } catch (SQLException e) {
            throw new PodaciException("Neuspelo cuvanje laboratorijskog nalaza.", e);
        }
    }

    @Override
    public boolean azuriraj(LabNalaz n) {
        if (n.getId() == null) {
            throw new IllegalArgumentException("Nalaz bez identifikatora ne moze da se azurira.");
        }
        String sql = """
                UPDATE lab_nalaz SET pacijent_id = ?, pregled_id = ?, naziv_analize = ?, vrednost = ?,
                                     jedinica = ?, ref_min = ?, ref_max = ?, datum_uzorkovanja = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = veza.prepareStatement(sql)) {
            popuni(ps, n);
            ps.setInt(9, n.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new PodaciException("Neuspelo azuriranje nalaza (id=" + n.getId() + ").", e);
        }
    }

    public List<LabNalaz> zaPacijenta(int pacijentId) {
        String sql = "SELECT " + KOLONE + " FROM lab_nalaz WHERE pacijent_id = ? "
                + "ORDER BY datum_uzorkovanja DESC, naziv_analize";
        return lista(sql, ps -> ps.setInt(1, pacijentId),
                "Neuspelo citanje nalaza pacijenta (id=" + pacijentId + ").");
    }

    public List<LabNalaz> vanOpsegaZaPacijenta(int pacijentId) {
        String sql = """
                SELECT id, pacijent_id, pregled_id, naziv_analize, vrednost, jedinica,
                       ref_min, ref_max, datum_uzorkovanja
                FROM lab_nalaz
                WHERE pacijent_id = ? AND (vrednost < ref_min OR vrednost > ref_max)
                ORDER BY datum_uzorkovanja DESC
                """;
        return lista(sql, ps -> ps.setInt(1, pacijentId),
                "Neuspelo citanje nalaza van opsega.");
    }

    private static void popuni(PreparedStatement ps, LabNalaz n) throws SQLException {
        ps.setInt(1, n.getPacijentId());
        postaviCeoBrojIliNull(ps, 2, n.getPregledId());
        ps.setString(3, n.getNazivAnalize());
        ps.setDouble(4, n.getVrednost());
        ps.setString(5, n.getJedinica());
        ps.setDouble(6, n.getRefMin());
        ps.setDouble(7, n.getRefMax());
        ps.setString(8, tekst(n.getDatumUzorkovanja()));
    }

    @Override
    protected LabNalaz procitaj(ResultSet rs) throws SQLException {
        LabNalaz n = new LabNalaz();
        n.setId(rs.getInt("id"));
        n.setPacijentId(rs.getInt("pacijent_id"));
        n.setPregledId(celiBrojIliNull(rs, "pregled_id"));
        n.setNazivAnalize(rs.getString("naziv_analize"));
        n.setVrednost(rs.getDouble("vrednost"));
        n.setJedinica(rs.getString("jedinica"));
        n.setRefMin(rs.getDouble("ref_min"));
        n.setRefMax(rs.getDouble("ref_max"));
        n.setDatumUzorkovanja(datum(rs.getString("datum_uzorkovanja")));
        return n;
    }
}
