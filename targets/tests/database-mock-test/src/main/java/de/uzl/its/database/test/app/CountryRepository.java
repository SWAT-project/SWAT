package de.uzl.its.database.test.app;

import org.springframework.data.repository.CrudRepository;
import java.util.ArrayList;
import java.util.List;

public class CountryRepository implements CrudRepository {

    private final List<Country> cs = new ArrayList<>();

    public CountryRepository() {
        super();
        cs.add(new Country(1, "Italy"));
        cs.add(new Country(2, "France"));
    }

    public Country getCountry(String name) {
        for (Country c : cs) {
            if (c.getName().equals(name)) {
                return c;
            }
        }

        return null;
    }

    public int getCountryId(String name) {
        for (Country c : cs) {
            if (c.getName().equals(name)) {
                return c.getId();
            }
        }

        return -1;
    }

    public String getCountryName(int id) {
        for (Country c : cs) {
            if (c.getId() == id) {
                return c.getName();
            }
        }

        return null;
    }
}
