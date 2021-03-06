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
package de.braintags.netrelay.controller.authentication;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import de.braintags.io.vertx.pojomapper.dataaccess.query.IQuery;
import de.braintags.io.vertx.pojomapper.dataaccess.write.IWrite;
import de.braintags.io.vertx.pojomapper.mapping.IMapper;
import de.braintags.io.vertx.pojomapper.util.QueryHelper;
import de.braintags.io.vertx.util.exception.InitException;
import de.braintags.netrelay.RequestUtil;
import de.braintags.netrelay.controller.api.MailController;
import de.braintags.netrelay.controller.api.MailController.MailSendResult;
import de.braintags.netrelay.controller.persistence.PersistenceController;
import de.braintags.netrelay.mapping.NetRelayMapperFactory;
import de.braintags.netrelay.model.IAuthenticatable;
import de.braintags.netrelay.model.Member;
import de.braintags.netrelay.model.RegisterClaim;
import de.braintags.netrelay.routing.RouterDefinition;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.mongo.MongoAuth;
import io.vertx.ext.auth.mongo.impl.MongoAuthImpl;
import io.vertx.ext.web.RoutingContext;

/**
 * This controller performs a registration by using double-opt-in.
 * The first is a form, you will create, which must contain minimal two fields with the field names
 * {@value #EMAIL_FIELD_NAME} and {@value #PASSWORD_FIELD_NAME}. Additional fields can be defined by using the same
 * structure than in the {@link PersistenceController}, like mapperName.fieldName ( for example: "customer.lastName" )
 * <br/>
 * The controller knows two actions:<br/>
 * - the start of a registration,<br/>
 * where a user filled out a registration form and sends that. The start of the
 * registration is activated, when the request contains a form parameter {@value #PASSWORD_FIELD_NAME}<br/>
 * - the confirmation of a registration<br/>
 * which is typically performed when a user clicked a link in a confirmation mail and is activated, when the request
 * contains the parameter {@value #VALIDATION_ID_PARAM}<br/>
 * If none of this fits, then the controller will throw an exception java.lang.IllegalArgumentException: invalid action
 * for registration<br/>
 * <br/>
 * At first, for the registration, the system is performing some ( optional ) checks, for instance, wether the email for
 * the registrator exists already in the datastore. If every check is fine, the controller creates a new instance of
 * {@link RegisterClaim}, which contains all needed data to finish the registration. Previously created instances of
 * RegisterClaim with the same email address are deactivated. <br/>
 * The id of the RegisterClaim is stored in the context under the property {@value #VALIDATION_ID_PARAM}, additionally
 * the RegisterClaim itself is stored inside the context under "RegisterClaim". Further the email address of the current
 * client is stored in the context under the parameter {@link MailController#TO_PARAMETER}, so that the MailController
 * can use it later on to send the message. Additionally all request parameters are added to the context, so that they
 * can be used as content for the generated mail.<br/>
 * After this, the MailController is called to compose and send the conformation mail to the client. The configuration
 * of the MailController must be contained inside the configuration of this RegisterController. The template, which is
 * part of that configuration, will be used to compose the confirmation mail, where the confirmation link must be
 * contained. The confirmation link has the structure:
 * <p>
 * confirmationPage?{@value #VALIDATION_ID_PARAM}=ID<br/>
 * The confirmation page can be any virtual page and must be defined as route for the RegisterController, so that it is
 * reacting to it. The ID is the ID, which was stored before in the context.<br/>
 * </p>
 * 
 * After successfully processing the MailController, the success page, defined by {@value #REG_START_SUCCESS_URL_PROP},
 * is called. <br/>
 * 
 * When a user clicks the link in the mail, the RegistrationController will perform the confirmation. It will fetch the
 * instance of RegisterClaim, which was previously created and generate a member, customer etc. from it and save it in
 * the datastore. After that it will call the success page, defined by {@value #REG_CONFIRM_SUCCESS_URL_PROP}
 * 
 * <br/>
 * <br/>
 * Config-Parameter:<br/>
 * <UL>
 * <LI>{@value #REG_START_SUCCESS_URL_PROP} - defines the url which is used, when the registration claim was successful.
 * Under this address the mail should be sent, where the confirmation link is integrated, like described up.
 * <LI>{@value #REG_START_FAIL_URL_PROP} - defines the url which is used, when the registration claim raised an error
 * <LI>{@value #REG_CONFIRM_SUCCESS_URL_PROP} - defines the url which is used, when the registration confirmation was
 * successfull
 * <LI>{@value #REG_CONFIRM_FAIL_URL_PROP} - defines the url which is used, when the registration confirmation raised an
 * error
 * <LI>{@value #AUTHENTICATABLE_CLASS_PROP} - the property name, which defines the class, which will be used to generate
 * a new member, user, customer etc.
 * <LI>Additionally the config-parameters of {@link MailController} must be set
 * <LI>additionally add the properties of {@link AbstractAuthProviderController} to allow direct login after successfull
 * registration confirmation
 * </UL>
 * <br>
 * 
 * Request-Parameter:<br/>
 * <UL>
 * <LI>for the start of a registration, a new instance of {@link RegisterClaim} is created by two fields first:
 * <UL>
 * <LI>email
 * <LI>password
 * </UL>
 * additional fields can be set by fields with the structure mapper.fieldName
 * <LI>confirmation of a registration: the parameter {@value #VALIDATION_ID_PARAM} must contain the id transported
 * before
 * 
 * </UL>
 * <br/>
 * 
 * Result-Parameter:<br/>
 * <UL>
 * <LI>{@value #REGISTER_ERROR_PARAM} the parameter, where an error String of a failed registration is stored in
 * the context. The codes are defined by {@link RegistrationCode}
 * <LI>{@value #VALIDATION_ID_PARAM} - on a successfull create action, the RegisterClaim is stored here and the request
 * is redirected to the success page, where the confirmation mail is created and sent. This parameter is keeping the
 * confirmatino id, which must be integrated into the link
 * </UL>
 * <br/>
 * 
 * @author Michael Remme
 *
 */
