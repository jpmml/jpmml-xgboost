/*
 * Copyright (c) 2016 Villu Ruusmann
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
package org.jpmml.xgboost;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.dmg.pmml.FieldName;
import org.jpmml.converter.FieldNameUtil;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.testing.ArchiveBatch;
import org.jpmml.evaluator.testing.FloatEquivalence;
import org.junit.Test;

public class ClassificationTest extends XGBoostTest {

	public ClassificationTest(){
		super(new FloatEquivalence(4));
	}

	@Override
	protected ArchiveBatch createBatch(String name, String dataset, Predicate<ResultField> predicate, Equivalence<Object> equivalence){
		ArchiveBatch result = new XGBoostTestBatch(name, dataset, predicate, equivalence){

			@Override
			public ClassificationTest getIntegrationTest(){
				return ClassificationTest.this;
			}

			@Override
			public List<Map<FieldName, String>> getInput() throws IOException {
				String[] dataset = parseDataset();

				List<Map<FieldName, String>> table = super.getInput();

				// XXX
				if(("AuditNA").equals(dataset[0])){
					FieldName income = FieldName.create("Income");

 					for(Map<FieldName, String> row : table){
 						String value = row.get(income);

 						if(value == null){
 							row.put(income, "NaN");
 						}
 					}
				}

				return table;
			}
		};

		return result;
	}

	@Test
	public void evaluateBinomialAudit() throws Exception {
		evaluate("BinomialClassification", "Audit", excludeFields(ClassificationTest.falseProbabilityField), new FloatEquivalence(8));
	}

	@Test
	public void evaluateBinomialAuditLimit() throws Exception {
		evaluate("BinomialClassification", "Audit@31", excludeFields(ClassificationTest.falseProbabilityField), new FloatEquivalence(8));
	}

	@Test
	public void evaluateBinomialAuditNA() throws Exception {
		evaluate("BinomialClassification", "AuditNA", excludeFields(ClassificationTest.falseProbabilityField), new FloatEquivalence(8));
	}

	@Test
	public void evaluateBinomialAuditNALimit() throws Exception {
		evaluate("BinomialClassification", "AuditNA@31", excludeFields(ClassificationTest.falseProbabilityField), new FloatEquivalence(8));
	}

	@Test
	public void evaluateHingeAudit() throws Exception {
		evaluate("HingeClassification", "Audit");
	}

	@Test
	public void evaluateHingeAuditNA() throws Exception {
		evaluate("HingeClassification", "AuditNA");
	}

	@Test
	public void evaluateMultinomialAudit() throws Exception {
		evaluate("MultinomialClassification", "Audit", new FloatEquivalence(8));
	}

	@Test
	public void evaluateMultinomialAuditNA() throws Exception {
		evaluate("MultinomialClassification", "AuditNA", new FloatEquivalence(8));
	}

	@Test
	public void evaluateIris() throws Exception {
		evaluate("MultinomialClassification", "Iris");
	}

	@Test
	public void evaluateIrisLimit() throws Exception {
		evaluate("MultinomialClassification", "Iris@11", new FloatEquivalence(8));
	}

	@Test
	public void evaluateIrisNA() throws Exception {
		evaluate("MultinomialClassification", "IrisNA", new FloatEquivalence(8));
	}

	@Test
	public void evaluateIrisNALimit() throws Exception {
		evaluate("MultinomialClassification", "IrisNA@11");
	}

	private static final FieldName falseProbabilityField = FieldNameUtil.create("probability", "0");
	private static final FieldName trueProbabilityField = FieldNameUtil.create("probability", "1");
}