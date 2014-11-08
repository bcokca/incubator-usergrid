/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 package org.apache.usergrid.scenarios

import java.io.File
import java.nio.file.{Paths, Files}

import io.gatling.core.Predef._
import io.gatling.http.Predef._
 import org.apache.usergrid.datagenerators.FeederGenerator
 import scala.concurrent.duration._

import scala.io.Source

import org.apache.usergrid.settings.{Headers, Settings}

/**
 *
 * Creates a new device
 *
 * Expects:
 *
 * authToken The auth token to use when creating the application
 * orgName The name of the org
 * appName The name of the app
 * notifierName The name of the created notifier
 * deviceName the name of the device created to send the notification to
 *
 * Produces:
 *
 * N/A
 *
 *
 */
object NotificationScenarios {

  val notifier = Settings.pushNotifier

  /**
   * send the notification now
   */
  val sendNotification = exec(http("Send Single Notification")
      .post("/devices/${entityName}/notifications")
      .body(StringBody("{\"payloads\":{\"" + notifier + "\":\"testmessage\"}}"))
      .headers(Headers.jsonAuthorized)
      .check(status.is(200))
    )

  val sendNotificationToUser= exec(http("Send Notification to All Devices")
    .post("/users/${userId}/notifications")
    .body(StringBody("{\"payloads\":{\"" + notifier + "\":\"testmessage\"}}"))
    .headers(Headers.jsonAuthorized)
    .check(status.is(200))
  )


  val userFeeder = Settings.getUserFeeder
  val createScenario = scenario("Create Push Notification")

    /**
     * TODO Shawn, do we want to make this circular so it repeats? We're running out of users
     * before the test finishes http://gatling.io/docs/2.0.2/session/feeder.html?highlight=feeder#recordseqfeederbuilder
     *
     * .feed(userFeeder.circular)
     *
     */

    //not sure we want to do this, but the feeder runs out otherwise. It doe
    .exec(TokenScenarios.getManagementToken)
    .feed(userFeeder)
    //create the user
    .exec(UserScenarios.postUser)
//    .exec(TokenScenarios.getUserToken)
    .exec( UserScenarios.getUserByUsername)
//    .repeat(2){
//    feed(FeederGenerator.generateEntityNameFeeder("device", Settings.numDevices))
//      .exec( DeviceScenarios.postDeviceWithNotifier)
//      .exec(ConnectionScenarios.postUserToDeviceConnection)
//  }
//    .exec(session => {
//    // print the Session for debugging, don't do that on real Simulations
//    println(session)
//    session
//  })
//    exec( sendNotificationToUser)

  /**
   * TODO: Add posting to users, which would expect a user in the session
   */




}
