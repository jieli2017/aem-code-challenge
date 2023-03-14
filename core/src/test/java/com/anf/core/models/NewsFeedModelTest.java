package com.anf.core.models;

import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.day.cq.wcm.api.Page;

import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AemContextExtension.class)
public class NewsFeedModelTest {

    private NewsFeedModel newsFeed;
    private Page page;
    private Resource resource;

    @BeforeEach
    public void setup(AemContext context) throws Exception {

        page = context.create().page("/content/mypage");
        resource = context.create().resource(page, "news",
          "sling:resourceType", "anf-code-challenge/components/newsfeed");

        newsFeed = resource.adaptTo(NewsFeedModel.class);
    }

    @Test
    void testGetTitle() throws Exception {

        //List<NewsModel> newsList = newsFeed.getNewsList();
        //assertNotNull(newsList.get(0).getTitle());
    }
}
