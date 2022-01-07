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
package org.jpmml.xgboost.testing;

import org.jpmml.converter.testing.Datasets;
import org.jpmml.evaluator.testing.FloatEquivalence;
import org.junit.Test;

public class RegressionTest extends XGBoostTest implements XGBoostAlgorithms, Datasets {

	public RegressionTest(){
		super(new FloatEquivalence(4));
	}

	@Test
	public void evaluateLinearAuto() throws Exception {
		evaluate(LINEAR_REGRESSION, AUTO);
	}

	@Test
	public void evaluateLinearAutoNA() throws Exception {
		evaluate(LINEAR_REGRESSION, AUTO_NA);
	}

	@Test
	public void evaluateLogisticAudit() throws Exception {
		evaluate(LOGISTIC_REGRESSION, AUDIT);
	}

	@Test
	public void evaluateLogisticAuditNA() throws Exception {
		evaluate(LOGISTIC_REGRESSION, AUDIT_NA);
	}

	@Test
	public void evaluateGammaVisit() throws Exception {
		evaluate(GAMMA_REGRESSION, VISIT, new FloatEquivalence(16));
	}

	@Test
	public void evaluateGammaVisitNA() throws Exception {
		evaluate(GAMMA_REGRESSION, VISIT_NA, new FloatEquivalence(20));
	}

	@Test
	public void evaluatePoissonVisit() throws Exception {
		evaluate(POISSON_REGRESSION, VISIT, new FloatEquivalence(12));
	}

	@Test
	public void evaluatePoissonVisitNA() throws Exception {
		evaluate(POISSON_REGRESSION, VISIT_NA, new FloatEquivalence(16));
	}

	@Test
	public void evaluateTweedieVisit() throws Exception {
		evaluate(TWEEDIE_REGRESSION, VISIT, new FloatEquivalence(16));
	}

	@Test
	public void evaluateTweedieVisitNA() throws Exception {
		evaluate(TWEEDIE_REGRESSION, VISIT_NA, new FloatEquivalence(20));
	}
}