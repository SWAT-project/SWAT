package de.uzl.its.targets.modelmapper.dto;

import java.util.Date;

public class BookDto {

    private String title;
    private Date year;
    private String authorName;
    private Long id;

    public BookDto() {}

    public BookDto(String title, Date year, String authorName, Long id) {
        this.title = title;
        this.year = year;
        this.authorName = authorName;
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getYear() {
        return year;
    }

    public void setYear(Date year) {
        this.year = year;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
