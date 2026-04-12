package de.pethmr.target.controller;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/books")
public class Books {

    @GetMapping("/new")
    @ResponseBody
    List<String> getNewBooks(@RequestParam String filter) {
        if (filter != null && filter.equals("Security")) {
            return new ArrayList<>(
                    List.of(
                            "Finding Vulnerabilities in the Wild",
                            "Hacker Psychology",
                            "Microarchitectural Side-Channels and Where to Find Them"));
        } else if (filter != null && filter.startsWith("Harry Potter")) {
            if (filter.equals("Harry Potter - All the things!")) {
                return new ArrayList<>(
                        List.of(
                                "Harry Potter and the Philosopher's Stone",
                                "Harry Potter and the Chamber of Secrets",
                                "Harry Potter and the Prisoner of Azkaban",
                                "Harry Potter and the Goblet of Fire",
                                "Harry Potter and the Order of the Phoenix",
                                "Harry Potter and the Half-Blood Prince",
                                "Harry Potter and the Deathly Hallows"));
            } else {
                return new ArrayList<>(
                        List.of(
                                "Harry Potter and the Chamber of Secrets",
                                "Harry Potter and the Goblet of Fire"));
            }
        } else {
            return new ArrayList<>(List.of("No such books!"));
        }
    }
}
