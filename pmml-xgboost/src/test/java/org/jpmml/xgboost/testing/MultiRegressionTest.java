/*
 * Copyright (c) 2024 Villu Ruusmann
 *
 * This file is part of JPMML-XGBoost
 *
 * JPMML-XGBoost is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-XGBoost is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-XGBoost.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpmml.xgboost.testing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import com.google.common.collect.Iterables;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.Model;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Output;
import org.dmg.pmml.OutputField;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PMMLFunctions;
import org.dmg.pmml.ResultFeature;
import org.jpmml.converter.ExpressionUtil;
import org.jpmml.converter.FieldNameUtil;
import org.jpmml.converter.ModelUtil;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.Table;
import org.jpmml.evaluator.testing.FloatEquivalence;
import org.junit.jupiter.api.Test;

public class MultiRegressionTest extends XGBoostEncoderBatchTest implements XGBoostAlgorithms, XGBoostDatasets, XGBoostFormats, XGBoostLearningRates {

	public MultiRegressionTest(){
		super(new FloatEquivalence(4));
	}

	@Override
	public XGBoostEncoderBatch createBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		XGBoostEncoderBatch result = new XGBoostEncoderBatch(algorithm, dataset, columnFilter, equivalence){

			{
				String algorithm = getAlgorithm();
				String dataset = getDataset();

				if(Objects.equals(AUDIT, dataset) || Objects.equals(AUDIT_NA, dataset)){

					if(algorithm.startsWith(SCALAR_LEAF)){
						setEta(ETA_AUDIT);
					}
				} else

				if(Objects.equals(AUTO, dataset) || Objects.equals(AUTO_NA, dataset)){

					if(Objects.equals(algorithm, SCALAR_LEAF + MEDIAN_REGRESSION) || Objects.equals(algorithm, SCALAR_LEAF + QUANTILE_REGRESSION)){
						setRecordCount(392);
					} else

					if(Objects.equals(algorithm, SCALAR_LEAF + RANDOM_FOREST)){
						setEta(ETA_AUTO / 37);
						setRecordCount(325);
					} else

					if(algorithm.startsWith(SCALAR_LEAF)){
						setEta(ETA_AUTO);
						setRecordCount(392);
					}
				}
			}

			@Override
			public MultiRegressionTest getArchiveBatchTest(){
				return MultiRegressionTest.this;
			}

			@Override
			public String getFeatureMapPath(){
				String algorithm = getAlgorithm();

				if(algorithm.endsWith(QUANTILE_REGRESSION)){
					return super.getFeatureMapPath();
				}

				return "/csv/Multi" + truncate(getDataset()) + ".fmap";
			}

			@Override
			public Table getInput() throws IOException {
				String algorithm = getAlgorithm();

				return super.getInput();
			}

			@Override
			public Table getOutput() throws IOException {
				Table table = super.getOutput();

				String algorithm = getAlgorithm();

				// XXX
				if(algorithm.endsWith(QUANTILE_REGRESSION)){

					for(int i = 0; i < QUANTILE_REGRESSION_TARGETS.size(); i++){
						table.renameColumn(QUANTILE_REGRESSION_TARGETS.get(i), QUANTILE_REGRESSION_OUTPUTS.get(i));
					}
				}

				return table;
			}

			@Override
			public PMML getPMML() throws Exception {
				PMML pmml = super.getPMML();

				String algorithm = getAlgorithm();

				// XXX
				if(algorithm.endsWith(QUANTILE_REGRESSION)){
					List<Model> models = pmml.getModels();

					Model model = Iterables.getOnlyElement(models);

					Output output = ModelUtil.ensureOutput(model);

					List<String> predictedNames = new ArrayList<>();

					for(String targetField : QUANTILE_REGRESSION_TARGETS){
						OutputField predictedOutputField = ModelUtil.createPredictedField(FieldNameUtil.create("predicted", targetField), OpType.CONTINUOUS, DataType.FLOAT)
							.setTargetField(targetField);

						output.addOutputFields(predictedOutputField);

						predictedNames.add(predictedOutputField.requireName());
					}

					String firstField = predictedNames.get(0);
					String secondField = predictedNames.get(1);
					String thirdField = predictedNames.get(2);

					// The median of three values, as max(min(a, b), min(max(a, b), c))
					Expression median = ExpressionUtil.createApply(PMMLFunctions.MAX,
						ExpressionUtil.createApply(PMMLFunctions.MIN, new FieldRef(firstField), new FieldRef(secondField)),
						ExpressionUtil.createApply(PMMLFunctions.MIN, ExpressionUtil.createApply(PMMLFunctions.MAX, new FieldRef(firstField), new FieldRef(secondField)), new FieldRef(thirdField))
					);

					List<Expression> expressions = Arrays.asList(
						ExpressionUtil.createApply(PMMLFunctions.MIN, new FieldRef(firstField), new FieldRef(secondField), new FieldRef(thirdField)),
						median,
						ExpressionUtil.createApply(PMMLFunctions.MAX, new FieldRef(firstField), new FieldRef(secondField), new FieldRef(thirdField))
					);

					for(int i = 0; i < QUANTILE_REGRESSION_OUTPUTS.size(); i++){
						OutputField quantileOutputField = new OutputField(QUANTILE_REGRESSION_OUTPUTS.get(i), OpType.CONTINUOUS, DataType.FLOAT)
							.setResultFeature(ResultFeature.TRANSFORMED_VALUE)
							.setExpression(expressions.get(i));

						output.addOutputFields(quantileOutputField);
					}
				}

				return pmml;
			}
		};

		return result;
	}

	@Test
	public void evaluateScalarLeafDTAuto() throws Exception {
		evaluate(SCALAR_LEAF + DECISION_TREE, AUTO);
	}

	@Test
	public void evaluateScalarLeafDTAutoNA() throws Exception {
		evaluate(SCALAR_LEAF + DECISION_TREE, AUTO_NA);
	}

	@Test
	public void evaluateScalarLeafGammaAuto() throws Exception {
		evaluate(SCALAR_LEAF + GAMMA_REGRESSION, AUTO, new FloatEquivalence(21));
	}

	@Test
	public void evaluateScalarLeafGammaAutoNA() throws Exception {
		evaluate(SCALAR_LEAF + GAMMA_REGRESSION, AUTO_NA, new FloatEquivalence(21));
	}

	@Test
	public void evaluateScalarLeafLinearAuto() throws Exception {
		evaluate(SCALAR_LEAF + LINEAR_REGRESSION, AUTO, new FloatEquivalence(7));
	}

	@Test
	public void evaluateScalarLeafLinearAutoNA() throws Exception {
		evaluate(SCALAR_LEAF + LINEAR_REGRESSION, AUTO_NA, new FloatEquivalence(7));
	}

	@Test
	public void evaluateScalarLeafLogisticAudit() throws Exception {
		evaluate(SCALAR_LEAF + LOGISTIC_REGRESSION, AUDIT, new FloatEquivalence(39));
	}

	@Test
	public void evaluateScalarLeafLogisticAuditNA() throws Exception {
		evaluate(SCALAR_LEAF + LOGISTIC_REGRESSION, AUDIT_NA, new FloatEquivalence(33));
	}

	@Test
	public void evaluateScalarLeafMedianAuto() throws Exception {
		evaluate(SCALAR_LEAF + MEDIAN_REGRESSION, AUTO, new FloatEquivalence(7));
	}

	@Test
	public void evaluateScalarLeafMedianAutoNA() throws Exception {
		evaluate(SCALAR_LEAF + MEDIAN_REGRESSION, AUTO_NA, new FloatEquivalence(6));
	}

	@Test
	public void evaluateScalarLeafPoissonAuto() throws Exception {
		evaluate(SCALAR_LEAF + POISSON_REGRESSION, AUTO, new FloatEquivalence(27));
	}

	@Test
	public void evaluateScalarLeafPoissonAutoNA() throws Exception {
		evaluate(SCALAR_LEAF + POISSON_REGRESSION, AUTO_NA, new FloatEquivalence(17));
	}

	@Test
	public void evaluateScalarLeafQuantileAuto() throws Exception {
		evaluate(SCALAR_LEAF + QUANTILE_REGRESSION, AUTO, excludeFields(QUANTILE_REGRESSION_EXCLUDED_FIELDS), new FloatEquivalence(5));
	}

	@Test
	public void evaluateScalarLeafQuantileAutoNA() throws Exception {
		evaluate(SCALAR_LEAF + QUANTILE_REGRESSION, AUTO_NA, excludeFields(QUANTILE_REGRESSION_EXCLUDED_FIELDS), new FloatEquivalence(6));
	}

	@Test
	public void evaluateScalarLeafRFAuto() throws Exception {
		evaluate(SCALAR_LEAF + RANDOM_FOREST, AUTO, new FloatEquivalence(12));
	}

	@Test
	public void evaluateScalarLeafRFAutoNA() throws Exception {
		evaluate(SCALAR_LEAF + RANDOM_FOREST, AUTO_NA, new FloatEquivalence(10));
	}

	@Test
	public void evaluateScalarLeafTweedieAuto() throws Exception {
		evaluate(SCALAR_LEAF + TWEEDIE_REGRESSION, AUTO, new FloatEquivalence(19));
	}

	@Test
	public void evaluateScalarLeafTweedieAutoNA() throws Exception {
		evaluate(SCALAR_LEAF + TWEEDIE_REGRESSION, AUTO_NA, new FloatEquivalence(22));
	}

	@Test
	public void evaluateVectorLeafDTAuto() throws Exception {
		evaluate(VECTOR_LEAF + DECISION_TREE, AUTO);
	}

	@Test
	public void evaluateVectorLeafDTAutoNA() throws Exception {
		evaluate(VECTOR_LEAF + DECISION_TREE, AUTO_NA);
	}

	@Test
	public void evaluateVectorLeafGammaAuto() throws Exception {
		evaluate(VECTOR_LEAF + GAMMA_REGRESSION, AUTO, new FloatEquivalence(18));
	}

	@Test
	public void evaluateVectorLeafGammaAutoNA() throws Exception {
		evaluate(VECTOR_LEAF + GAMMA_REGRESSION, AUTO_NA, new FloatEquivalence(20));
	}

	@Test
	public void evaluateVectorLeafLinearAuto() throws Exception {
		evaluate(VECTOR_LEAF + LINEAR_REGRESSION, AUTO, new FloatEquivalence(6));
	}

	@Test
	public void evaluateVectorLeafLinearAutoNA() throws Exception {
		evaluate(VECTOR_LEAF + LINEAR_REGRESSION, AUTO_NA, new FloatEquivalence(8));
	}

	@Test
	public void evaluateVectorLeafLogisticAudit() throws Exception {
		evaluate(VECTOR_LEAF + LOGISTIC_REGRESSION, AUDIT, new FloatEquivalence(42));
	}

	@Test
	public void evaluateVectorLeafLogisticAuditNA() throws Exception {
		evaluate(VECTOR_LEAF + LOGISTIC_REGRESSION, AUDIT_NA, new FloatEquivalence(26));
	}

	@Test
	public void evaluateVectorLeafMedianAuto() throws Exception {
		evaluate(VECTOR_LEAF + MEDIAN_REGRESSION, AUTO, new FloatEquivalence(5));
	}

	@Test
	public void evaluateVectorLeafMedianAutoNA() throws Exception {
		evaluate(VECTOR_LEAF + MEDIAN_REGRESSION, AUTO_NA, new FloatEquivalence(6));
	}

	@Test
	public void evaluateVectorLeafPoissonAuto() throws Exception {
		evaluate(VECTOR_LEAF + POISSON_REGRESSION, AUTO, new FloatEquivalence(16));
	}

	@Test
	public void evaluateVectorLeafPoissonAutoNA() throws Exception {
		evaluate(VECTOR_LEAF + POISSON_REGRESSION, AUTO_NA, new FloatEquivalence(18));
	}

	@Test
	public void evaluateVectorLeafQuantileAuto() throws Exception {
		evaluate(VECTOR_LEAF + QUANTILE_REGRESSION, AUTO, excludeFields(QUANTILE_REGRESSION_EXCLUDED_FIELDS), new FloatEquivalence(6));
	}

	@Test
	public void evaluateVectorLeafQuantileAutoNA() throws Exception {
		evaluate(VECTOR_LEAF + QUANTILE_REGRESSION, AUTO_NA, excludeFields(QUANTILE_REGRESSION_EXCLUDED_FIELDS), new FloatEquivalence(5));
	}

	@Test
	public void evaluateVectorLeafRFAuto() throws Exception {
		evaluate(VECTOR_LEAF + RANDOM_FOREST, AUTO, new FloatEquivalence(10));
	}

	@Test
	public void evaluateVectorLeafRFAutoNA() throws Exception {
		evaluate(VECTOR_LEAF + RANDOM_FOREST, AUTO_NA, new FloatEquivalence(9));
	}

	@Test
	public void evaluateVectorLeafTweedieAuto() throws Exception {
		evaluate(VECTOR_LEAF + TWEEDIE_REGRESSION, AUTO, new FloatEquivalence(17));
	}

	@Test
	public void evaluateVectorLeafTweedieAutoNA() throws Exception {
		evaluate(VECTOR_LEAF + TWEEDIE_REGRESSION, AUTO_NA, new FloatEquivalence(20));
	}

	private static final Table DATASET_EMPTY = new Table(0);

	private static final List<String> QUANTILE_REGRESSION_TARGETS = Arrays.asList("_target1", "_target2", "_target3");
	private static final List<String> QUANTILE_REGRESSION_OUTPUTS = Arrays.asList(FieldNameUtil.create("quantile", "_target", 25), FieldNameUtil.create("quantile", "_target", 50), FieldNameUtil.create("quantile", "_target", 75));

	private static final List<String> QUANTILE_REGRESSION_EXCLUDED_FIELDS = Arrays.asList("_target1", "_target2", "_target3", "predicted(_target1)", "predicted(_target2)", "predicted(_target3)");
}