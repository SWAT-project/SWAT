package de.pethmr.target.controller;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/dvds")
public class DVDs {

    @GetMapping("/classics/{start}/{end}")
    @ResponseBody
    List<String> getDVDsFromYearRange(@PathVariable int start, @PathVariable int end) {

        if (end < start) {
            return List.of("You must be drunk!");
        } else if (end < 1888) {
            return List.of("Sorry, no movies before 1888!");
        } else if (start > 2023) {
            return List.of("Time machines have yet to be invented!");
        } else {
            return List.of(
                    "Batman - The Dark Knight",
                    "Roundhay Garden Scene",
                    "The Perks of Being a Wallflower");
        }
    }
}