public class RegisterController extends AbstractAuthProviderController {
  private static final io.vertx.core.logging.Logger LOGGER = io.vertx.core.logging.LoggerFactory
      .getLogger(RegisterController.class);

  /**
   * The url, which shall be called after a successful registration start
   */
  public static final String REG_START_SUCCESS_URL_PROP = "regStartSuccessUrl";

  /**
   * The url, which shall be called after a failed register
   */
  public static final String REG_START_FAIL_URL_PROP = "regStartFailUrl";

  /**
   * The url, which shall be called after a successful registration confirmation
   */
  public static final String REG_CONFIRM_SUCCESS_URL_PROP = "regConfirmSuccessUrl";

  /**
   * The url, which shall be called after a failed confirmation
   */
  public static final String REG_CONFIRM_FAIL_URL_PROP = "regConfirmFailUrl";

  /**
   * The name of the class - as instance of {@link IAuthenticatable} - which will be used to save a successful and
   * improved registration as {@link Member} for instance
   */
  public static final String AUTHENTICATABLE_CLASS_PROP = "authenticatableClass";

  /**
   * Property defines, whether the system checks, wether an email exists already in the datastore
   */
  public static final String ALLOW_DUPLICATION_EMAIL_PROP = "allowDuplicateEmail";

  /**
   * The name of the parameter which is used to store error information in the context
   */
  public static final String REGISTER_ERROR_PARAM = "registerError";

  /**
   * The name of the parameter, which keeps the validation ID, which will be used on the success page to create the
   * validation link
   */
  public static final String VALIDATION_ID_PARAM = "validationId";

  /**
   * The name of the property which is used to store the {@link MailSendResult} in the context, if the mail sending
   * failed
   */
  private static final String MAIL_SEND_RESULT_PROP = "mailSendResult";

  /**
   * The name of the field used to send the password
   */
  public static final String PASSWORD_FIELD_NAME = "password";

  /**
   * The name of the field used to send the email
   */
  public static final String EMAIL_FIELD_NAME = "email";

  private String successUrl;
  private String failUrl;
  private String successConfirmUrl;
  private String failConfirmUrl;
  private Class<? extends IAuthenticatable> authenticatableCLass;
  private IMapper mapper;
  private boolean allowDuplicateEmail;
  private MailController.MailPreferences mailPrefs;
  private AuthProvider authProvider;

  /*
   * (non-Javadoc)
   * 
   * @see io.vertx.core.Handler#handle(java.lang.Object)
   */
  @Override
  public void handle(RoutingContext context) {
    if (hasParameter(context, PASSWORD_FIELD_NAME)) {
      registerStart(context);
    } else if (hasParameter(context, VALIDATION_ID_PARAM)) {
      registerConfirm(context);
    } else {
      context.fail(new IllegalArgumentException("invalid action for registration"));
    }
  }

