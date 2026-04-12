package de.uzl.its.targets.simple_list_endpoint.controller;

import de.uzl.its.targets.simple_list_endpoint.dto.SimpleDto;
import de.uzl.its.targets.simple_list_endpoint.dto.SomeObject;
import de.uzl.its.targets.simple_list_endpoint.dto.SomeObjectWithList;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/simple-list-endpoint")
public class SimpleListEndpoint {

    @PostMapping("/justAnArray")
    @ResponseBody
    public ResponseEntity<String> justAnArray(@RequestBody String [] names) {
        if (names.length >= 10) {
            if (names[9].equals("NotMyName1")) {
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } else if (names.length >= 2) {
            if (names[2].equals("NotMyName2")) {
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/justAList")
    @ResponseBody
    public ResponseEntity<String> justAList(@RequestBody List<String> names) {
        if (names != null && names.contains("NotMyName1")) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/dtoWithList")
    @ResponseBody
    public ResponseEntity<String> dtoWithList(@RequestBody SimpleDto simpleDto) {
        if (simpleDto != null && simpleDto.names != null && simpleDto.names.contains("NotMyName2")) {
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/justAList2")
    @ResponseBody
    public ResponseEntity<String> justAList2(@RequestBody List<String> names) {
        if (names != null) {
            if (names.size() >= 2 && names.get(1).equals("NotMyName3")) {
                return new ResponseEntity<>(HttpStatus.OK);
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/dtoWithList2")
    @ResponseBody
    public ResponseEntity<String> dtoWithList2(@RequestBody SimpleDto simpleDto) {
        if (simpleDto != null && simpleDto.names != null) {
            if (simpleDto.names.size() >= 2 && simpleDto.names.get(1).equals("NotMyName4")) {
                return new ResponseEntity<>(HttpStatus.OK);
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/noList")
    @ResponseBody
    public ResponseEntity<String> noList(@RequestBody SomeObject someObject) {
        if (someObject != null) {
            if (someObject.getA() == 2 && someObject.getB() == 3) {
                return new ResponseEntity<>(HttpStatus.OK);
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/optionalObjectWithList")
    @ResponseBody
    public ResponseEntity<String> optionalObjectWithList(@RequestBody(required = false) SomeObjectWithList someObjectWithList) {
        if (someObjectWithList != null) {
            if (someObjectWithList.getList().get(2).equals("2") && someObjectWithList.getB() == 3) {
                return new ResponseEntity<>(HttpStatus.OK);
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    // @PostMapping("/listOfSomeObjects")
    // @ResponseBody
    // public ResponseEntity<String> dtoWithList2(@RequestBody List<SomeObject> someObjects) {
    //     if (someObjects != null && someObjects.size() > 1) {
    //         if (someObjects.get(0).getA() == 2 && someObjects.get(1).getB() == 3) {
    //             return new ResponseEntity<>(HttpStatus.OK);
    //         }
    //         return new ResponseEntity<>(HttpStatus.OK);
    //     } else {
    //         return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    //     }
    // }
}

