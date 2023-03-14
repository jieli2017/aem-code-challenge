package com.anf.core.servlets;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Session;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;

@Component(service = { Servlet.class })
@SlingServletPaths(
  value = "/bin/querybuilder"
)
/***Begin Code - Jie Li Exercise 3 ***/
public class QueryBuilderApiServlet extends SlingSafeMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(QueryBuilderApiServlet.class);

    @Reference
    private QueryBuilder builder;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        try {
            ResourceResolver resourceResolver = request.getResourceResolver();
            Session session = resourceResolver.adaptTo(Session.class);
            Map<String, String> predicate = new HashMap<>();

            predicate.put("path", "/content/anf-code-challenge/us/en");
            predicate.put("type", "cq:Page");
            predicate.put("p.limit", "10");
            predicate.put("property.operation", "exists");
            predicate.put("property", "jcr:content/anfCodeChallenge");

            Query query = builder.createQuery(PredicateGroup.create(predicate), session);
            SearchResult searchResult = query.getResult();

            for (Hit hit : searchResult.getHits()) {
                String path = hit.getPath();
                response.getWriter().println(path);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }
}
/***END Code*****/