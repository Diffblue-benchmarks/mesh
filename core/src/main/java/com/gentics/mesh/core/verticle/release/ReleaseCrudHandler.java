package com.gentics.mesh.core.verticle.release;

import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.stereotype.Component;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.core.rest.schema.SchemaReferenceList;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;

import io.vertx.core.eventbus.DeliveryOptions;
import rx.Observable;

/**
 * CRUD Handler for Releases
 */
@Component
public class ReleaseCrudHandler extends AbstractCrudHandler<Release, ReleaseResponse> {

	@Override
	public RootVertex<Release> getRootVertex(InternalActionContext ac) {
		return ac.getProject().getReleaseRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac, String uuid) {
		throw new NotImplementedException("Release can't be deleted");
	}

	/**
	 * Handle getting the schema versions of a release.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of release to be queried
	 */
	public void handleGetSchemaVersions(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		db.asyncNoTrxExperimental(() -> {
			return getRootVertex(ac).loadObjectByUuid(ac, uuid, GraphPermission.READ_PERM).flatMap((release) -> getSchemaVersions(release));
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	/**
	 * Handle assignment of schema version to a release.
	 * 
	 * @param ac
	 * @param uuid
	 *            Uuid of release
	 */
	public void handleAssignSchemaVersion(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		db.asyncNoTrxExperimental(() -> {
			RootVertex<Release> root = getRootVertex(ac);
			return root.loadObjectByUuid(ac, uuid, UPDATE_PERM).flatMap(release -> {
				SchemaReferenceList schemaReferenceList = ac.fromJson(SchemaReferenceList.class);
				SchemaContainerRoot schemaContainerRoot = ac.getProject().getSchemaContainerRoot();

				return db.trx(() -> {
					Observable<SchemaContainerVersion> obs = Observable.from(schemaReferenceList)
							.flatMap(reference -> schemaContainerRoot.fromReference(reference));
					obs.toBlocking().forEach(version -> {
						SchemaContainerVersion assignedVersion = release.getVersion(version.getSchemaContainer());
						if (assignedVersion != null && assignedVersion.getVersion() > version.getVersion()) {
							throw error(BAD_REQUEST, "error_release_downgrade_schema_version", version.getName(),
									Integer.toString(assignedVersion.getVersion()), Integer.toString(version.getVersion()));
						}
						release.assignSchemaVersion(version);

						DeliveryOptions options = new DeliveryOptions();
						options.addHeader(NodeMigrationVerticle.PROJECT_UUID_HEADER,
								release.getRoot().getProject().getUuid());
						options.addHeader(NodeMigrationVerticle.RELEASE_UUID_HEADER, release.getUuid());
						options.addHeader(NodeMigrationVerticle.UUID_HEADER, version.getSchemaContainer().getUuid());
						options.addHeader(NodeMigrationVerticle.FROM_VERSION_UUID_HEADER, assignedVersion.getUuid());
						options.addHeader(NodeMigrationVerticle.TO_VERSION_UUID_HEADER, version.getUuid());
						Mesh.vertx().eventBus().send(NodeMigrationVerticle.SCHEMA_MIGRATION_ADDRESS, null, options);
					});
					return getSchemaVersions(release);
				});
			});
		}).subscribe(model -> ac.respond(model, OK), ac::fail);

	}

	/**
	 * Get the rest model of the schema versions of the release
	 * 
	 * @param release
	 *            release
	 * @return observable emitting the rest model
	 */
	protected Observable<SchemaReferenceList> getSchemaVersions(Release release) {
		try {
			return Observable.from(release.findAllSchemaVersions()).map(SchemaContainerVersion::transformToReference).collect(() -> {
				return new SchemaReferenceList();
			} , (x, y) -> {
				x.add(y);
			});
		} catch (Exception e) {
			throw error(INTERNAL_SERVER_ERROR, "Unknown error while getting schema versions", e);
		}
	}
}