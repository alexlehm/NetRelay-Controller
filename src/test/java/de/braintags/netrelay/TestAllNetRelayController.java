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
package de.braintags.netrelay;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.braintags.netrelay.TestAllNetRelay;
import de.braintags.netrelay.unit.TAuthentication;
import de.braintags.netrelay.unit.TCaptureParameters;
import de.braintags.netrelay.unit.TDataTablesController;
import de.braintags.netrelay.unit.TFailureController;
import de.braintags.netrelay.unit.TMailController;
import de.braintags.netrelay.unit.TMailProcessor;
import de.braintags.netrelay.unit.TPasswordLost;
import de.braintags.netrelay.unit.TPersistenceSuite;
import de.braintags.netrelay.unit.TRegistration;
import de.braintags.netrelay.unit.TTemplateController;

/**
 * LET TSettings the last class
 * 
 * @author Michael Remme
 * 
 */
@RunWith(Suite.class)
@SuiteClasses({ TestAllNetRelay.class, TFailureController.class, TTemplateController.class, TCaptureParameters.class,
    TMailProcessor.class, TPersistenceSuite.class, TAuthentication.class, TRegistration.class, TPasswordLost.class,
    TDataTablesController.class, TMailController.class, })

public class TestAllNetRelayController {
  // -DBlockedThreadCheckInterval=10000000 -DWarningExceptionTime=10000000 -DtestTimeout=5
  // -Djava.util.logging.config.file=src/main/resources/logging.properties

}
