package de.uzl.its.targets.controller;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.util.ArrayList;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @RequestMapping(value = "/test/{id}", method = GET)
    public String test1(@PathVariable("id") int id) {
        if(id == 42) {
            System.out.println("Hidden URI with id = 42 has been called");
        }
        return "The dynamic URI has been called with param " + id;
    }
}
