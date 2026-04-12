package de.uzl.its.targets.modelmapper.controller;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import de.uzl.its.targets.modelmapper.dto.BookDto;
import de.uzl.its.targets.modelmapper.mapper.BookMapper;
import de.uzl.its.targets.modelmapper.model.Book;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.uzl.its.targets.modelmapper.model.Book2;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/books")
public class Books {
    Logger logger = new LoggerContext().getLogger("de.uzl.its.targets.controller.Books");
    private final BookMapper bookMapper = new BookMapper();

    @GetMapping("/all")
    @ResponseBody
    List<BookDto> getBooks() {
        Logger logger = new LoggerContext().getLogger("de.uzl.its.targets.controller.Books");
        ModelMapper mapper = new ModelMapper();
        logger.debug("Test out!");
        return new ArrayList<>(
                List.of(
                        mapper.map(
                                new Book(
                                        "Finding Vulnerabilities in the Wild",
                                        new Date(2011, 11, 11),
                                        "ABC",
                                        123456L),
                                BookDto.class),
                        mapper.map(
                                new Book(
                                        "Hacker Psychology",
                                        new Date(2012, 12, 12),
                                        "XYZ",
                                        123456L),
                                BookDto.class),
                        mapper.map(
                                new Book(
                                        "Microarchitectural Side-Channels and Where to Find Them",
                                        new Date(2010, 10, 10),
                                        "MNO",
                                        123456L),
                                BookDto.class)));
    }

    @GetMapping("/one")
    @ResponseBody
    BookDto getBookByAuthor(@RequestParam String authorName) {
        Logger logger = new LoggerContext().getLogger("de.uzl.its.targets.controller.Books");
        ModelMapper mapper = new ModelMapper();
        logger.debug("Test out!");
        BookDto mappedBook =
                mapper.map(
                        new Book(
                                "Finding Vulnerabilities in the Wild",
                                new Date(2011, 11, 11),
                                authorName,
                                123456L),
                        BookDto.class);

        String authorName1 = mappedBook.getAuthorName();
        System.out.println("Author name: " + authorName1);
        if (authorName1.equals("Oddly specific author")) {
            return mappedBook;
        } else {
            return null;
        }
    }

    @GetMapping("/one1")
    @ResponseBody
    BookDto getBookByAuthor1(@RequestParam String authorName) {
        Logger logger = new LoggerContext().getLogger("de.uzl.its.targets.controller.Books");
        ModelMapper mapper = new ModelMapper();
        logger.debug("Test out!");
        BookDto mappedBook =
                mapper.map(
                        new Book(
                                "Finding Vulnerabilities in the Wild",
                                new Date(2011, 11, 11),
                                authorName,
                                123456L),
                        BookDto.class);

        String authorName1 = mappedBook.getAuthorName();
        System.out.println("Author name: " + authorName1);
        if (authorName.equals("Oddly specific author")) {
            return mappedBook;
        } else {
            return null;
        }
    }
    @GetMapping("/one2")
    @ResponseBody
    BookDto getBookByAuthor2(@RequestParam String authorName, @RequestParam String authorName2) {
        authorName2 = authorName2 + "!";
        Logger logger = new LoggerContext().getLogger("de.uzl.its.targets.controller.Books");
        ModelMapper mapper = new ModelMapper();
        logger.debug("Test out!");
        BookDto mappedBook =
                mapper.map(
                        new Book(
                                "Finding Vulnerabilities in the Wild",
                                new Date(2011, 11, 11),
                                authorName,
                                123456L),
                        BookDto.class);

        String authorName1 = mappedBook.getAuthorName();
        System.out.println("Author name: " + authorName1);
        if (authorName.equals(authorName2)) {
            return mappedBook;
        } else {
            return null;
        }
    }
    @GetMapping("/looseMatching")
    @ResponseBody
    BookDto getBookByAuthor3(@RequestParam String authorName, @RequestParam String authorName2) {
        authorName2 = authorName2 + "!";
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.LOOSE);
        logger.debug("Test out!");
        BookDto mappedBook =
                mapper.map(
                        new Book2(
                                "Finding Vulnerabilities in the Wild",
                                new Date(2011, 11, 11),
                                authorName,
                                123456L),
                        BookDto.class);

        String authorName1 = mappedBook.getAuthorName();
        System.out.println("Author name: " + authorName1);
        if (authorName.equals(authorName2)) {
            return mappedBook;
        } else {
            return null;
        }
    }
    @PostMapping("/newBook")
    @ResponseBody
    public BookDto newBook(@RequestBody BookDto newBook) throws Exception {
        String title = newBook.getTitle();
        logger.info("Create book '{}'.", title);
        Book book = bookMapper.map2Model(newBook);

        if (book == null) {
            throw new NullPointerException("Book is null");
        }

        BookDto bookDto = bookMapper.map2DTO(book);
        if (bookDto.getAuthorName().contains("secret")){
            System.out.println("Author name contains secret");
            bookDto.setAuthorName("REDACTED");
        }
        return bookDto;
    }
}

