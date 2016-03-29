package org.jpmml.xgboost;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.jpmml.evaluator.ArchiveBatch;
import org.jpmml.evaluator.Batch;
import org.jpmml.evaluator.IntegrationTest;
import org.jpmml.evaluator.visitors.InvalidFeatureInspector;
import org.jpmml.evaluator.visitors.UnsupportedFeatureInspector;

public class XGBoostTest extends IntegrationTest {

	@Override
	protected ArchiveBatch createBatch(String name, String dataset){
		ArchiveBatch result = new ArchiveBatch(name, dataset){

			@Override
			public InputStream open(String path){
				Class<? extends XGBoostTest> clazz = XGBoostTest.this.getClass();

				return clazz.getResourceAsStream(path);
			}

			@Override
			public PMML getPMML() throws Exception {
				Learner learner;

				try(InputStream is = open("/xgboost/" + getName() + getDataset() + ".model")){
					learner = XGBoostUtil.loadLearner(is);
				}

				FeatureMap featureMap;

				try(InputStream is = open("/csv/" + getDataset() + ".fmap")){
					featureMap = XGBoostUtil.loadFeatureMap(is);
				}

				PMML pmml = learner.encodePMML(null, null, featureMap);

				ensureValidity(pmml);

				return pmml;
			}
		};

		return result;
	}

	@Override
	public void evaluate(Batch batch, Set<FieldName> ignoredFields) throws Exception {
		evaluate(batch, ignoredFields, 1e-6, 1e-6);
	}

	static
	private void ensureValidity(PMML pmml){
		List<Visitor> visitors = Arrays.<Visitor>asList(
			new UnsupportedFeatureInspector(),
			new InvalidFeatureInspector()
		);

		for(Visitor visitor : visitors){
			visitor.applyTo(pmml);
		}
	}
}