package de.uzl.its.targets.gson.controller;

import java.io.*;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gson")
public class GsonEndpoints {

    @PostMapping("/read")
    public ResponseEntity<String> read(@RequestBody String data, @RequestParam(defaultValue = "false") boolean lenient) {
        TypeAdapter<JsonElement> adapter = new Gson().getAdapter(JsonElement.class);
        JsonReader reader = new JsonReader(new StringReader(data));
        reader.setLenient(lenient);
        try {
            while (reader.peek() != JsonToken.END_DOCUMENT) {
                adapter.read(reader);
            }
        } catch (JsonParseException | IllegalStateException | NumberFormatException | IOException expected) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/parse")
    public ResponseEntity<String> parse(@RequestBody String data) {
        try {
            JsonParser.parseString(data);
        } catch (JsonParseException expected) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/streamParser")
    public ResponseEntity<String> streamParser(@RequestBody String data) {
        try {
            JsonStreamParser parser = new JsonStreamParser(data);
            JsonElement element;
            while (parser.hasNext() == true) {
                // We do not catch NoSuchElementException here as we
                // have just checked an element exists.
                element = parser.next();
            }
        } catch (JsonParseException expected) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
