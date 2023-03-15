package com.anf.core.models;

import java.util.Iterator;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.day.cq.wcm.api.Page;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.sling.testing.mock.jcr.MockNodeTypes.NT_UNSTRUCTURED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doCallRealMethod;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
public class NewsFeedModelTest {

    private final AemContext ctx = new AemContext();

    private static final Logger LOG = LoggerFactory.getLogger(NewsFeedModelTest.class);

    @Mock
    private NewsFeedModel newsFeed;

    private NewsModel newsModel;

    @BeforeEach
    public void setup(AemContext context) throws Exception {
        ctx.addModelsForClasses(NewsFeedModel.class);
        ctx.addModelsForClasses(NewsModel.class);
        ctx.load().json("/com/anf/core/models/newsfeed.json", "/content");
        ctx.currentResource("/content/newsfeed");
        newsFeed = ctx.request().adaptTo(NewsFeedModel.class);
        Iterator<Resource> iterator = ctx.currentResource().listChildren();
        if (iterator.hasNext()) {
            newsModel = iterator.next().adaptTo(NewsModel.class);
        }
    }

    @Test
    void testGetTitle() throws Exception {
        assertNotNull(newsModel.getTitle());
    }

    @Test
    void testGetEmptyList() throws Exception {
        assertTrue(newsFeed.getNewsList().isEmpty());
    }
    @Test
    void testGetAuthor() throws Exception {
      assertNotNull(newsModel.getAuthor());
    }
    @Test
    void testGetCurrentDate() throws Exception {
        assertNotNull(newsModel.getCurrentDate());
    }
    @Test
    void testGetDescription() throws Exception {
        assertNotNull(newsModel.getDescription());
    }

    @Test
    void testGetContent() throws Exception {
        assertNotNull(newsModel.getContent());
    }

    @Test
    void testGetUrl() throws Exception {
        assertNotNull(newsModel.getUrl());
    }

    @Test
    void testGetUrlImage() throws Exception {
        assertNotNull(newsModel.getUrlImage());
    }

}
