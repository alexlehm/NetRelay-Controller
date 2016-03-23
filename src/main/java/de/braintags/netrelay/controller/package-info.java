/**
 * :numbered:
 * :toc: left
 * :toclevels: 3
 * 
 * = NetRelay^(R)^ Controller
 * 
 * This project is an extension of the project NetRelay and contains several implementations of
 * {@link de.braintags.netrelay.controller.IController}.
 * 
 * If you are searching for a very quick entry into NetRelay with a prepared, ready to use project based on NetRelay,
 * you should go to link:https://github.com/BraintagsGmbH/NetRelay-Demoproject[ Quickstart with NetRelay-Demoproject]
 * 
 * For basic information about NetRelay go to the https://github.com/BraintagsGmbH/NetRelay[ NetRelay documentation ]
 * 
 * 
 * == Using NetRelay-Controller inside your build environments
 * To use this project, add the following dependency to the _dependencies_ section of your build descriptor:
 * 
 * 
 * * Maven (in your `pom.xml`):
 *
 * [source,xml,subs="+attributes"]
 * ----
 * <dependency>
 *   <groupId>${maven.groupId}</groupId>
 *   <artifactId>${maven.artifactId}</artifactId>
 *   <version>${maven.version}</version>
 * </dependency>
 * ----
 *
 * * Gradle (in your `build.gradle` file):
 *
 * [source,groovy,subs="+attributes"]
 * ----
 * dependencies {
 *   compile '${maven.groupId}:${maven.artifactId}:${maven.version}'
 * }
 * ----
 *
 * == Controllers
 * 
 * 
 * 
 */
@Document(fileName = "index.adoc")
package de.braintags.netrelay.controller;

import io.vertx.docgen.Document;
