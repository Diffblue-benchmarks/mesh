package com.gentics.mesh.core.data.fieldhandler.microschema;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.fieldhandler.AbstractComparatorDateTest;
import com.gentics.mesh.core.data.schema.handler.AbstractFieldSchemaContainerComparator;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.test.context.MeshTestSetting;
import static com.gentics.mesh.test.TestSize.FULL;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = false)
public class MicroschemaComparatorDateTest extends AbstractComparatorDateTest<Microschema> {

	@Override
	public AbstractFieldSchemaContainerComparator<Microschema> getComparator() {
		return new MicroschemaComparator();
	}

	@Override
	public Microschema createContainer() {
		return FieldUtil.createMinimalValidMicroschema();
	}

}
