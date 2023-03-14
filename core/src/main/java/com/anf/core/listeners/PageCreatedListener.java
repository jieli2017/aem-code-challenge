package com.anf.core.listeners;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.day.cq.wcm.api.PageEvent;
import com.day.cq.wcm.api.PageModification;

@Component(service = EventHandler.class,
  immediate = true,
  property = {EventConstants.EVENT_TOPIC + "=" + PageEvent.EVENT_TOPIC})
@ServiceDescription("add property after page created")
public class PageCreatedListener implements EventHandler {

    private static final Map<String, Object> AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, "anf-write-service");
    private static final String ROOT_PAGE_PATH = "/content/anf-code-challenge/us/en";

    @Reference
    private ResourceResolverFactory resolverFactory;

    @Override
    public void handleEvent(final Event event) {
        Iterator<PageModification> pageInfo = PageEvent.fromEvent(event).getModifications();
        while (pageInfo.hasNext()) {
            PageModification pageModification = pageInfo.next();
            String pagePath = pageModification.getPath();
            if (StringUtils.contains(pagePath, ROOT_PAGE_PATH) &&
                  pageModification.getType().equals(PageModification.ModificationType.CREATED)) {
                addPageCreated(pagePath);
            }
        }
    }

    private void addPageCreated(String pagePath) {
        try(ResourceResolver resolver = this.resolverFactory.getServiceResourceResolver(AUTH_INFO)) {
            Optional.of(resolver)
                    .map(r -> resolver.getResource(pagePath + "/jcr:content"))
                    .map(res -> res.adaptTo(ModifiableValueMap.class))
                    .map(m -> m.put("pageCreated", true));
            resolver.commit();
        } catch (Exception e) {

        }
    }

}
