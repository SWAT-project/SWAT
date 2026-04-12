package de.uzl.its.targets.modelmapper.mapper;/*
 * Open Hospital (www.open-hospital.org)
 * Copyright © 2006-2023 Informatici Senza Frontiere (info@informaticisenzafrontiere.org)
 *
 * Open Hospital is a free and open source software for healthcare data management.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * https://www.gnu.org/licenses/gpl-3.0-standalone.html
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import de.uzl.its.targets.modelmapper.dto.BookDto;
import de.uzl.its.targets.modelmapper.model.Book;
import org.modelmapper.ModelMapper;

import java.util.Date;

public class BookMapping {

    public static void addMapping(ModelMapper modelMapper) {

        modelMapper.getConfiguration().setAmbiguityIgnored(true);
        modelMapper.typeMap(Book.class, BookDto.class).addMappings(mapper -> {
            mapper.<String> map(Book::getAuthorName, BookDto::setAuthorName);
            mapper.<String> map(Book::getTitle, BookDto::setTitle);
            mapper.<Long> map(Book::getId, BookDto::setId);
            mapper.<Date> map(Book::getYear, BookDto::setYear);
        });

        modelMapper.typeMap(BookDto.class, Book.class).addMappings(mapper -> {
            mapper.<String> map(BookDto::getAuthorName, Book::setAuthorName);
            mapper.<String> map(BookDto::getTitle, Book::setTitle);
            mapper.<Long> map(BookDto::getId, Book::setId);
            mapper.<Date> map(BookDto::getYear, Book::setYear);
        });

    }
}