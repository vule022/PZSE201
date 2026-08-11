package rs.medikarton.bezbednost;

import rs.medikarton.izuzeci.PodaciException;
import rs.medikarton.model.Pacijent;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// NAMERNO RANJIV KOD ZA DEMONSTRACIJU
public class RanjiviPacijentDao {

    private final Connection veza;

    public RanjiviPacijentDao(Connection veza) {
        this.veza = veza;
    }

    public List<Pacijent> pretraziPoPrezimenu(String deoPrezimena) {
        String sql = "SELECT id, jmbg, ime, prezime, datum_rodjenja, pol, email, telefon, "
                + "adresa, krvna_grupa, alergije FROM pacijent WHERE prezime LIKE '"
                + deoPrezimena + "%'";
        List<Pacijent> rezultat = new ArrayList<>();
        try (Statement s = veza.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                Pacijent p = new Pacijent();
                p.setId(rs.getInt("id"));
                p.setJmbg(rs.getString("jmbg"));
                p.setIme(rs.getString("ime"));
                p.setPrezime(rs.getString("prezime"));
                p.setEmail(rs.getString("email"));
                p.setAlergije(rs.getString("alergije"));
                rezultat.add(p);
            }
            return rezultat;
        } catch (SQLException e) {
            throw new PodaciException("Ranjivi upit nije uspeo: " + sql, e);
        }
    }

    public boolean prijava(String korisnickoIme, String lozinkaHash) {
        String sql = "SELECT COUNT(*) FROM korisnik WHERE korisnicko_ime = '" + korisnickoIme
                + "' AND lozinka_hash = '" + lozinkaHash + "'";
        try (Statement s = veza.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new PodaciException("Ranjivi upit nije uspeo: " + sql, e);
        }
    }

    public List<String> pretragaSaDveKolone(String deoPrezimena) {
        String sql = "SELECT prezime, ime FROM pacijent WHERE prezime LIKE '" + deoPrezimena + "%'";
        List<String> rezultat = new ArrayList<>();
        try (Statement s = veza.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                rezultat.add(rs.getString(1) + " | " + rs.getString(2));
            }
            return rezultat;
        } catch (SQLException e) {
            throw new PodaciException("Ranjivi upit nije uspeo: " + sql, e);
        }
    }
}
