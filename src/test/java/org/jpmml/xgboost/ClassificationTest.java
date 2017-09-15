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

import org.junit.Test;

public class ClassificationTest extends XGBoostTest {

	@Test
	public void evaluateAudit() throws Exception {
		evaluate("BinomialClassification", "Audit");
	}

	@Test
	public void evaluateAuditLimit() throws Exception {
		evaluate("BinomialClassification", "Audit@9");
	}

	@Test
	public void evaluateAuditNA() throws Exception {
		evaluate("BinomialClassification", "AuditNA");
	}

	@Test
	public void evaluateAuditNALimit() throws Exception {
		evaluate("BinomialClassification", "AuditNA@9");
	}

	@Test
	public void evaluateIris() throws Exception {
		evaluate("MultinomialClassification", "Iris", new XGBoostEquivalence(12));
	}

	@Test
	public void evaluateIrisLimit() throws Exception {
		evaluate("MultinomialClassification", "Iris@9", new XGBoostEquivalence(2));
	}

	@Test
	public void evaluateIrisNA() throws Exception {
		evaluate("MultinomialClassification", "IrisNA", new XGBoostEquivalence(12));
	}

	@Test
	public void evaluateIrisNALimit() throws Exception {
		evaluate("MultinomialClassification", "IrisNA@9", new XGBoostEquivalence(4));
	}
}