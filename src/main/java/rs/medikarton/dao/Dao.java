package rs.medikarton.dao;

import java.util.List;
import java.util.Optional;

public interface Dao<T> {

    T sacuvaj(T entitet);

    boolean azuriraj(T entitet);

    boolean obrisi(int id);

    Optional<T> nadjiPoId(int id);

    List<T> svi();

    int prebroj();
}
