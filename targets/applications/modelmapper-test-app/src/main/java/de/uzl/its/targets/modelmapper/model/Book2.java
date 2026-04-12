package de.uzl.its.targets.modelmapper.model;

import java.util.Date;

public class Book2 {

    private String title;
    private Date year;
    private String displayAuthorName;
    private Long id;

    public Book2(String title, Date year, String displayAuthorName, Long id) {
        this.title = title;
        this.year = year;
        this.displayAuthorName = displayAuthorName;
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

    public String getDisplayAuthorName() {
        return displayAuthorName;
    }

    public void setDisplayAuthorName(String displayAuthorName) {
        this.displayAuthorName = displayAuthorName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
