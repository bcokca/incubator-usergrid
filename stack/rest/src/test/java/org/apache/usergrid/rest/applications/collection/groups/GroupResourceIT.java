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
package org.apache.usergrid.rest.applications.collection.groups;


import java.util.Map;
import java.util.UUID;

import javax.rmi.CORBA.Util;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

import com.google.gson.JsonObject;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.usergrid.persistence.model.util.UUIDGenerator;
import org.apache.usergrid.utils.JsonUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.usergrid.cassandra.Concurrent;
import org.apache.usergrid.java.client.Client.Query;
import org.apache.usergrid.java.client.response.ApiResponse;
import org.apache.usergrid.rest.AbstractRestIT;
import org.apache.usergrid.rest.TestContextSetup;
import org.apache.usergrid.rest.test.resource.app.GroupsCollection;
import org.apache.usergrid.utils.UUIDUtils;

import static org.junit.Assert.*;

/** @author rockerston */
@Concurrent()
public class GroupResourceIT extends AbstractRestIT {
    private static Logger log = LoggerFactory.getLogger( GroupResourceIT.class );

    /*
    private static final String GROUP = "testGroup" + UUIDUtils.newTimeUUID();
    private static final String USER = "edanuff" + UUIDUtils.newTimeUUID();
*/

    @Rule
    public TestContextSetup context = new TestContextSetup( this );

    public GroupResourceIT() throws Exception { }

    /***
     *
     * Verify that we can create a group with a standard string in the name and path
     */
    @Test()
    public void createGroupValidation() throws IOException {

        GroupsCollection groups = context.groups();

        //create a group with a normal name
        String groupName = "testgroup";
        String groupPath = "testgroup";
        JsonNode testGroup = groups.create(groupName, groupPath);
        //verify the group was created
        assertNull(testGroup.get("errors"));
        assertEquals(testGroup.get("path").asText(), groupPath);
    }

    /***
     *
     * Verify that we can create a group with a slash in the name and path
     */
    @Test()
    public void createGroupSlashInNameAndPathValidation() throws IOException {

        GroupsCollection groups = context.groups();

        //create a group with a slash in the name
        String groupNameSlash = "test/group";
        String groupPathSlash = "test/group";
        JsonNode testGroup = groups.create( groupNameSlash, groupPathSlash );
        //verify the group was created
        assertNull( testGroup.get( "errors" ) );
        assertEquals( testGroup.get("path").asText(),groupPathSlash );
    }

    /***
     *
     * Verify that we can create a group with a space in the name
     */
    @Test()
    public void createGroupSpaceInNameValidation() throws IOException {

        GroupsCollection groups = context.groups();

        //create a group with a space in the name
        String groupName = "test group";
        String groupPath = "testgroup";
        try {
            JsonNode testGroup = groups.create(groupName, groupPath);
        } catch (UniformInterfaceException e) {
            //verify the correct error was returned
            JsonNode node = mapper.readTree( e.getResponse().getEntity( String.class ));
            assertEquals( "illegal_argument", node.get( "error" ).textValue() );
        }
    }

    /***
     *
     * Verify that we cannot create a group with a space in the path
     */
    @Test()
    public void createGroupSpaceInPathValidation() throws IOException {

        GroupsCollection groups = context.groups();

        //create a group with a space in the path
        String groupName = "testgroup";
        String groupPath = "test group";
        try {
            JsonNode testGroup = groups.create(groupName, groupPath);
        } catch (UniformInterfaceException e) {
            //verify the correct error was returned
            JsonNode node = mapper.readTree( e.getResponse().getEntity( String.class ));
            assertEquals( "illegal_argument", node.get( "error" ).textValue() );
        }
    }

    /***
     *
     * Verify that we cannot create a group with a space in the path
     */
    @Test
    public void postGroupActivity() throws IOException {


        //1. create a group
        GroupsCollection groups = context.groups();

        //create a group with a normal name
        String groupName = "groupTitle";
        String groupPath = "groupPath";
        JsonNode testGroup = groups.create(groupName, groupPath);
        //verify the group was created
        assertNull(testGroup.get("errors"));
        assertEquals(testGroup.get("path").asText(), groupPath);

        //2. post group activity

        //TODO: actually post a group activity
    }

    @Test
    public void addRemovePermission() throws IOException {

        GroupsCollection groups = context.groups();



        UUID id = UUIDUtils.newTimeUUID();

        String groupName = "groupname" + id;

        ApiResponse response = client.createGroup( groupName );
        assertNull( "Error was: " + response.getErrorDescription(), response.getError() );

        refreshIndex("test-organization", "test-app");

        UUID createdId = response.getEntities().get( 0 ).getUuid();

        // add Permission
        String orgName = context.getOrgName();
        String appName = context.getAppName();
        String path = "/"+orgName+"/"+appName+"/groups/";

        String json = "{\"permission\":\"delete:/test\"}";
        JsonNode node = mapper.readTree( resource().path( path + createdId + "/permissions" )
                .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class, json ));

