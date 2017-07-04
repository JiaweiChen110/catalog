/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package org.ow2.proactive.catalog.rest.controller;

import static com.google.common.truth.Truth.assertThat;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ow2.proactive.catalog.Application;
import org.ow2.proactive.catalog.dto.BucketMetadata;
import org.ow2.proactive.catalog.util.IntegrationTestUtil;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.io.ByteStreams;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ValidatableResponse;


/**
 * @author ActiveEon Team
 */
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { Application.class })
@WebIntegrationTest(randomPort = true)
public class CatalogObjectControllerIntegrationTest extends AbstractRestAssuredTest {

    private static final String CATALOG_OBJECTS_RESOURCE = "/buckets/{bucketId}/resources";

    private static final String CATALOG_OBJECT_RESOURCE = "/buckets/{bucketId}/resources/{name}";

    private static final String CATALOG_OBJECT_REVISIONS_RESOURCE = "/buckets/{bucketId}/resources/{name}/revisions";

    private static final String BUCKETS_RESOURCE = "/buckets";

    private static final String contentType = "application/xml";

    private BucketMetadata bucket;

    @Before
    public void setup() throws IOException {
        HashMap<String, Object> result = given().parameters("name",
                                                            "myBucket",
                                                            "owner",
                                                            "BucketControllerIntegrationTestUser")
                                                .when()
                                                .post(BUCKETS_RESOURCE)
                                                .then()
                                                .statusCode(HttpStatus.SC_CREATED)
                                                .extract()
                                                .path("");

        bucket = new BucketMetadata(((Integer) result.get("id")).longValue(),
                                    (String) result.get("name"),
                                    (String) result.get("owner"));

        // Add an object of kind "workflow" into first bucket
        given().pathParam("bucketId", bucket.getMetaDataId())
               .queryParam("kind", "workflow")
               .queryParam("name", "workflowname")
               .queryParam("commitMessage", "commit message")
               .queryParam("contentType", contentType)
               .multiPart(IntegrationTestUtil.getWorkflowFile("workflow.xml"))
               .when()
               .post(CATALOG_OBJECTS_RESOURCE)
               .then()
               .statusCode(HttpStatus.SC_CREATED);

    }

    @After
    public void cleanup() {
        IntegrationTestUtil.cleanup();
    }

    @Test
    public void testCreateWorkflowShouldReturnSavedWorkflow() {
        given().pathParam("bucketId", bucket.getMetaDataId())
               .queryParam("kind", "workflow")
               .queryParam("name", "workflow_test")
               .queryParam("commitMessage", "first commit")
               .queryParam("contentType", contentType)
               .multiPart(IntegrationTestUtil.getWorkflowFile("workflow.xml"))
               .when()
               .post(CATALOG_OBJECTS_RESOURCE)
               .then()
               .assertThat()
               .statusCode(HttpStatus.SC_CREATED)
               .body("object[0].bucket_id", is(bucket.getMetaDataId().intValue()))
               .body("object[0].kind", is("workflow"))
               .body("object[0].name", is("workflow_test"))

               .body("object[0].object_key_values", hasSize(6))
               //check job info
               .body("object[0].object_key_values[0].label", is("job_information"))
               .body("object[0].object_key_values[0].key", is("project_name"))
               .body("object[0].object_key_values[0].value", is("Project Name"))
               .body("object[0].object_key_values[1].label", is("job_information"))
               .body("object[0].object_key_values[1].key", is("name"))
               .body("object[0].object_key_values[1].value", is("Valid Workflow"))
               //check variables label
               .body("object[0].object_key_values[2].label", is("variable"))
               .body("object[0].object_key_values[2].key", is("var1"))
               .body("object[0].object_key_values[2].value", is("var1Value"))
               .body("object[0].object_key_values[3].label", is("variable"))
               .body("object[0].object_key_values[3].key", is("var2"))
               .body("object[0].object_key_values[3].value", is("var2Value"))
               //check generic_information label
               .body("object[0].object_key_values[4].label", is("generic_information"))
               .body("object[0].object_key_values[4].key", is("genericInfo1"))
               .body("object[0].object_key_values[4].value", is("genericInfo1Value"))
               .body("object[0].object_key_values[5].label", is("generic_information"))
               .body("object[0].object_key_values[5].key", is("genericInfo2"))
               .body("object[0].object_key_values[5].value", is("genericInfo2Value"))
               .body("object[0].content_type", is(contentType));
    }

