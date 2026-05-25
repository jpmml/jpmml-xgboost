/*
 * Copyright (c) 2026 Villu Ruusmann
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

import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.jpmml.evaluator.ResultField;

abstract
public class LegacyXGBoostEncoderBatch extends XGBoostEncoderBatch {

	public LegacyXGBoostEncoderBatch(String algorithm, String dataset, Predicate<ResultField> columnFilter, Equivalence<Object> equivalence){
		super(algorithm, dataset, columnFilter, equivalence);

		setFormats(new String[]{XGBoostFormats.BINARY});
	}

	@Override
	public String getLearnerPath(String format){
		return (super.getLearnerPath(format)).replace("/xgboost/", "/xgboost/legacy/");
	}

	@Override
	public String getInputCsvPath(){
		return super.getInputCsvPath();
	}

	@Override
	public String getOutputCsvPath(){
		return (super.getOutputCsvPath()).replace("/csv/", "/csv/legacy/");
	}
}