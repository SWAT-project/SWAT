package de.uzl.its.targets.database.controller;

import de.uzl.its.targets.database.daos.CountryRepository;
import de.uzl.its.targets.database.models.Country;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/h2")
public class H2Controller {

    private final CountryRepository repo;

    public H2Controller(CountryRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/country/{name}")
    @ResponseBody
    public Country getCountry(@PathVariable("name") String name) {

        Country c = repo.getCountry(name);

        if (c == null) {
            System.out.println("No such country\n");
            c = new Country();
            c.setId(-1);
            c.setName("No such country");
        } else if (c.getId() == 2) {
            System.out.println("You found country " + c.getName() + ".");
        } else {
            System.out.println("Are you looking for: (" + c.getName() + ", " + c.getId() + ")?\n");
        }

        return c;
    }

    @GetMapping("/country/id/{name}")
    @ResponseBody
    public int getCountryId(@PathVariable("name") String name) {

        int id = repo.getCountryId(name);

        if (id == -1) {
            System.out.println("No such country\n");
            id = -1;
        } else {
            System.out.println("Are you looking for: (" + id + ")?\n");
        }

        return id;
    }

    @GetMapping("/country/name/{id}")
    @ResponseBody
    public String getCountryById(@PathVariable("id") int id) {

        String cname = repo.getCountryName(id);

        if (cname == null) {
            System.out.println("No such country\n");
            cname = "WTF?";
        } else if (cname.equals("France")) {
            System.out.println("You arrived in France.");
        } else {
            System.out.println("Are you looking for: (" + cname + ")?\n");
        }

        return cname;
    }
}