    @Test
    public void testCreateWorkflowShouldReturnUnsupportedMediaTypeWithoutBody() {
        given().pathParam("bucketId", bucket.getMetaDataId())
               .when()
               .post(CATALOG_OBJECTS_RESOURCE)
               .then()
               .assertThat()
               .statusCode(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    public void testCreateWorkflowShouldReturnNotFoundIfNonExistingBucketId() {
        given().pathParam("bucketId", 42)
               .queryParam("kind", "workflow")
               .queryParam("name", "workflow_test")
               .queryParam("commitMessage", "first commit")
               .queryParam("contentType", contentType)
               .multiPart(IntegrationTestUtil.getWorkflowFile("workflow.xml"))
               .when()
               .post(CATALOG_OBJECTS_RESOURCE)
               .then()
               .assertThat()
               .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testGetWorkflowShouldReturnLatestSavedWorkflowRevision() throws IOException {

        given().pathParam("bucketId", bucket.getMetaDataId())
               .pathParam("name", "workflowname")
               .queryParam("commitMessage", "commit message2")
               .multiPart(IntegrationTestUtil.getWorkflowFile("workflow.xml"))
               .when()
               .post(CATALOG_OBJECT_REVISIONS_RESOURCE)
               .then()
               .statusCode(HttpStatus.SC_CREATED);

        HashMap<String, Object> thirdWFRevision = given().pathParam("bucketId", bucket.getMetaDataId())
                                                         .pathParam("name", "workflowname")
                                                         .queryParam("commitMessage", "commit message3")
                                                         .multiPart(IntegrationTestUtil.getWorkflowFile("workflow.xml"))
                                                         .when()
                                                         .post(CATALOG_OBJECT_REVISIONS_RESOURCE)
                                                         .then()
                                                         .statusCode(HttpStatus.SC_CREATED)
                                                         .extract()
                                                         .path("");

        ValidatableResponse response = given().pathParam("bucketId", bucket.getMetaDataId())
                                              .pathParam("name", "workflowname")
                                              .when()
                                              .get(CATALOG_OBJECT_RESOURCE)
                                              .then()
                                              .assertThat()
                                              .statusCode(HttpStatus.SC_OK);

        String responseString = response.extract().response().asString();

        response.body("bucket_id", is(thirdWFRevision.get("bucket_id")))
                .body("name", is(thirdWFRevision.get("name")))
                .body("commit_time", is(thirdWFRevision.get("commit_time")))
                .body("object_key_values", hasSize(6))
                //check generic_information label
                .body("object_key_values[0].label", is("generic_information"))
                .body("object_key_values[0].key", is("genericInfo1"))
                .body("object_key_values[0].value", is("genericInfo1Value"))
                .body("object_key_values[1].label", is("generic_information"))
                .body("object_key_values[1].key", is("genericInfo2"))
                .body("object_key_values[1].value", is("genericInfo2Value"))
                //check job info
                .body("object_key_values[2].label", is("job_information"))
                .body("object_key_values[2].key", is("name"))
                .body("object_key_values[2].value", is("Valid Workflow"))
                .body("object_key_values[3].label", is("job_information"))
                .body("object_key_values[3].key", is("project_name"))
                .body("object_key_values[3].value", is("Project Name"))
                //check variables label
                .body("object_key_values[4].label", is("variable"))
                .body("object_key_values[4].key", is("var1"))
                .body("object_key_values[4].value", is("var1Value"))
                .body("object_key_values[5].label", is("variable"))
                .body("object_key_values[5].key", is("var2"))
                .body("object_key_values[5].value", is("var2Value"))
                .body("content_type", is(contentType));
    }

    @Test
    public void testGetRawWorkflowShouldReturnSavedRawObject() throws IOException {
        Response response = given().pathParam("bucketId", bucket.getMetaDataId())
                                   .pathParam("name", "workflowname")
                                   .when()
                                   .get(CATALOG_OBJECT_RESOURCE + "/raw");

        Arrays.equals(ByteStreams.toByteArray(response.asInputStream()),
                      IntegrationTestUtil.getWorkflowAsByteArray("workflow.xml"));

        response.then().assertThat().statusCode(HttpStatus.SC_OK).contentType(contentType);
    }

    @Test
    public void testGetWorkflowShouldReturnNotFoundIfNonExistingBucketId() {
        given().pathParam("bucketId", 42)
               .pathParam("name", "23")
               .when()
               .get(CATALOG_OBJECT_RESOURCE)
               .then()
               .assertThat()
               .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testGetWorkflowPayloadShouldReturnNotFoundIfNonExistingBucketId() {
        given().pathParam("bucketId", 42)
               .pathParam("name", "42")
               .when()
               .get(CATALOG_OBJECT_RESOURCE + "/raw")
               .then()
               .assertThat()
               .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testGetWorkflowShouldReturnNotFoundIfNonExistingObjectId() {
        given().pathParam("bucketId", 1)
               .pathParam("name", "42")
               .when()
               .get(CATALOG_OBJECT_RESOURCE)
               .then()
               .assertThat()
               .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testGetWorkflowPayloadShouldReturnNotFoundIfNonExistingObjectId() {
        given().pathParam("bucketId", 1)
               .pathParam("name", "42")
               .when()
               .get(CATALOG_OBJECT_RESOURCE + "/raw")
               .then()
               .assertThat()
               .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testListWorkflowsShouldReturnSavedWorkflows() {
        given().pathParam("bucketId", bucket.getMetaDataId())
               .when()
               .get(CATALOG_OBJECTS_RESOURCE)
               .then()
               .assertThat()
               .statusCode(HttpStatus.SC_OK);
    }

    @Test
    public void testListWorkflowsShouldReturnNotFoundIfNonExistingBucketId() {
        List<?> response = given().pathParam("bucketId", 42)
                                  .when()
                                  .get(CATALOG_OBJECTS_RESOURCE)
                                  .then()
                                  .assertThat()
                                  .statusCode(HttpStatus.SC_OK)
                                  .extract()
                                  .path("");
        assertThat(response).isEmpty();
    }

    @Test
    public void testDeleteExistingObject() {
        given().pathParam("bucketId", bucket.getMetaDataId())
               .pathParam("name", "workflowname")
               .when()
               .delete(CATALOG_OBJECT_RESOURCE)
               .then()
               .assertThat()
               .statusCode(HttpStatus.SC_OK);

        // check that the object is really gone
        given().pathParam("bucketId", bucket.getMetaDataId())
               .pathParam("name", "workflowname")
               .when()
               .get(CATALOG_OBJECT_RESOURCE)
               .then()
               .assertThat()
               .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testDeleteNonExistingWorkflow() {
        given().pathParam("bucketId", bucket.getMetaDataId())
               .pathParam("name", "42")
               .when()
               .delete(CATALOG_OBJECT_RESOURCE)
               .then()
               .assertThat()
               .statusCode(HttpStatus.SC_NOT_FOUND);
    }

}