  private void registerStart(RoutingContext context) {
    try {
      String password = readParameter(context, PASSWORD_FIELD_NAME, false);
      String email = readParameter(context, EMAIL_FIELD_NAME, false);
      checkEmail(email, emailRes -> {
        if (emailRes.failed()) {
          context.put(REGISTER_ERROR_PARAM, emailRes.cause().getMessage());
          context.reroute(failUrl);
        } else {
          checkPassword(password, pwRes -> {
            if (pwRes.failed()) {
              context.put(REGISTER_ERROR_PARAM, pwRes.cause().getMessage());
              context.reroute(failUrl);
            } else {
              createRegisterClaim(context, email, password, rcRes -> {
                if (rcRes.failed()) {
                  String message = rcRes.cause().getMessage();
                  context.put(REGISTER_ERROR_PARAM, message);
                  context.reroute(failUrl);
                } else {
                  RegisterClaim rc = rcRes.result();
                  LOGGER.info("Created RegisterClaim id is " + rc.id);
                  addParameterToContext(context, rc);
                  MailController.sendMail(context, getNetRelay().getMailClient(), mailPrefs, result -> {
                    MailController.MailSendResult msResult = result.result();
                    if (msResult.success) {
                      RequestUtil.sendRedirect(context.response(), successUrl);
                    } else {
                      context.put(MAIL_SEND_RESULT_PROP, msResult);
                      context.reroute(failUrl);
                    }
                  });
                }
              });
            }
          });
        }
      });
    } catch (Exception e) {
      LOGGER.error("", e);
      context.put(REGISTER_ERROR_PARAM, e.getMessage());
      context.reroute(failUrl);
    }
  }

  private void addParameterToContext(RoutingContext context, RegisterClaim claim) {
    claim.requestParameter.entrySet().forEach(entry -> context.put(entry.getKey(), entry.getValue()));
  }

  private void createRegisterClaim(RoutingContext context, String email, String password,
      Handler<AsyncResult<RegisterClaim>> handler) {
    deactivatePreviousClaims(context, email, previous -> {
      if (previous.failed()) {
        handler.handle(Future.failedFuture(previous.cause()));
      } else {
        RegisterClaim rc = new RegisterClaim(email, password, context.request());
        IWrite<RegisterClaim> write = getNetRelay().getDatastore().createWrite(RegisterClaim.class);
        write.add(rc);
        write.save(sr -> {
          if (sr.failed()) {
            LOGGER.error("", sr.cause());
            handler.handle(Future.failedFuture(sr.cause()));
          } else {
            context.put(RegisterClaim.class.getSimpleName(), rc);
            context.put(MailController.TO_PARAMETER, email);
            context.put(VALIDATION_ID_PARAM, rc.id);
            handler.handle(Future.succeededFuture(rc));
          }
        });
      }
    });
  }

  private void deactivatePreviousClaims(RoutingContext context, String email, Handler<AsyncResult<Void>> handler) {
    IQuery<RegisterClaim> query = getNetRelay().getDatastore().createQuery(RegisterClaim.class);
    query.field("email").is(email).field("active").is(true);
    QueryHelper.executeToList(query, qr -> {
      if (qr.failed()) {
        handler.handle(Future.failedFuture(qr.cause()));
      } else {
        List<RegisterClaim> cl = (List<RegisterClaim>) qr.result();
        if (!cl.isEmpty()) {
          IWrite<RegisterClaim> write = getNetRelay().getDatastore().createWrite(RegisterClaim.class);
          cl.forEach(rc -> rc.active = false);
          write.addAll(cl);
          write.save(wr -> {
            if (wr.failed()) {
              handler.handle(Future.failedFuture(wr.cause()));
            } else {
              handler.handle(Future.succeededFuture());
            }
          });
        } else {
          handler.handle(Future.succeededFuture());
        }
      }
    });
  }

  private void checkPassword(String password, Handler<AsyncResult<RegistrationCode>> handler) {
    if (password == null || password.hashCode() == 0) {
      handler.handle(Future.failedFuture(RegistrationCode.PASSWORD_REQUIRED.toString()));
    } else {
      handler.handle(Future.succeededFuture(RegistrationCode.OK));
    }
  }

