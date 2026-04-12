package de.uzl.its.targets.java_diff_utils.controller;

import com.github.difflib.DiffUtils;

import de.uzl.its.targets.java_diff_utils.dto.DiffRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/java-diff-utils")
public class JavaDiffUtilsEndpoints {

    @PostMapping("/diffInline")
    public ResponseEntity<String> diffInline(@RequestBody DiffRequest diffRequest) {
        try {
            DiffUtils.diffInline(diffRequest.doc1, diffRequest.doc2);
        } catch (IllegalStateException e) {
            new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
