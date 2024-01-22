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

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.devsmart.ubjson.GsonUtil;
import com.devsmart.ubjson.UBObject;
import com.devsmart.ubjson.UBReader;
import com.devsmart.ubjson.UBValue;
import com.google.common.collect.Iterables;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.dmg.pmml.DataField;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Expression;
import org.dmg.pmml.Field;
import org.dmg.pmml.OpType;
import org.dmg.pmml.PMML;
import org.dmg.pmml.PMMLFunctions;
import org.dmg.pmml.Value;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.mining.MiningModel;
import org.jpmml.converter.BinaryFeature;
import org.jpmml.converter.CategoricalFeature;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.FieldNameUtil;
import org.jpmml.converter.Label;
import org.jpmml.converter.MissingValueFeature;
import org.jpmml.converter.MultiLabel;
import org.jpmml.converter.PMMLEncoder;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.converter.Schema;
import org.jpmml.converter.ThresholdFeature;
import org.jpmml.converter.ValueUtil;
import org.jpmml.converter.visitors.TreeModelPruner;
import org.jpmml.xgboost.visitors.TreeModelCompactor;

public class Learner implements BinaryLoadable, JSONLoadable, UBJSONLoadable {

	private float base_score;

	private int num_feature;

	private int num_class;

	private int contain_extra_attrs;

	private int contain_eval_metrics;

	private int major_version;

	private int minor_version;

	private int num_target;

	private int base_score_estimated;

	private ObjFunction obj;

	private GBTree gbtree;

	private Map<String, String> attributes = null;

	private String[] feature_names = null;

	private String[] feature_types = null;

	private String[] metrics = null;


	public Learner(){
	}

	@Override
	public void loadBinary(XGBoostDataInput input) throws IOException {
		this.base_score = input.readFloat();
		this.num_feature = input.readInt();
		this.num_class = input.readInt();
		this.contain_extra_attrs = input.readInt();
		this.contain_eval_metrics = input.readInt();

		this.major_version = input.readInt();
		this.minor_version = input.readInt();

		if(this.major_version < 0 || this.major_version > 2){
			throw new IllegalArgumentException(this.major_version + "." + this.minor_version);
		}

		this.num_target = Math.max(input.readInt(), 1);
		this.base_score_estimated = input.readInt();

		input.readReserved(25);

		String name_obj = input.readString();

		this.obj = parseObjective(name_obj);

		// Starting from 1.0.0, the base score is saved as an untransformed value
		if(this.major_version >= 1){
			this.base_score = this.obj.probToMargin(this.base_score) + 0f;
		} else

		{
			this.base_score = this.base_score;
		}

		String name_gbm = input.readString();

		this.gbtree = parseGradientBooster(name_gbm);
		this.gbtree.loadBinary(input);

		if(this.contain_extra_attrs != 0){
			this.attributes = input.readStringMap();
		} // End if

		if(this.major_version >= 1){
			return;
		} // End if

		if(this.obj instanceof PoissonRegression){
			String max_delta_step;

			try {
				max_delta_step = input.readString();
			} catch(EOFException eofe){
				// Ignored
			}
		} // End if

		if(this.contain_eval_metrics != 0){
			this.metrics = input.readStringVector();
		}
	}

	@Override
	public void loadJSON(JsonObject root){
		UBValue value = GsonUtil.toUBValue(root);

		loadUBJSON(value.asObject());
	}

	@Override
	public void loadUBJSON(UBObject root){

		if(!root.containsKey("version")){
			throw new IllegalArgumentException("Property \"version\" not found among " + root.keySet());
		}

		int[] version = UBJSONUtil.toIntArray(root.get("version"));

		this.major_version = version[0];
		this.minor_version = version[1];

		if(this.major_version < 1 || this.major_version > 2){
			throw new IllegalArgumentException(this.major_version + "." + this.minor_version);
		}

		UBObject learner = root.get("learner").asObject();

		UBObject learnerModelParam = learner.get("learner_model_param").asObject();

		this.base_score = learnerModelParam.get("base_score").asFloat32();
		this.num_feature = learnerModelParam.get("num_feature").asInt();
		this.num_class = learnerModelParam.get("num_class").asInt();

		if(learnerModelParam.containsKey("num_target")){
			this.num_target = learnerModelParam.get("num_target").asInt();
		} else

		{
			this.num_target = 1;
		}

		UBObject objective = learner.get("objective").asObject();

		String name_obj = objective.get("name").asString();

		this.obj = parseObjective(name_obj);

		// Starting from 1.0.0, the base score is saved as an untransformed value
		this.base_score = this.obj.probToMargin(this.base_score) + 0f;

		UBObject gradientBooster = learner.get("gradient_booster").asObject();

		String name_gbm = gradientBooster.get("name").asString();

		this.gbtree = parseGradientBooster(name_gbm);
		this.gbtree.loadUBJSON(gradientBooster);

		if(learner.containsKey("attributes")){
			UBObject attributes = learner.get("attributes").asObject();

			this.attributes = new HashMap<>();

			String[] keys = {"best_iteration", "best_score"};
			for(String key : keys){

				if(attributes.containsKey(key)){
					this.attributes.put(key, attributes.get(key).asString());
				}
			}
		} // End if

		if(learner.containsKey("feature_names")){
			this.feature_names = UBJSONUtil.toStringArray(learner.get("feature_names"));
		} // End if

		if(learner.containsKey("feature_types")){
			this.feature_types = UBJSONUtil.toStringArray(learner.get("feature_types"));
		}
	}

