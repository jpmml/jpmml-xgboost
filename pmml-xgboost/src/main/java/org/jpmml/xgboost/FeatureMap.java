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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Value;
import org.jpmml.converter.BinaryFeature;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.PMMLEncoder;
import org.jpmml.converter.PMMLUtil;

public class FeatureMap {

	private List<Entry> entries = new ArrayList<>();

	private Map<Value.Property, List<String>> valueMap = new EnumMap<>(Value.Property.class);


	public FeatureMap(){
	}

	public List<Feature> encodeFeatures(PMMLEncoder encoder){
		List<Feature> result = new ArrayList<>();

		Set<DataField> dataFields = new LinkedHashSet<>();

		List<Entry> entries = getEntries();
		for(Entry entry : entries){
			Feature feature = entry.encodeFeature(encoder);

			result.add(feature);

			DataField dataField = (DataField)feature.getField();

			dataFields.add(dataField);
		}

		Collection<Map.Entry<Value.Property, List<String>>> valueEntries = this.valueMap.entrySet();

		for(DataField dataField : dataFields){

			for(Map.Entry<Value.Property, List<String>> valueEntry : valueEntries){
				PMMLUtil.addValues(dataField, valueEntry.getKey(), valueEntry.getValue());
			}
		}

		return result;
	}

	public void addEntry(String name, String type){
		addEntry(name, Entry.Type.fromString(type));

	}

	public void addEntry(String name, Entry.Type type){
		Entry entry;

		if(type == Entry.Type.BINARY_INDICATOR){
			String value = null;

			int equals = name.indexOf('=');
			if(equals > -1){
				value = name.substring(equals + 1);
				name = name.substring(0, equals);
			}

			entry = new CategoricalEntry(name, value, type);
		} else

		{
			entry = new ContinuousEntry(name, type);
		}

		addEntry(entry);
	}

	public void addEntry(Entry entry){
		List<Entry> entries = getEntries();

		entries.add(entry);
	}

	public List<Entry> getEntries(){
		return this.entries;
	}

	public void addValidValue(String value){
		addValue(Value.Property.VALID, value);
	}

	public void addInvalidValue(String value){
		addValue(Value.Property.INVALID, value);
	}

	public void addMissingValue(String value){
		addValue(Value.Property.MISSING, value);
	}

	private void addValue(Value.Property property, String value){

		if(value == null){
			return;
		}

		List<String> values = this.valueMap.get(property);
		if(values == null){
			values = new ArrayList<>();

			this.valueMap.put(property, values);
		}

		values.add(value);
	}

	abstract
	static
	public class Entry {

		private String name = null;

		private Type type = null;


		public Entry(String name, Type type){
			setName(name);
			setType(type);
		}

		abstract
		public Feature encodeFeature(PMMLEncoder encoder);

		public String getName(){
			return this.name;
		}

		private void setName(String name){
			this.name = Objects.requireNonNull(name);
		}

		public Type getType(){
			return this.type;
		}

		private void setType(Type type){
			this.type = Objects.requireNonNull(type);
		}

		static
		public enum Type {
			BINARY_INDICATOR,
			FLOAT,
			INTEGER,
			;

			static
			public Type fromString(String string){

				switch(string){
					case "i":
						return Type.BINARY_INDICATOR;
					case "q":
					case "float":
						return Type.FLOAT;
					case "int":
						return Type.INTEGER;
					default:
						throw new IllegalArgumentException(string);
				}
			}
		}
	}

	static
	private class ContinuousEntry extends Entry {

		public ContinuousEntry(String name, Type type){
			super(name, type);
		}

		@Override
		public Feature encodeFeature(PMMLEncoder encoder){
			String name = getName();
			Type type = getType();

			DataField dataField = encoder.getDataField(name);
			if(dataField == null){

				switch(type){
					case FLOAT:
						dataField = encoder.createDataField(name, OpType.CONTINUOUS, DataType.FLOAT);
						break;
					case INTEGER:
						dataField = encoder.createDataField(name, OpType.CONTINUOUS, DataType.INTEGER);
						break;
					default:
						throw new IllegalArgumentException();
				}
			}

			return new ContinuousFeature(encoder, dataField);
		}
	}

	static
	private class CategoricalEntry extends Entry {

		private String value = null;


		public CategoricalEntry(String name, String value, Type type){
			super(name, type);

			setValue(value);
		}

		@Override
		public Feature encodeFeature(PMMLEncoder encoder){
			String name = getName();
			String value = getValue();
			Type type = getType();

			DataField dataField = encoder.getDataField(name);
			if(dataField == null){

				switch(type){
					case BINARY_INDICATOR:
						if(value != null){
							dataField = encoder.createDataField(name, OpType.CATEGORICAL, DataType.STRING);
						} else

						{
							dataField = encoder.createDataField(name, OpType.CATEGORICAL, DataType.BOOLEAN);
						}
						break;
					default:
						throw new IllegalArgumentException();
				}
			} // End if

			if(value != null){
				PMMLUtil.addValues(dataField, Collections.singletonList(value));

				return new BinaryFeature(encoder, dataField, value);
			} else

			{
				return new BinaryFeature(encoder, dataField, Boolean.TRUE);
			}
		}

		public String getValue(){
			return this.value;
		}

		private void setValue(String value){
			this.value = value;
		}
	}
}