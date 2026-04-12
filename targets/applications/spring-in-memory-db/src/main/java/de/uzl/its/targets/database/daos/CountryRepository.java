package de.uzl.its.targets.database.daos;

import de.uzl.its.targets.database.models.Country;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface CountryRepository extends CrudRepository<Country, Integer> {

    @Query("SELECT c FROM Country c WHERE c.name = ?1")
    Country getCountry(String name);

    @Query("SELECT c.id FROM Country c WHERE c.name = ?1")
    int getCountryId(String name);

    @Query("SELECT c.name FROM Country c WHERE c.id =?1")
    String getCountryName(int id);
}