        // check it
        assertNull( node.get( "errors" ) );
        assertEquals( node.get( "data" ).get( 0 ).asText(), "delete:/test" );

        refreshIndex("test-organization", "test-app");

        node = mapper.readTree( resource().path( "/test-organization/test-app/groups/" + createdId + "/permissions" )
                .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));
        assertNull( node.get( "errors" ) );
        assertEquals( node.get( "data" ).get( 0 ).asText(), "delete:/test" );


        // remove Permission

        node = mapper.readTree( resource().path( "/test-organization/test-app/groups/" + createdId + "/permissions" )
                .queryParam( "access_token", access_token ).queryParam( "permission", "delete%3A%2Ftest" )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE ).delete( String.class ));

        // check it
        assertNull( node.get( "errors" ) );
        assertTrue( node.get( "data" ).size() == 0 );

        refreshIndex("test-organization", "test-app");

        node = mapper.readTree( resource().path( "/test-organization/test-app/groups/" + createdId + "/permissions" )
                .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));
        assertNull( node.get( "errors" ) );
        assertTrue( node.get( "data" ).size() == 0 );
    }

/*
    @Test
    public void addRemoveRole() throws IOException {

        UUID id = UUIDUtils.newTimeUUID();

        String groupName = "groupname" + id;
        String roleName = "rolename" + id;

        ApiResponse response = client.createGroup( groupName );
        assertNull( "Error was: " + response.getErrorDescription(), response.getError() );

        UUID createdId = response.getEntities().get( 0 ).getUuid();

        refreshIndex("test-organization", "test-app");

        // create Role

        String json = "{\"title\":\"" + roleName + "\",\"name\":\"" + roleName + "\"}";
        JsonNode node = mapper.readTree( resource().path( "/test-organization/test-app/roles" ).queryParam( "access_token", access_token )
                        .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE )
                        .post( String.class, json ));

        // check it
        assertNull( node.get( "errors" ) );


        refreshIndex("test-organization", "test-app");

        // add Role

        node = mapper.readTree( resource().path( "/test-organization/test-app/groups/" + createdId + "/roles/" + roleName )
                .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).post( String.class ));

        refreshIndex("test-organization", "test-app");

        // check it
        assertNull( node.get( "errors" ) );
        assertEquals( node.get( "entities" ).get( 0 ).get( "name" ).asText(), roleName );

        node = mapper.readTree( resource().path( "/test-organization/test-app/groups/" + createdId + "/roles" )
                .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));
        assertNull( node.get( "errors" ) );
        assertEquals( node.get( "entities" ).get( 0 ).get( "name" ).asText(), roleName );

        // check root roles
        node = mapper.readTree( resource().path( "/test-organization/test-app/roles" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));
        assertNull( node.get( "errors" ) );
        assertTrue( node.get( "entities" ).findValuesAsText( "name" ).contains( roleName ) );

        refreshIndex("test-organization", "test-app");

        // remove Role

        node = mapper.readTree( resource().path( "/test-organization/test-app/groups/" + createdId + "/roles/" + roleName )
                .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).delete( String.class ));
        assertNull( node.get( "errors" ) );

        refreshIndex("test-organization", "test-app");

        node = mapper.readTree( resource().path( "/test-organization/test-app/groups/" + createdId + "/roles" )
                .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));
        assertNull( node.get( "errors" ) );
        assertTrue( node.get( "entities" ).size() == 0 );

        // check root roles - role should remain
        node = mapper.readTree( resource().path( "/test-organization/test-app/roles" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));
        assertNull( node.get( "errors" ) );
        assertTrue( node.get( "entities" ).findValuesAsText( "name" ).contains( roleName ) );

        // now kill the root role
        node = mapper.readTree( resource().path( "/test-organization/test-app/roles/" + roleName )
                .queryParam( "access_token", access_token ).accept( MediaType.APPLICATION_JSON )
                .type( MediaType.APPLICATION_JSON_TYPE ).delete( String.class ));
        assertNull( node.get( "errors" ) );

        refreshIndex("test-organization", "test-app");

        // now it should be gone
        node = mapper.readTree( resource().path( "/test-organization/test-app/roles" ).queryParam( "access_token", access_token )
                .accept( MediaType.APPLICATION_JSON ).type( MediaType.APPLICATION_JSON_TYPE ).get( String.class ));
        assertNull( node.get( "errors" ) );
        assertFalse( node.get( "entities" ).findValuesAsText( "name" ).contains( roleName ) );
    }
    */

}