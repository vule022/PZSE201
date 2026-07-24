package rs.medikarton.dao;

import rs.medikarton.izuzeci.PodaciException;
import rs.medikarton.model.StatusTermina;
import rs.medikarton.model.Termin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TerminDao extends OsnovniDao<Termin> {

    private static final String KOLONE =
            "id, pacijent_id, lekar_id, pocetak, trajanje_min, status, razlog_dolaska";

    public TerminDao(Connection veza) {
        super(veza);
    }

    @Override
    protected String tabela() {
        return "termin";
    }

    @Override
    protected String kolone() {
        return KOLONE;
    }

    @Override
    protected String redosled() {
        return "pocetak";
    }

    @Override
    public Termin sacuvaj(Termin t) {
        String sql = """
                INSERT INTO termin (pacijent_id, lekar_id, pocetak, trajanje_min, status, razlog_dolaska)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = pripremiSaKljucem(sql)) {
            popuni(ps, t);
            t.setId(izvrsiInsert(ps));
            return t;
        } catch (SQLException e) {
            throw new PodaciException("Neuspelo cuvanje termina.", e);
        }
    }

    @Override
    public boolean azuriraj(Termin t) {
        if (t.getId() == null) {
            throw new IllegalArgumentException("Termin bez identifikatora ne moze da se azurira.");
        }
        String sql = """
                UPDATE termin SET pacijent_id = ?, lekar_id = ?, pocetak = ?,
                                  trajanje_min = ?, status = ?, razlog_dolaska = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = veza.prepareStatement(sql)) {
            popuni(ps, t);
            ps.setInt(7, t.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new PodaciException("Neuspelo azuriranje termina (id=" + t.getId() + ").", e);
        }
    }

    public List<Termin> aktivniZaLekaraNaDan(int lekarId, LocalDate dan) {
        String sql = """
                SELECT id, pacijent_id, lekar_id, pocetak, trajanje_min, status, razlog_dolaska
                FROM termin
                WHERE lekar_id = ?
                  AND pocetak >= ? AND pocetak < ?
                  AND status IN ('ZAKAZAN','POTVRDJEN')
                ORDER BY pocetak
                """;
        return lista(sql, ps -> {
            ps.setInt(1, lekarId);
            ps.setString(2, dan.atStartOfDay().toString());
            ps.setString(3, dan.plusDays(1).atStartOfDay().toString());
        }, "Neuspelo citanje termina lekara za dan " + dan + ".");
    }

    public List<Termin> zaPacijenta(int pacijentId) {
        String sql = "SELECT " + KOLONE + " FROM termin WHERE pacijent_id = ? ORDER BY pocetak DESC";
        return lista(sql, ps -> ps.setInt(1, pacijentId),
                "Neuspelo citanje termina pacijenta (id=" + pacijentId + ").");
    }

    public List<Termin> predstojeciZaPacijenta(int pacijentId, LocalDateTime od) {
        String sql = """
                SELECT id, pacijent_id, lekar_id, pocetak, trajanje_min, status, razlog_dolaska
                FROM termin
                WHERE pacijent_id = ? AND pocetak >= ? AND status IN ('ZAKAZAN','POTVRDJEN')
                ORDER BY pocetak
                """;
        return lista(sql, ps -> {
            ps.setInt(1, pacijentId);
            ps.setString(2, od.toString());
        }, "Neuspelo citanje predstojecih termina.");
    }

    private static void popuni(PreparedStatement ps, Termin t) throws SQLException {
        ps.setInt(1, t.getPacijentId());
        ps.setInt(2, t.getLekarId());
        ps.setString(3, tekst(t.getPocetak()));
        ps.setInt(4, t.getTrajanjeMin());
        ps.setString(5, t.getStatus().name());
        ps.setString(6, t.getRazlogDolaska());
    }

    @Override
    protected Termin procitaj(ResultSet rs) throws SQLException {
        Termin t = new Termin();
        t.setId(rs.getInt("id"));
        t.setPacijentId(rs.getInt("pacijent_id"));
        t.setLekarId(rs.getInt("lekar_id"));
        t.setPocetak(vreme(rs.getString("pocetak")));
        t.setTrajanjeMin(rs.getInt("trajanje_min"));
        t.setStatus(StatusTermina.izTeksta(rs.getString("status")));
        t.setRazlogDolaska(rs.getString("razlog_dolaska"));
        return t;
    }
}
