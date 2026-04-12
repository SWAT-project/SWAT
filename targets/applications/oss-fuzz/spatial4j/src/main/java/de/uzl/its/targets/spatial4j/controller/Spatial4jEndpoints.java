package de.uzl.its.targets.spatial4j.controller;

import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.SpatialContextFactory;
import org.locationtech.spatial4j.exception.InvalidShapeException;
import java.text.ParseException;

import org.locationtech.spatial4j.shape.Shape;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/*
 * POLYGONs could supported with jts-core backend, however, as far as I can tell, oss-fuzz does not include it:
 *  https://github.com/google/oss-fuzz/tree/5111e1856f84b46233929072c155b8abab9d1d68/projects/spatial4j
 */
@RestController
@RequestMapping("/spatial4j")
public class Spatial4jEndpoints {

    @PostMapping("/readShapeFromWkt")
    public ResponseEntity<String> readShapeFromWkt(@RequestBody String shapeInput) {
        try {
            SpatialContext obj = new SpatialContext(new SpatialContextFactory());
            obj.readShapeFromWkt(shapeInput);
            // Shape s = obj.readShapeFromWkt(shapeInput);
            // System.out.println(s);
        } catch (InvalidShapeException | ParseException | AssertionError e) {
            // System.err.println(e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
