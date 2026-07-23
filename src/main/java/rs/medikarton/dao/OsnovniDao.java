package rs.medikarton.dao;

import rs.medikarton.izuzeci.PodaciException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

abstract class OsnovniDao<T> implements Dao<T> {

    protected final Connection veza;

    protected OsnovniDao(Connection veza) {
        this.veza = Objects.requireNonNull(veza, "Veza ka bazi ne sme biti null.");
    }

    protected abstract String tabela();

    protected abstract String kolone();

    protected abstract String redosled();

    protected abstract T procitaj(ResultSet rs) throws SQLException;

    @Override
    public boolean obrisi(int id) {
        String sql = "DELETE FROM " + tabela() + " WHERE id = ?";
        try (PreparedStatement ps = veza.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new PodaciException("Neuspelo brisanje iz tabele " + tabela() + " (id=" + id + ").", e);
        }
    }

    @Override
    public int prebroj() {
        String sql = "SELECT COUNT(*) FROM " + tabela();
        try (PreparedStatement ps = veza.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new PodaciException("Neuspelo prebrojavanje u tabeli " + tabela() + ".", e);
        }
    }

    @Override
    public Optional<T> nadjiPoId(int id) {
        return jedan("SELECT " + kolone() + " FROM " + tabela() + " WHERE id = ?",
                ps -> ps.setInt(1, id), "Neuspelo citanje iz tabele " + tabela() + ".");
    }

    @Override
    public List<T> svi() {
        return lista("SELECT " + kolone() + " FROM " + tabela() + " ORDER BY " + redosled(),
                ps -> { }, "Neuspelo citanje tabele " + tabela() + ".");
    }

    protected Optional<T> jedan(String sql, PostavljacParametara parametri, String poruka) {
        try (PreparedStatement ps = veza.prepareStatement(sql)) {
            parametri.postavi(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(procitaj(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PodaciException(poruka, e);
        }
    }

    protected List<T> lista(String sql, PostavljacParametara parametri, String poruka) {
        List<T> rezultat = new ArrayList<>();
        try (PreparedStatement ps = veza.prepareStatement(sql)) {
            parametri.postavi(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rezultat.add(procitaj(rs));
                }
            }
            return rezultat;
        } catch (SQLException e) {
            throw new PodaciException(poruka, e);
        }
    }

    protected int izvrsiInsert(PreparedStatement ps) throws SQLException {
        ps.executeUpdate();
        try (ResultSet kljucevi = ps.getGeneratedKeys()) {
            if (kljucevi.next()) {
                return kljucevi.getInt(1);
            }
        }
        throw new SQLException("Baza nije vratila generisani kljuc.");
    }

    protected PreparedStatement pripremiSaKljucem(String sql) throws SQLException {
        return veza.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    }

    protected static String tekst(LocalDateTime vreme) {
        return vreme == null ? null : vreme.toString();
    }

    protected static String tekst(LocalDate datum) {
        return datum == null ? null : datum.toString();
    }

    protected static LocalDateTime vreme(String tekst) {
        return (tekst == null || tekst.isBlank()) ? null : LocalDateTime.parse(tekst);
    }

    protected static LocalDate datum(String tekst) {
        return (tekst == null || tekst.isBlank()) ? null : LocalDate.parse(tekst);
    }

    protected static Integer celiBrojIliNull(ResultSet rs, String kolona) throws SQLException {
        int vrednost = rs.getInt(kolona);
        return rs.wasNull() ? null : vrednost;
    }

    protected static void postaviCeoBrojIliNull(PreparedStatement ps, int indeks, Integer vrednost)
            throws SQLException {
        if (vrednost == null) {
            ps.setNull(indeks, java.sql.Types.INTEGER);
        } else {
            ps.setInt(indeks, vrednost);
        }
    }

    @FunctionalInterface
    protected interface PostavljacParametara {
        void postavi(PreparedStatement ps) throws SQLException;
    }
}
