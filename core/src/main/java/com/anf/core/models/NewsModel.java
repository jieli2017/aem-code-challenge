package com.anf.core.models;

import javax.annotation.PostConstruct;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class NewsModel {

    @ValueMapValue
    private String title;

    @ValueMapValue
    private String author;

    @ValueMapValue
    private String description;

    @ValueMapValue
    private String content;

    @ValueMapValue
    private String url;

    @ValueMapValue
    private String urlImage;

    //private String currentDate;

    @PostConstruct
    protected void init() {
        //this.currentDate = java.time.LocalDate.now().toString();
    }

    public String getTitle() {
        return this.title;
    }

    public String getAuthor() {
        return this.author;
    }

    public String getCurrentDate() {
        return java.time.LocalDate.now().toString();
    }

    public String getDescription() {
        return this.description;
    }

    public String getContent() {
        return this.content;
    }

    public String getUrl() {
        return this.url;
    }

    public String getUrlImage() {
        return this.urlImage;
    }

}
