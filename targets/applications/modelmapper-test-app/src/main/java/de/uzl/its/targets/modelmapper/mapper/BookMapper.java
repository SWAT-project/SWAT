package de.uzl.its.targets.modelmapper.mapper;

import java.util.List;
import java.util.stream.Collectors;

import de.uzl.its.targets.modelmapper.dto.BookDto;
import de.uzl.its.targets.modelmapper.model.Book;
import jakarta.annotation.PostConstruct;


import org.springframework.stereotype.Component;

@Component
public class BookMapper extends GenericMapper<Book, BookDto> {

    public BookMapper() {
        super(Book.class, BookDto.class);
    }
    @PostConstruct
    private void postConstruct() {
        BookMapping.addMapping(modelMapper);
    }

    @Override
    public List<BookDto> map2DTOList(List<Book> list) {
        return list.stream().map(this::map2DTO).collect(Collectors.toList());
    }

    @Override
    public List<Book> map2ModelList(List<BookDto> list) {
        return list.stream().map(this::map2Model).collect(Collectors.toList());
    }
}