  private void checkEmail(String email, Handler<AsyncResult<RegistrationCode>> handler) {
    if (email == null || email.hashCode() == 0) {
      handler.handle(Future.failedFuture(RegistrationCode.EMAIL_REQUIRED.toString()));
    } else if (!allowDuplicateEmail) {
      IQuery<? extends IAuthenticatable> query = getNetRelay().getDatastore().createQuery(this.authenticatableCLass);
      query.field("email").is(email);
      query.executeCount(qr -> {
        if (qr.failed()) {
          LOGGER.error("", qr.cause());
          handler.handle(Future.failedFuture(qr.cause()));
        } else {
          if (qr.result().getCount() > 0) {
            handler.handle(Future.failedFuture(RegistrationCode.EMAIL_EXISTS.toString()));
          } else {
            handler.handle(Future.succeededFuture(RegistrationCode.OK));
          }
        }
      });
    } else {
      handler.handle(Future.succeededFuture(RegistrationCode.OK));
    }
  }

  private void registerConfirm(RoutingContext context) {
    try {
      String claimId = context.request().getParam(VALIDATION_ID_PARAM);
      QueryHelper.findRecordById(getNetRelay().getDatastore(), RegisterClaim.class, claimId, cr -> {
        if (cr.failed()) {
          context.fail(cr.cause());
        } else {
          if (cr.result() == null) {
            context.put(REGISTER_ERROR_PARAM, RegistrationCode.CONFIRMATION_FAILURE);
            context.reroute(failConfirmUrl);
          } else {
            finishConfirm(context, cr);
          }
        }
      });
    } catch (Exception e) {
      LOGGER.error("", e);
      context.put(REGISTER_ERROR_PARAM, e.getMessage());
      context.reroute(failConfirmUrl);
    }
  }

  /**
   * @param context
   * @param cr
   */
  private void finishConfirm(RoutingContext context, AsyncResult<?> cr) {
    RegisterClaim rc = (RegisterClaim) cr.result();
    toAuthenticatable(context, rc, acRes -> {
      if (acRes.failed()) {
        LOGGER.error("", acRes.cause());
        context.put(REGISTER_ERROR_PARAM, acRes.cause().getMessage());
        context.reroute(failConfirmUrl);
      } else {
        RequestUtil.sendRedirect(context.response(), successConfirmUrl);
      }
    });
  }

  @SuppressWarnings({ "unchecked" })
  private void toAuthenticatable(RoutingContext context, RegisterClaim rc, Handler<AsyncResult<Void>> handler) {
    NetRelayMapperFactory mapperFactory = (NetRelayMapperFactory) getNetRelay().getNetRelayMapperFactory();
    Map<String, String> props = extractPropertiesFromMap(mapper.getMapperClass().getSimpleName(), rc.requestParameter);
    mapperFactory.getStoreObjectFactory().createStoreObject(props, mapper, result -> {
      if (result.failed()) {
        handler.handle(Future.failedFuture(result.cause()));
      } else {
        IAuthenticatable user = (IAuthenticatable) result.result().getEntity();
        user.setEmail(rc.email);
        user.setPassword(rc.password);
        IWrite<IAuthenticatable> write = (IWrite<IAuthenticatable>) getNetRelay().getDatastore()
            .createWrite(authenticatableCLass);
        write.add(user);
        write.save(wr -> {
          if (wr.failed()) {
            handler.handle(Future.failedFuture(wr.cause()));
          } else {
            deactivateRegisterClaim(rc);
            doUserLogin(user, handler);
          }
        });
      }
    });
  }

  private void doUserLogin(IAuthenticatable user, Handler<AsyncResult<Void>> handler) {
    AuthProvider auth = getAuthProvider();
    if (auth == null) {
      handler.handle(Future.succeededFuture());
    } else if (auth instanceof AuthProviderProxy) {
      try {
        AuthProviderProxy mAuth = (AuthProviderProxy) auth;
        JsonObject authInfo = getAuthObject(user, mAuth);
        mAuth.authenticate(authInfo, res -> {
          if (res.failed()) {
            LOGGER.warn("Unsuccessfull login", res.cause());
            handler.handle(Future.failedFuture(res.cause()));
          } else {
            LOGGER.info("direct login successfull");
            handler.handle(Future.succeededFuture());
          }
        });
      } catch (Exception e) {
        handler.handle(Future.failedFuture(e));
      }
    } else {
      handler.handle(Future.failedFuture(new UnsupportedOperationException(
          "unsupported AuthProvider for direct login: " + auth.getClass().getName())));
    }
  }

  private JsonObject getAuthObject(IAuthenticatable user, AuthProviderProxy proxy) {
    AuthProvider prov = proxy.getProvider();
    if (prov instanceof MongoAuthImpl) {
      JsonObject authInfo = new JsonObject();
      authInfo.put(((MongoAuthImpl) prov).getUsernameCredentialField(), user.getEmail())
          .put(((MongoAuthImpl) prov).getPasswordCredentialField(), user.getPassword());
      return authInfo;
    }
    throw new UnsupportedOperationException("Unsupported authprovider class: " + prov.getClass());
  }

