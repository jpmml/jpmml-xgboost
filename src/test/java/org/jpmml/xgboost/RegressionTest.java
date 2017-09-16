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

public class RegressionTest extends XGBoostTest {

	@Test
	public void evaluateAuto() throws Exception {
		evaluate("LinearRegression", "Auto", new XGBoostEquivalence(4));
	}

	@Test
	public void evaluateAutoNA() throws Exception {
		evaluate("LinearRegression", "AutoNA", new XGBoostEquivalence(4));
	}

	@Test
	public void evaluateAudit() throws Exception {
		evaluate("LogisticRegression", "Audit");
	}

	@Test
	public void evaluateAuditNA() throws Exception {
		evaluate("LogisticRegression", "AuditNA");
	}

	@Test
	public void evaluateVisit() throws Exception {
		evaluate("PoissonRegression", "Visit", new XGBoostEquivalence(16));
	}

	@Test
	public void evaluateVisitNA() throws Exception {
		evaluate("PoissonRegression", "VisitNA", new XGBoostEquivalence(20));
	}
}