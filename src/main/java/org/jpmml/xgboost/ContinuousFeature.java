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

import java.math.BigDecimal;
import java.math.MathContext;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.Predicate;
import org.dmg.pmml.SimplePredicate;
import org.jpmml.converter.ValueUtil;

public class ContinuousFeature extends Feature {

	public ContinuousFeature(DataField dataField){
		super(dataField);
	}

	@Override
	public boolean isDefaultLeft(Node node){
		return node.default_left();
	}

	@Override
	public Predicate encodePredicate(int splitCondition, boolean left){
		Number value = encodeValue(splitCondition);

		Predicate simplePredicate = new SimplePredicate()
			.setField(getName())
			.setOperator(left ? SimplePredicate.Operator.LESS_THAN : SimplePredicate.Operator.GREATER_OR_EQUAL)
			.setValue(ValueUtil.formatValue(value));

		return simplePredicate;
	}

	private Number encodeValue(int splitCondition){
		DataField dataField = getDataField();

		float value = Float.intBitsToFloat(splitCondition);

		DataType dataType = dataField.getDataType();
		switch(dataType){
			case INTEGER:
				return encodeIntegerValue(value);
			default:
				return encodeFloatValue(value);
		}
	}

	private Number encodeIntegerValue(float value){
		Integer result = ((int)(value + 1f));

		return result;
	}

	private Number encodeFloatValue(float value){
		BigDecimal result = new BigDecimal(value, MathContext.DECIMAL32);

		return result;
	}
}