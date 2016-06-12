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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Value;
import org.jpmml.converter.BinaryFeature;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Schema;

public class FeatureMap {

	private List<Feature> features = new ArrayList<>();

	private Map<FieldName, DataField> dataFields = new LinkedHashMap<>();


	public FeatureMap(){
	}

	public void load(InputStream is) throws IOException {

		try(Reader reader = new InputStreamReader(is, "UTF-8")){
			load(new BufferedReader(reader));
		}
	}

	public void load(BufferedReader reader) throws IOException {

		for(int i = 0; true; i++){
			String line = reader.readLine();
			if(line == null){
				break;
			}

			StringTokenizer st = new StringTokenizer(line, "\t");

			if(st.countTokens() != 3){
				throw new IllegalArgumentException(line);
			}

			load(st.nextToken(), st.nextToken(), st.nextToken());
		}
	}

	public void load(String id, String name, String type){

		if(Integer.parseInt(id) != this.features.size()){
			throw new IllegalArgumentException(id);
		}

		String value = null;

		if(("i").equals(type)){
			int equals = name.indexOf('=');
			if(equals < 0){
				throw new IllegalArgumentException(name);
			}

			value = name.substring(equals + 1);
			name = name.substring(0, equals);
		}

		load(FieldName.create(name), value, type);
	}

	private void load(FieldName name, String value, String type){
		DataField dataField = this.dataFields.get(name);

		if(dataField == null){
			dataField = createDataField(name, type);

			this.dataFields.put(name, dataField);
		}

		if(value != null){
			dataField.addValues(new Value(value));
		}

		Feature feature;

		OpType opType = dataField.getOpType();
		switch(opType){
			case CATEGORICAL:
				feature = new BinaryFeature(dataField, value);
				break;
			case CONTINUOUS:
				feature = new ContinuousFeature(dataField);
				break;
			default:
				throw new IllegalArgumentException(type);
		}

		this.features.add(feature);
	}

	public Schema createSchema(FieldName targetField, List<String> targetCategories){
		List<FieldName> activeFields = new ArrayList<>(this.dataFields.keySet());

		Schema schema = new Schema(targetField, targetCategories, activeFields, this.features);

		return schema;
	}

	public List<DataField> getDataFields(){
		List<DataField> dataFields = new ArrayList<>(this.dataFields.values());

		return dataFields;
	}

	static
	private DataField createDataField(FieldName name, String type){

		switch(type){
			case "i":
				return new DataField(name, OpType.CATEGORICAL, DataType.STRING);
			case "q":
				return new DataField(name, OpType.CONTINUOUS, DataType.FLOAT);
			case "int":
				return new DataField(name, OpType.CONTINUOUS, DataType.INTEGER);
			default:
				throw new IllegalArgumentException(type);
		}
	}
}