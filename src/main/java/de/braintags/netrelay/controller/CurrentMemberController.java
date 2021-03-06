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

import de.braintags.netrelay.MemberUtil;
import de.braintags.netrelay.controller.authentication.AuthenticationController;
import de.braintags.netrelay.model.Member;
import de.braintags.netrelay.routing.RouterDefinition;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * If a user is logged in, the propriate record is fetched from the datastore and stored as
 * {@link Member#CURRENT_USER_PROPERTY} in the context. Extensions of this class may overwrite the method
 * {@link #loadMemberData(Member, RoutingContext, Handler)} to load additional data.
 * 
 * <br>
 * <br>
 * Config-Parameter:<br/>
 * <br>
 * Request-Parameter:<br/>
 * <br/>
 * Result-Parameter:<br/>
 * {@link Member#CURRENT_USER_PROPERTY} in the context<br/>
 * 
 * @author Michael Remme
 */
public class CurrentMemberController extends AbstractController {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(CurrentMemberController.class);

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.core.Handler#handle(java.lang.Object)
   */
  @Override
  public final void handle(RoutingContext context) {
    loadMember(context, result -> {
      if (result.failed()) {
        context.fail(result.cause());
      } else {
        Member member = context.get(Member.CURRENT_USER_PROPERTY);
        loadMemberData(member, context, dataResult -> {
          if (dataResult.failed()) {
            context.fail(dataResult.cause());
          } else {
            context.next();
          }
        });
      }
    });
  }

  /**
   * Extensions may load additional data for the current member
   * 
   * @param member
   *          the member, if logged in or null
   * @param context
   *          the context of the current request
   * @param handler
   *          the handler to be informed
   */
  protected void loadMemberData(Member member, RoutingContext context, Handler<AsyncResult<Void>> handler) {
    handler.handle(Future.succeededFuture());
  }

  /**
   * Loads the data of a logged in member and stores them in the context
   * 
   * @param context
   * @param handler
   */
  private final void loadMember(RoutingContext context, Handler<AsyncResult<Void>> handler) {
    if (context.user() != null) {
      try {
        Class<? extends Member> mapperClass = getMapperClass(context);
        MemberUtil.getCurrentUser(context, getNetRelay().getDatastore(), mapperClass, res -> {
          if (res.failed()) {
            handler.handle(Future.failedFuture(res.cause()));
          } else {
            Member user = res.result();
            context.put(Member.CURRENT_USER_PROPERTY, user);
            MemberUtil.setCurrentUser(user, context);
            handler.handle(Future.succeededFuture());
          }
        });
      } catch (Exception e) {
        handler.handle(Future.failedFuture(e));
      }
    } else {
      handler.handle(Future.succeededFuture());
    }
  }

  private Class<? extends Member> getMapperClass(RoutingContext context) {
    String mapperName = context.user().principal().getString(AuthenticationController.MAPPERNAME_IN_PRINCIPAL);
    if (mapperName == null) {
      throw new IllegalArgumentException("No mapper definition found in principal");
    }

    Class<? extends Member> mapperClass = getNetRelay().getSettings().getMappingDefinitions()
        .getMapperClass(mapperName);
    if (mapperClass == null) {
      throw new IllegalArgumentException("No MapperClass definition for: " + mapperName);
    }
    return mapperClass;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.AbstractController#initProperties(java.util.Properties)
   */
  @Override
  public void initProperties(Properties properties) {
  }

  /**
   * Creates a default definition for the current instance
   * 
   * @return
   */
  public static RouterDefinition createDefaultRouterDefinition() {
    RouterDefinition def = new RouterDefinition();
    def.setName(CurrentMemberController.class.getSimpleName());
    def.setBlocking(false);
    def.setController(CurrentMemberController.class);
    def.setHandlerProperties(getDefaultProperties());
    def.setRoutes(new String[] {});
    return def;
  }

  /**
   * Get the default properties for an implementation of StaticController
   * 
   * @return
   */
  public static Properties getDefaultProperties() {
    Properties json = new Properties();
    return json;
  }
}