	public <DIS extends InputStream & DataInput> void loadBinary(DIS is, String charset) throws IOException {
		boolean hasSerializationHeader = consumeHeader(is, XGBoostUtil.SERIALIZATION_HEADER);
		if(hasSerializationHeader){
			long offset = is.readLong();

			if(offset < 0L){
				throw new IOException();
			}
		} else

		{
			// Ignored
		}

		boolean hasBInfHeader = consumeHeader(is, XGBoostUtil.BINF_HEADER);
		if(hasBInfHeader){
			// Ignored
		}

		try(XGBoostDataInput input = new XGBoostDataInput(is, charset)){
			loadBinary(input);

			if(hasSerializationHeader){
				// Ignored
			} else

			{
				int eof = is.read();
				if(eof != -1){
					throw new IOException();
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	public void loadJSON(InputStream is, String charset, String jsonPath) throws IOException {
		JsonParser parser = new JsonParser();

		if(charset == null){
			charset = "UTF-8";
		}

		try(Reader reader = new InputStreamReader(is, charset)){
			JsonElement element = parser.parse(reader);

			JsonObject object = element.getAsJsonObject();

			String[] names = jsonPath.split("\\.");
			for(int i = 0; i < names.length; i++){
				String name = names[i];

				if(i == 0 && ("$").equals(name)){
					continue;
				}

				JsonElement childElement = object.get(name);
				if(childElement == null){
					throw new IllegalArgumentException("Property \"" + name + "\" not among " + object.keySet());
				}

				object = childElement.getAsJsonObject();
			}

			loadJSON(object);

			int eof = is.read();
			if(eof != -1){
				throw new IOException();
			}
		}
	}

	public void loadUBJSON(InputStream is, String jsonPath) throws IOException {

		try(UBReader reader = new UBReader(is)){
			UBObject object = reader.read().asObject();

			String[] names = jsonPath.split("\\.");
			for(int i = 0; i < names.length; i++){
				String name = names[i];

				if(i == 0 && ("$").equals(name)){
					continue;
				}

				UBValue childValue = object.get(name);
				if(childValue == null){
					throw new IllegalArgumentException("Property \"" + name + "\" not among " + object.keySet());
				}

				object = childValue.asObject();
			}

			loadUBJSON(object);

			int eof = is.read();
			if(eof != -1){
				throw new IOException();
			}
		}
	}

	public FeatureMap encodeFeatureMap(){

		if(this.feature_names == null || this.feature_types == null){
			return null;
		}

		FeatureMap result = new FeatureMap();

		for(int i = 0; i < this.feature_names.length; i++){
			result.addEntry(this.feature_names[i], this.feature_types[i]);
		}

		return result;
	}

	public Schema encodeSchema(String targetName, List<String> targetCategories, FeatureMap featureMap, XGBoostEncoder encoder){

		if(targetName == null){
			targetName = "_target";
		}

		Label label = encodeLabel(targetName, targetCategories, encoder);

		List<Feature> features = featureMap.encodeFeatures(encoder);

		return new Schema(encoder, label, features);
	}

	public Label encodeLabel(String targetName, List<String> targetCategories, XGBoostEncoder encoder){

		if(this.num_target == 1){
			return this.obj.encodeLabel(targetName, targetCategories, encoder);
		} else

		if(this.num_target >= 2){
			List<Label> labels = new ArrayList<>();

			for(int i = 0; i < this.num_target; i++){
				Label label = this.obj.encodeLabel(targetName + String.valueOf(i + 1), targetCategories, encoder);

				labels.add(label);
			}

			return new MultiLabel(labels);
		} else

		{
			throw new IllegalArgumentException();
		}
	}

	public Schema toXGBoostSchema(boolean numeric, Schema schema){
		FeatureTransformer function = new FeatureTransformer(){

			private List<? extends Feature> features = schema.getFeatures();


			@Override
			public int getSplitIndex(Feature feature){
				return this.features.indexOf(feature);
			}

			@Override
			public Feature transformNumerical(Feature feature){

				if(feature instanceof BinaryFeature){
					BinaryFeature binaryFeature = (BinaryFeature)feature;

					return binaryFeature;
				} else

				if(feature instanceof MissingValueFeature){
					MissingValueFeature missingValueFeature = (MissingValueFeature)feature;

					return missingValueFeature;
				} else

				if(feature instanceof ThresholdFeature && !numeric){
					ThresholdFeature thresholdFeature = (ThresholdFeature)feature;

					return thresholdFeature;
				} else

				{
					ContinuousFeature continuousFeature = feature.toContinuousFeature();

					DataType dataType = continuousFeature.getDataType();
					switch(dataType){
						case INTEGER:
						case FLOAT:
							break;
						case DOUBLE:
							continuousFeature = continuousFeature.toContinuousFeature(DataType.FLOAT);
							break;
						default:
							throw new IllegalArgumentException("Expected integer, float or double data type for continuous feature " + continuousFeature.getName() + ", got " + dataType.value() + " data type");
					}

					return continuousFeature;
				}
			}

			@Override
			public Feature transformCategorical(Feature feature){

				if(feature instanceof CategoricalFeature){
					CategoricalFeature categoricalFeature = (CategoricalFeature)feature;

					return categoricalFeature;
				} else

				{
					throw new IllegalArgumentException();
				}
			}
		};

		return schema.toTransformedSchema(function);
	}

	public Schema toValueFilteredSchema(Number missing, Schema schema){
		FeatureTransformer function = new FeatureTransformer(){

			private List<? extends Feature> features = schema.getFeatures();


			@Override
			public int getSplitIndex(Feature feature){
				return this.features.indexOf(feature);
			}

			@Override
			public Feature transformNumerical(Feature feature){

				if(feature instanceof BinaryFeature){
					BinaryFeature binaryFeature = (BinaryFeature)feature;

					return binaryFeature;
				} else

				if(feature instanceof MissingValueFeature){
					MissingValueFeature missingValueFeature = (MissingValueFeature)feature;

					return missingValueFeature;
				} else

				{
					ContinuousFeature continuousFeature = feature.toContinuousFeature();

					Field<?> field = continuousFeature.getField();

					if(field instanceof DataField){
						DataField dataField = (DataField)field;

						// XXX
						if(ValueUtil.isNaN(missing)){
							DataType dataType = dataField.getDataType();

							switch(dataType){
								case FLOAT:
								case DOUBLE:
									break;
								default:
									return continuousFeature;
							}
						}

						PMMLUtil.addValues(dataField, Value.Property.MISSING, Collections.singletonList(missing));

						return continuousFeature;
					} // End if

					// XXX
					if(ValueUtil.isNaN(missing)){
						return continuousFeature;
					}

					PMMLEncoder encoder = continuousFeature.getEncoder();

					Expression expression = PMMLUtil.createApply(PMMLFunctions.IF,
						PMMLUtil.createApply(PMMLFunctions.AND,
							PMMLUtil.createApply(PMMLFunctions.ISNOTMISSING, continuousFeature.ref()),
							PMMLUtil.createApply(PMMLFunctions.NOTEQUAL, continuousFeature.ref(), PMMLUtil.createConstant(missing))
						),
						continuousFeature.ref()
					);

					DerivedField derivedField = encoder.createDerivedField(FieldNameUtil.create("filter", continuousFeature, missing), OpType.CONTINUOUS, continuousFeature.getDataType(), expression);

					return new ContinuousFeature(encoder, derivedField);
				}
			}

			@Override
			public Feature transformCategorical(Feature feature){

				if(feature instanceof CategoricalFeature){
					CategoricalFeature categoricalFeature = (CategoricalFeature)feature;

					return categoricalFeature;
				} else

				{
					throw new IllegalArgumentException();
				}
			}
		};

		return schema.toTransformedSchema(function);
	}

	public PMML encodePMML(Map<String, ?> options, String targetName, List<String> targetCategories, FeatureMap featureMap){
		XGBoostEncoder encoder = new XGBoostEncoder();

		FeatureMap embeddedFeatureMap = encodeFeatureMap();
		if(embeddedFeatureMap != null){
			embeddedFeatureMap.update(featureMap);

			featureMap = embeddedFeatureMap;
		}

		Schema schema = encodeSchema(targetName, targetCategories, featureMap, encoder);

		MiningModel miningModel = encodeMiningModel(options, schema);

		PMML pmml = encoder.encodePMML(miningModel);

		return pmml;
	}

	public MiningModel encodeMiningModel(Map<String, ?> options, Schema schema){
		Number missing = (Number)options.get(HasXGBoostOptions.OPTION_MISSING);
		Boolean compact = (Boolean)options.get(HasXGBoostOptions.OPTION_COMPACT);
		Boolean numeric = (Boolean)options.get(HasXGBoostOptions.OPTION_NUMERIC);
		Boolean prune = (Boolean)options.get(HasXGBoostOptions.OPTION_PRUNE);
		Integer ntreeLimit = (Integer)options.get(HasXGBoostOptions.OPTION_NTREE_LIMIT);

		if(numeric == null){
			numeric = Boolean.TRUE;
		} // End if

		if(missing != null){
			schema = toValueFilteredSchema(missing, schema);
		}

		MiningModel miningModel = this.gbtree.encodeMiningModel(this.obj, this.base_score, ntreeLimit, numeric, schema)
			.setAlgorithmName("XGBoost (" + this.gbtree.getAlgorithmName() + ")");

		if((Boolean.TRUE).equals(compact)){

			if((Boolean.FALSE).equals(numeric)){
				throw new IllegalArgumentException("Conflicting XGBoost options");
			}

			Visitor visitor = new TreeModelCompactor();

			visitor.applyTo(miningModel);
		} // End if

		if((Boolean.TRUE).equals(prune)){
			Visitor visitor = new TreeModelPruner();

			visitor.applyTo(miningModel);
		}

		return miningModel;
	}

	public int num_feature(){
		return this.num_feature;
	}

	public int num_class(){
		return this.num_class;
	}

	public ObjFunction obj(){
		return this.obj;
	}

	public GBTree gbtree(){
		return this.gbtree;
	}

	public String getAttribute(String key){

		if(this.attributes != null && this.attributes.containsKey(key)){
			return this.attributes.get(key);
		}

		return null;
	}

	public Integer getBestIteration(){
		String bestIteration = getAttribute("best_iteration");

		if(bestIteration != null){
			return Integer.valueOf(bestIteration);
		}

		return null;
	}

	public Double getBestScore(){
		String bestScore = getAttribute("best_score");

		if(bestScore != null){
			return Double.valueOf(bestScore);
		}

		return null;
	}

	private GBTree parseGradientBooster(String name_gbm){

		switch(name_gbm){
			case "gbtree":
				return new GBTree();
			case "dart":
				return new Dart();
			default:
				throw new IllegalArgumentException(name_gbm);
		}
	}

	private ObjFunction parseObjective(String name_obj){

		switch(name_obj){
			case "reg:linear":
			case "reg:pseudohubererror":
			case "reg:squarederror":
			case "reg:squaredlogerror":
				return new LinearRegression(name_obj);
			case "reg:logistic":
				return new LogisticRegression(name_obj);
			case "reg:gamma":
			case "reg:tweedie":
				return new GeneralizedLinearRegression(name_obj);
			case "count:poisson":
				return new PoissonRegression(name_obj);
			case "binary:hinge":
				return new HingeClassification(name_obj);
			case "binary:logistic":
				return new BinomialLogisticRegression(name_obj);
			case "rank:map":
			case "rank:ndcg":
			case "rank:pairwise":
				return new LambdaMART(name_obj);
			case "survival:aft":
				return new AFT(name_obj);
			case "multi:softmax":
			case "multi:softprob":
				return new MultinomialLogisticRegression(name_obj, this.num_class);
			default:
				throw new IllegalArgumentException(name_obj);
		}
	}

	static
	private <DIS extends InputStream & DataInput> boolean consumeHeader(DIS is, String header) throws IOException {
		byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);

		byte[] buffer = new byte[headerBytes.length];

		is.mark(buffer.length);

		is.readFully(buffer);

		boolean equals = Arrays.equals(headerBytes, buffer);
		if(!equals){
			is.reset();
		}

		return equals;
	}

	abstract
	private class FeatureTransformer implements Function<Feature, Feature> {

		abstract
		public int getSplitIndex(Feature feature);

		abstract
		public Feature transformNumerical(Feature feature);

		abstract
		public Feature transformCategorical(Feature feature);

		@Override
		public Feature apply(Feature feature){
			int splitIndex = getSplitIndex(feature);

			Integer splitType = getSplitType(splitIndex);
			if(splitType == null){
				return feature;
			}

			switch(splitType){
				case Node.SPLIT_NUMERICAL:
					return transformNumerical(feature);
				case Node.SPLIT_CATEGORICAL:
					return transformCategorical(feature);
				default:
					throw new IllegalArgumentException();
			}
		}

		private Integer getSplitType(int splitIndex){
			Set<Integer> splitTypes = Learner.this.gbtree.getSplitType(splitIndex);

			if(splitTypes.size() == 0){
				return null;
			} else

			if(splitTypes.size() == 1){
				return Iterables.getOnlyElement(splitTypes);
			} else

			{
				throw new IllegalArgumentException();
			}
		}
	}
}