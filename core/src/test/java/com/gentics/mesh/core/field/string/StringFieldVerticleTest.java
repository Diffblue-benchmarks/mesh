package com.gentics.mesh.core.field.string;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.field.AbstractFieldVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.graphdb.NoTx;

import io.netty.handler.codec.http.HttpResponseStatus;

public class StringFieldVerticleTest extends AbstractFieldVerticleTest {

	private static final String FIELD_NAME = "stringField";

	/**
	 * Update the schema and add a string field.
	 * 
	 * @throws IOException
	 */
	@Before
	public void updateSchema() throws IOException {
		try (NoTx noTx = db.noTx()) {
			Schema schema = schemaContainer("folder").getLatestVersion().getSchema();

			// add non restricted string field
			StringFieldSchema stringFieldSchema = new StringFieldSchemaImpl();
			stringFieldSchema.setName(FIELD_NAME);
			stringFieldSchema.setLabel("Some label");
			schema.addField(stringFieldSchema);

			// add restricted string field
			StringFieldSchema restrictedStringFieldSchema = new StringFieldSchemaImpl();
			restrictedStringFieldSchema.setName("restrictedstringField");
			restrictedStringFieldSchema.setLabel("Some label");
			restrictedStringFieldSchema.setAllowedValues(new String[] { "one", "two", "three" });
			schema.addField(restrictedStringFieldSchema);

			schemaContainer("folder").getLatestVersion().setSchema(schema);
		}
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		try (NoTx noTx = db.noTx()) {
			NodeResponse response = createNode(null, (Field) null);
			StringFieldImpl stringField = response.getFields().getStringField(FIELD_NAME);
			assertNull(stringField);
		}
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			for (int i = 0; i < 20; i++) {
				NodeGraphFieldContainer container = node.getGraphFieldContainer("en");
				String oldValue = getStringValue(container, FIELD_NAME);

				String newValue = "content " + i;

				NodeResponse response = updateNode(FIELD_NAME, new StringFieldImpl().setString(newValue));
				StringFieldImpl field = response.getFields().getStringField(FIELD_NAME);
				assertEquals(newValue, field.getString());
				node.reload();
				container.reload();

				assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion().getNumber());
				assertEquals("Check old value", oldValue, getStringValue(container, FIELD_NAME));
			}
		}
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		try (NoTx noTx = db.noTx()) {
			NodeResponse firstResponse = updateNode(FIELD_NAME, new StringFieldImpl().setString("bla"));
			String oldNumber = firstResponse.getVersion().getNumber();

			NodeResponse secondResponse = updateNode(FIELD_NAME, new StringFieldImpl().setString("bla"));
			assertThat(secondResponse.getVersion().getNumber()).as("New version number").isEqualTo(oldNumber);
		}
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		try (NoTx noTx = db.noTx()) {
			NodeResponse firstResponse = updateNode(FIELD_NAME, new StringFieldImpl().setString("bla"));
			String oldVersion = firstResponse.getVersion().getNumber();

			NodeResponse secondResponse = updateNode(FIELD_NAME, null);
			assertThat(secondResponse.getFields().getStringField(FIELD_NAME)).as("Updated Field").isNull();
			assertThat(secondResponse.getVersion().getNumber()).as("New version number").isNotEqualTo(oldVersion);

			// Assert that the old version was not modified
			Node node = folder("2015");
			NodeGraphFieldContainer latest = node.getLatestDraftFieldContainer(english());
			assertThat(latest.getVersion().toString()).isEqualTo(secondResponse.getVersion().getNumber());
			assertThat(latest.getString(FIELD_NAME)).isNull();
			assertThat(latest.getPreviousVersion().getString(FIELD_NAME)).isNotNull();
			String oldValue = latest.getPreviousVersion().getString(FIELD_NAME).getString();
			assertThat(oldValue).isEqualTo("bla");

			NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
			assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion().getNumber(),
					secondResponse.getVersion().getNumber());
		}
	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		try (NoTx noTx = db.noTx()) {
			NodeResponse firstResponse = updateNode(FIELD_NAME, new StringFieldImpl().setString("bla"));
			String oldVersion = firstResponse.getVersion().getNumber();

			StringField emptyField = new StringFieldImpl();
			emptyField.setString("");
			NodeResponse secondResponse = updateNode(FIELD_NAME, emptyField);
			assertThat(secondResponse.getFields().getStringField(FIELD_NAME)).as("Updated Field").isNotNull();
			assertThat(secondResponse.getFields().getStringField(FIELD_NAME).getString()).as("Updated Field Value").isEqualTo("");
			assertThat(secondResponse.getVersion().getNumber()).as("New version number").isNotEqualTo(oldVersion);

			NodeResponse thirdResponse = updateNode(FIELD_NAME, emptyField);
			assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion().getNumber(),
					secondResponse.getVersion().getNumber());
		}
	}

	/**
	 * Get the string value
	 * 
	 * @param container
	 *            container
	 * @param fieldName
	 *            field name
	 * @return string value (may be null)
	 */
	protected String getStringValue(NodeGraphFieldContainer container, String fieldName) {
		StringGraphField field = container.getString(fieldName);
		return field != null ? field.getString() : null;
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		try (NoTx noTx = db.noTx()) {
			NodeResponse response = createNode(FIELD_NAME, new StringFieldImpl().setString("someString"));
			StringFieldImpl field = response.getFields().getStringField(FIELD_NAME);
			assertEquals("someString", field.getString());
		}
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
			StringGraphField stringField = container.createString(FIELD_NAME);
			stringField.setString("someString");
			NodeResponse response = readNode(node);
			StringFieldImpl deserializedStringField = response.getFields().getStringField(FIELD_NAME);
			assertNotNull(deserializedStringField);
			assertEquals("someString", deserializedStringField.getString());
		}
	}

	@Test
	public void testValueRestrictionValidValue() {
		try (NoTx noTx = db.noTx()) {
			NodeResponse response = updateNode("restrictedstringField", new StringFieldImpl().setString("two"));
			StringFieldImpl field = response.getFields().getStringField("restrictedstringField");
			assertEquals("two", field.getString());
		}
	}

	@Test
	public void testValueRestrictionInvalidValue() {
		try (NoTx noTx = db.noTx()) {
			updateNodeFailure("restrictedstringField", new StringFieldImpl().setString("invalid"), HttpResponseStatus.BAD_REQUEST,
					"node_error_invalid_string_field_value", "restrictedstringField", "invalid");
		}
	}
}
