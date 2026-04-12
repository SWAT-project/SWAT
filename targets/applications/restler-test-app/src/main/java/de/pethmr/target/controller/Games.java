package de.pethmr.target.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/games")
public class Games {

    @GetMapping("/by-price")
    @ResponseBody
    String getRandomGameByPrice(@RequestParam float price) {
        if (price == 57.99) {
            return "Pacman";
        } else {
            return "Super Mario Bros.";
        }
    }
}
