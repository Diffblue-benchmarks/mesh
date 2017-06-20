package com.gentics.mesh.core.user;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.ferma.Tx;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.auth.MeshAuthHandler;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class AnonymousAccessEndpointTest extends AbstractMeshTest {

	@Test
	public void testAnonymousAccess() {
		client().logout().toCompletable().await();
		UserResponse response = call(() -> client().me());
		assertEquals(MeshAuthHandler.ANONYMOUS_USERNAME, response.getUsername());

		MeshResponse<UserResponse> rawResponse = client().me().invoke();
		latchFor(rawResponse);
		assertThat(rawResponse.getRawResponse().cookies()).as("Anonymous access should not set any cookie").isEmpty();

		String uuid = db().tx(() -> content().getUuid());
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid), FORBIDDEN, "error_missing_perm", uuid);

		try (Tx tx = tx()) {
			anonymousRole().grantPermissions(content(), READ_PERM);
			tx.success();
		}
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));

		// Test toggling the anonymous option
		Mesh.mesh().getOptions().getAuthenticationOptions().setEnableAnonymousAccess(false);
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid), UNAUTHORIZED, "error_not_authorized");
		Mesh.mesh().getOptions().getAuthenticationOptions().setEnableAnonymousAccess(true);
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));

		// Verify that anonymous access does not work if the anonymous user is deleted
		try (Tx tx = tx()) {
			users().get(MeshAuthHandler.ANONYMOUS_USERNAME).remove();
			tx.success();
		}
		call(() -> client().findNodeByUuid(PROJECT_NAME, uuid), UNAUTHORIZED, "error_not_authorized");
	}

}