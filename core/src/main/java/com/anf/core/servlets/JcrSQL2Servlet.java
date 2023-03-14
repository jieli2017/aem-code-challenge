package com.anf.core.servlets;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.jcr.query.Query.JCR_SQL2;

@Component(service = { Servlet.class })
@SlingServletPaths(
  value = "/bin/jcrsql2query"
)
/***Begin Code - Jie Li Test 3***/
public class JcrSQL2Servlet  extends SlingSafeMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(JcrSQL2Servlet.class);;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        try {
            ResourceResolver resourceResolver = request.getResourceResolver();
            Session session = resourceResolver.adaptTo(Session.class);
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            String sql = "SELECT page.* FROM [cq:Page] AS page "
                             + "INNER JOIN [cq:PageContent] AS jcrcontent ON ISCHILDNODE(jcrcontent, page) "
                             + "WHERE ISDESCENDANTNODE(page ,\"/content/anf-code-challenge/us/en\") "
                             + "AND jcrcontent.[anfCodeChallenge] IS NOT NULL";
            Query query = queryManager.createQuery(sql, JCR_SQL2);
            query.setLimit(10);
            QueryResult result = query.execute();
            NodeIterator nodeIterator = result.getNodes();
            while(nodeIterator.hasNext()) {
                Node currentNode = nodeIterator.nextNode();
                response.getWriter().println(currentNode.getPath());
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }
}
/***END Code*****/