  // let it run async and don't wait
  private void deactivateRegisterClaim(RegisterClaim claim) {
    claim.active = false;
    IWrite<RegisterClaim> write = getNetRelay().getDatastore().createWrite(RegisterClaim.class);
    write.add(claim);
    write.save(wr -> {
      if (wr.failed()) {
        LOGGER.error("", wr.cause());
      }
    });
  }

  // TODO This method should be integrated into NetRelayStoreObjectFactory to be executed before generation of instance
  // and then replaced in InsertAction either
  private Map<String, String> extractPropertiesFromMap(String entityName, Map<String, String> attrs) {
    Map<String, String> returnList = new HashMap<>();
    String startKey = entityName.toLowerCase() + ".";
    Iterator<Entry<String, String>> it = attrs.entrySet().iterator();
    while (it.hasNext()) {
      Entry<String, String> entry = it.next();
      String key = entry.getKey().toLowerCase();
      if (key.startsWith(startKey)) {
        String pureKey = key.substring(startKey.length());
        String value = entry.getValue();
        returnList.put(pureKey, value);
      }
    }
    return returnList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.braintags.netrelay.controller.AbstractController#initProperties(java.util.Properties)
   */
  @SuppressWarnings("unchecked")
  @Override
  public void initProperties(Properties properties) {
    successUrl = readProperty(REG_START_SUCCESS_URL_PROP, null, true);
    failUrl = readProperty(REG_START_FAIL_URL_PROP, null, true);
    successConfirmUrl = readProperty(REG_CONFIRM_SUCCESS_URL_PROP, null, true);
    failConfirmUrl = readProperty(REG_CONFIRM_FAIL_URL_PROP, null, true);
    try {
      authenticatableCLass = (Class<? extends IAuthenticatable>) Class
          .forName(readProperty(AUTHENTICATABLE_CLASS_PROP, Member.class.getName(), false));
    } catch (ClassNotFoundException e) {
      throw new InitException(e);
    }
    NetRelayMapperFactory mapperFactory = (NetRelayMapperFactory) getNetRelay().getNetRelayMapperFactory();
    mapper = mapperFactory.getMapper(authenticatableCLass);
    super.initProperties(properties);
    allowDuplicateEmail = Boolean.valueOf(readProperty(ALLOW_DUPLICATION_EMAIL_PROP, "false", false));
    mailPrefs = MailController.createMailPreferences(getVertx(), properties);
  }

  @Override
  protected AuthProviderProxy createAuthProvider(Properties properties) {
    String tmpAuthProvider = readProperty(AUTH_PROVIDER_PROP, null, false);
    if (tmpAuthProvider != null) {
      return super.createAuthProvider(properties);
    } else {
      return null;
    }
  }

  /**
   * Creates a default definition for the current instance
   * 
   * @return
   */
  public static RouterDefinition createDefaultRouterDefinition() {
    RouterDefinition def = new RouterDefinition();
    def.setName(RegisterController.class.getSimpleName());
    def.setBlocking(false);
    def.setController(RegisterController.class);
    def.setHandlerProperties(getDefaultProperties());
    def.setRoutes(new String[] { "/customer/doRegister", "/customer/verifyRegistration" });
    return def;
  }

  /**
   * Get the default properties for an implementation of StaticController
   * 
   * @return
   */
  public static Properties getDefaultProperties() {
    Properties json = new Properties();
    json.put(AUTH_PROVIDER_PROP, AUTH_PROVIDER_MONGO);
    // coming from IAuthenticatable
    json.put(MongoAuth.PROPERTY_PASSWORD_FIELD, "password");
    json.put(MongoAuth.PROPERTY_USERNAME_FIELD, "email");

    json.put(REG_START_SUCCESS_URL_PROP, "/customer/registerSuccess.html");
    json.put(REG_START_FAIL_URL_PROP, "/customer/registerFail.html");
    json.put(REG_CONFIRM_SUCCESS_URL_PROP, "/customer/registerConfirmSuccess.html");
    json.put(REG_CONFIRM_FAIL_URL_PROP, "/customer/registerConfirmFail.html");

    json.put(AUTHENTICATABLE_CLASS_PROP, Member.class.getName());
    json.put(MongoAuth.PROPERTY_COLLECTION_NAME, Member.class.getSimpleName());
    json.put(ALLOW_DUPLICATION_EMAIL_PROP, "false");
    return json;
  }

}
