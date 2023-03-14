package com.anf.core.models;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

/***Begin Code - Jie Li Exercise 2 ***/

@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class NewsFeedModel {

    private static final String DEFAULT_NEW_DATA_PATH = "/var/commerce/products/anf-code-challenge/newsData";

    @SlingObject
    private ResourceResolver resourceResolver;

    @ValueMapValue
    @Default(values = DEFAULT_NEW_DATA_PATH)
    private String newsDataPath;

    private List<NewsModel> newsList = new ArrayList<>();

    @PostConstruct
    protected void init() {
        Resource newsResource = resourceResolver.getResource(newsDataPath);
        if (newsResource != null) {
            Iterator<Resource> iterator = newsResource.listChildren();
            while (iterator.hasNext()) {
                NewsModel newsModel = iterator.next().adaptTo(NewsModel.class);
                if (newsModel != null && StringUtils.isNotBlank(newsModel.getTitle())) {
                    this.newsList.add(newsModel);
                }
            }
        }
    }

    public List<NewsModel> getNewsList() {
        return this.newsList;
    }
}

/***END Code*****/