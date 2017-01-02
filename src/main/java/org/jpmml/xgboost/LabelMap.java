/*
 * Copyright (c) 2017 Villu Ruusmann
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

import java.util.Collections;
import java.util.Map;

import org.dmg.pmml.DataField;
import org.dmg.pmml.FieldName;
import org.jpmml.converter.Label;

public class LabelMap {

	private DataField dataField = null;

	private Label label = null;


	public LabelMap(DataField dataField, Label label){
		setDataField(dataField);
		setLabel(label);
	}

	public Map<FieldName, DataField> getDataFields(){
		return Collections.singletonMap(this.dataField.getName(), this.dataField);
	}

	public DataField getDataField(){
		return this.dataField;
	}

	private void setDataField(DataField dataField){
		this.dataField = dataField;
	}

	public Label getLabel(){
		return this.label;
	}

	private void setLabel(Label label){
		this.label = label;
	}
}