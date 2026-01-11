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
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.OpType;
import org.dmg.pmml.Value;
import org.jpmml.converter.BinaryFeature;
import org.jpmml.converter.CategoricalFeature;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.ExceptionUtil;
import org.jpmml.converter.Feature;
import org.jpmml.converter.FieldUtil;
import org.jpmml.converter.PMMLEncoder;

public class FeatureMap {

	private List<Entry> entries = new ArrayList<>();

	private Map<Value.Property, List<String>> valueMap = new EnumMap<>(Value.Property.class);


	public FeatureMap(){
	}

	public List<Feature> encodeFeatures(Learner learner, PMMLEncoder encoder){
		List<Entry> entries = getEntries();

		List<Feature> result = new ArrayList<>();

		Set<DataField> dataFields = new LinkedHashSet<>();

		for(int i = 0; i < entries.size(); i++){
			Entry entry = entries.get(i);

			Feature feature = entry.encodeFeature(learner, i, encoder);

			result.add(feature);

			DataField dataField = (DataField)feature.getField();

			dataFields.add(dataField);
		}

		Collection<Map.Entry<Value.Property, List<String>>> valueEntries = this.valueMap.entrySet();

		for(DataField dataField : dataFields){

			for(Map.Entry<Value.Property, List<String>> valueEntry : valueEntries){
				FieldUtil.addValues(dataField, valueEntry.getKey(), valueEntry.getValue());
			}
		}

		return result;
	}

	public boolean isEmpty(){
		List<Entry> entries = getEntries();

		return entries.isEmpty();
	}

	public void update(FeatureMap featureMap){
		List<Entry> entries = getEntries();

		for(Entry entry : entries){
			List<Entry> updateEntries = featureMap.getEntries(entry.getName());

			if(updateEntries.isEmpty()){
				throw new XGBoostException("Feature " + ExceptionUtil.formatName(entry.getName()) + " has no feature map entries");
			} // End if

			if(entry instanceof CategoricalEntry){
				CategoricalEntry categoricalEntry = (CategoricalEntry)entry;

				List<?> values = updateEntries.stream()
					.map(IndicatorEntry.class::cast)
					.map(IndicatorEntry::getValue)
					.collect(Collectors.toList());

				categoricalEntry.setValues(values);
			}
		}
	}

	public void addEntry(String name, String type){
		Entry entry = createEntry(name, Entry.Type.fromString(type));

		addEntry(entry);
	}

	public void addEntry(Entry entry){
		List<Entry> entries = getEntries();

		entries.add(entry);
	}

	public List<Entry> getEntries(String name){
		List<Entry> result = new ArrayList<>();

		List<Entry> entries = getEntries();
		for(Entry entry : entries){

			if(Objects.equals(name, entry.getName())){
				result.add(entry);
			}
		}

		return result;
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

	static
	private Entry createEntry(String name, Entry.Type type){

		switch(type){
			case INDICATOR:
				String value = null;

				int equals = name.indexOf('=');
				if(equals > -1){
					value = name.substring(equals + 1);
					name = name.substring(0, equals);
				}

				return new IndicatorEntry(name, value, type);
			case QUANTITATIVE:
			case INTEGER:
			case FLOAT:
				return new ContinuousEntry(name, type);
			case CATEGORICAL:
				return new CategoricalEntry(name, type);
			default:
				throw new IllegalArgumentException();
		}
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
		public Feature encodeFeature(Learner learner, int index, PMMLEncoder encoder);

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
			INDICATOR,
			QUANTITATIVE,
			INTEGER,
			FLOAT,
			CATEGORICAL,
			;

			static
			public Type fromString(String string){

				switch(string){
					case "i":
						return Type.INDICATOR;
					case "q":
						return Type.QUANTITATIVE;
					case "int":
						return Type.INTEGER;
					case "float":
						return Type.FLOAT;
					case "c":
					case "categorical":
						return Type.CATEGORICAL;
					default:
						throw new XGBoostException("Entry type " + ExceptionUtil.formatParameter(string) + " is not supported");
				}
			}
		}
	}

	static
	public class IndicatorEntry extends Entry {

		private String value = null;


		public IndicatorEntry(String name, String value, Type type){
			super(name, type);

			setValue(value);
		}

		@Override
		public Feature encodeFeature(Learner learner, int index, PMMLEncoder encoder){
			String name = getName();
			String value = getValue();
			Type type = getType();

			DataField dataField = encoder.getDataField(name);
			if(dataField == null){

				switch(type){
					case INDICATOR:
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
				FieldUtil.addValues(dataField, Collections.singletonList(value));

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

	static
	public class ContinuousEntry extends Entry {

		public ContinuousEntry(String name, Type type){
			super(name, type);
		}

		@Override
		public Feature encodeFeature(Learner learner, int index, PMMLEncoder encoder){
			String name = getName();
			Type type = getType();

			DataField dataField = encoder.getDataField(name);
			if(dataField == null){

				switch(type){
					case QUANTITATIVE:
						dataField = encoder.createDataField(name, OpType.CONTINUOUS, DataType.FLOAT);
						break;
					case INTEGER:
						dataField = encoder.createDataField(name, OpType.CONTINUOUS, DataType.INTEGER);
						break;
					case FLOAT:
						dataField = encoder.createDataField(name, OpType.CONTINUOUS, DataType.FLOAT);
						break;
					default:
						throw new IllegalArgumentException();
				}
			}

			return new ContinuousFeature(encoder, dataField);
		}
	}

	static
	public class CategoricalEntry extends Entry {

		private List<?> values = null;


		public CategoricalEntry(String name, Type type){
			super(name, type);
		}

		@Override
		public Feature encodeFeature(Learner learner, int index, PMMLEncoder encoder){
			String name = getName();
			Type type = getType();
			List<?> values = getValues();

			DataType dataType = DataType.STRING;

			if(values == null){
				GBTree gbtree = learner.gbtree();

				BitSet splitIndices = gbtree.getSplitCategories(index);

				// Used feature
				if(splitIndices != null){
					// Binary categorical features only exhibit one "effective" category level
					int size = Math.max(splitIndices.length(), 2);

					values = new IntegerRange(size);
				} else

				// Unused feature
				{
					// Constant-like feature
					values = new IntegerRange(1);
				}

				dataType = DataType.INTEGER;
			}

			DataField dataField = encoder.getDataField(name);
			if(dataField == null){

				switch(type){
					case CATEGORICAL:
						dataField = encoder.createDataField(name, OpType.CATEGORICAL, dataType, values);
						break;
					default:
						throw new IllegalArgumentException();
				}
			}

			return new CategoricalFeature(encoder, dataField, values);
		}

		public List<?> getValues(){
			return this.values;
		}

		private void setValues(List<?> values){
			this.values = values;
		}
	}
}