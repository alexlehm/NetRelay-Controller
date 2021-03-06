/*
 * #%L
 * netrelay
 * %%
 * Copyright (C) 2015 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.netrelay.controller;

import java.util.Properties;

import de.braintags.netrelay.RequestUtil;
import de.braintags.netrelay.exception.PropertyRequiredException;
import de.braintags.netrelay.routing.RouterDefinition;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * The RedirectController redirects fitting routes to the page, specified by property
 * {@link RedirectController#DESTINATION_PROPERTY}
 * 
 * <br>
 * <br>
 * Config-Parameter:<br/>
 * {@value #DESTINATION_PROPERTY} - the name of the property, which defines the destination, where the redirect will aim
 * to<br>
 * Request-Parameter:<br/>
 * <br/>
 * Result-Parameter:<br/>
 * <br/>
 * 
 * @author Michael Remme
 */
public class RedirectController extends AbstractController {
  /**
   * The propertyname to define the destination, where the current instance is redirecting to
   */
  public static final String DESTINATION_PROPERTY = "destination";

  private String destination;

  /**
   * 
   */
  public RedirectController() {
  }

  @Override
  public void initProperties(Properties properties) {
    if (!properties.containsKey(DESTINATION_PROPERTY)) {
      throw new PropertyRequiredException(DESTINATION_PROPERTY);
    }
    destination = properties.getProperty(DESTINATION_PROPERTY);
  }

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.core.Handler#handle(java.lang.Object)
   */
  @Override
  public void handle(RoutingContext context) {
    HttpServerResponse response = context.response();
    RequestUtil.sendRedirect(response, context.request(), destination);
  }

  /**
   * Creates a default definition for the current instance
   * 
   * @return
   */
  public static RouterDefinition createDefaultRouterDefinition() {
    RouterDefinition def = new RouterDefinition();
    def.setName(RedirectController.class.getSimpleName());
    def.setBlocking(false);
    def.setController(RedirectController.class);
    def.setHandlerProperties(getDefaultProperties());
    def.setRoutes(new String[] { "/" });
    return def;
  }

  /**
   * Get the default properties for an implementation of StaticController
   * 
   * @return
   */
  public static Properties getDefaultProperties() {
    Properties json = new Properties();
    json.put(DESTINATION_PROPERTY, "/index.html");
    return json;
  }
}
