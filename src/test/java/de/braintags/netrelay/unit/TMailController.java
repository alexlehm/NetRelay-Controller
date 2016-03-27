/*
 * #%L
 * vertx-pojongo
 * %%
 * Copyright (C) 2015 Braintags GmbH
 * %%
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * #L%
 */
package de.braintags.netrelay.unit;

import org.junit.Test;

import de.braintags.netrelay.RequestUtil;
import de.braintags.netrelay.controller.BodyController;
import de.braintags.netrelay.controller.ThymeleafTemplateController;
import de.braintags.netrelay.controller.api.MailController;
import de.braintags.netrelay.init.Settings;
import de.braintags.netrelay.routing.RouterDefinition;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;

/**
 * 
 * 
 * @author Michael Remme
 * 
 */
public class TMailController extends NetRelayBaseConnectorTest {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(TMailController.class);
  public static final String TEST_IMAGE_URI = "http://www.braintags.de/images/design/logo.png";

  @Test
  public void sendSimpleMail(TestContext context) {
    try {
      String url = "/api/sendMail";
      Buffer responseBuffer = Buffer.buffer();
      testRequest(context, HttpMethod.POST, url, req -> {
        Buffer buffer = Buffer.buffer();
        buffer.appendString("to=" + NetRelayBaseTest.TESTS_MAIL_RECIPIENT);
        buffer.appendString("&subject=").appendString(RequestUtil.encodeText("Test sendSimpleMail"));
        buffer.appendString("&mailText=").appendString(RequestUtil.encodeText("super cleverer text als nachricht"));

        req.headers().set("content-length", String.valueOf(buffer.length()));
        req.headers().set("content-type", "application/x-www-form-urlencoded");
        req.write(buffer);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        JsonObject json = new JsonObject(resp.content.toString());
        context.assertTrue(json.getBoolean("success"), "success flag not set");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  @Test
  public void sendHtmlMessage(TestContext context) {
    try {
      String url = "/api/sendMail";
      Buffer responseBuffer = Buffer.buffer();
      testRequest(context, HttpMethod.POST, url, req -> {
        Buffer buffer = Buffer.buffer();
        buffer.appendString("to=" + NetRelayBaseTest.TESTS_MAIL_RECIPIENT);
        buffer.appendString("&subject=").appendString(RequestUtil.encodeText("Test sendHtmlMessage"));
        buffer.appendString("&mailText=").appendString(RequestUtil.encodeText("super cleverer text als nachricht"));
        buffer.appendString("&htmlText=")
            .appendString(RequestUtil.encodeText("this is html text <a href=\"braintags.de\">braintags.de</a>"));

        req.headers().set("content-length", String.valueOf(buffer.length()));
        req.headers().set("content-type", "application/x-www-form-urlencoded");
        req.write(buffer);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        JsonObject json = new JsonObject(resp.content.toString());
        context.assertTrue(json.getBoolean("success"), "success flag not set");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  @Test
  public void sendHtmlMessageWithInlineImage(TestContext context) {
    try {
      String url = "/api/sendMail";
      Buffer responseBuffer = Buffer.buffer();
      testRequest(context, HttpMethod.POST, url, req -> {
        Buffer buffer = Buffer.buffer();
        buffer.appendString("to=" + NetRelayBaseTest.TESTS_MAIL_RECIPIENT);
        buffer.appendString("&subject=").appendString(RequestUtil.encodeText("Test sendHtmlMessageWithInlineImage"));
        // buffer.appendString("&mailText=").appendString(RequestUtil.encodeText("super cleverer text als nachricht"));
        buffer.appendString("&htmlText=").appendString(
            RequestUtil.encodeText("this is html text <a href=\"braintags.de\">braintags.de</a> with an <img src=\""
                + TEST_IMAGE_URI + "\"/>"));

        req.headers().set("content-length", String.valueOf(buffer.length()));
        req.headers().set("content-type", "application/x-www-form-urlencoded");
        req.write(buffer);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        JsonObject json = new JsonObject(resp.content.toString());
        context.assertTrue(json.getBoolean("success"), "success flag not set");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  @Test
  public void sendHtmlMessageByTemplate(TestContext context) {
    try {
      String url = "/api/sendMail";
      Buffer responseBuffer = Buffer.buffer();
      testRequest(context, HttpMethod.POST, url, req -> {
        Buffer buffer = Buffer.buffer();
        buffer.appendString("to=" + NetRelayBaseTest.TESTS_MAIL_RECIPIENT);
        buffer.appendString("&subject=").appendString(RequestUtil.encodeText("Test sendHtmlMessageByTemplate"));
        buffer.appendString("&mailText=").appendString(RequestUtil.encodeText("super cleverer text als nachricht"));
        buffer.appendString("&template=").appendString(RequestUtil.encodeText("mailing/customerMail.html"));

        req.headers().set("content-length", String.valueOf(buffer.length()));
        req.headers().set("content-type", "application/x-www-form-urlencoded");
        req.write(buffer);
      }, resp -> {
        LOGGER.info("RESPONSE: " + resp.content);
        JsonObject json = new JsonObject(resp.content.toString());
        context.assertTrue(json.getBoolean("success"), "success flag not set");
      }, 200, "OK", null);
    } catch (Exception e) {
      context.fail(e);
    }
  }

  @Override
  public void modifySettings(TestContext context, Settings settings) {
    super.modifySettings(context, settings);
    initMailClient(settings);
    RouterDefinition def = defineRouterDefinition(MailController.class, "/api/sendMail");
    def.getHandlerProperties().put(MailController.FROM_PARAM, TESTS_MAIL_FROM);
    def.getHandlerProperties().put(ThymeleafTemplateController.TEMPLATE_DIRECTORY_PROPERTY, "testTemplates");
    def.getHandlerProperties().put(MailController.INLINE_PROP, "true");

    settings.getRouterDefinitions().addAfter(BodyController.class.getSimpleName(), def);
  }

}
