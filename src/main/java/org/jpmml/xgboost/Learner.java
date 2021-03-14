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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.dmg.pmml.DataType;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.dmg.pmml.Visitor;
import org.dmg.pmml.mining.MiningModel;
import org.jpmml.converter.BaseNFeature;
import org.jpmml.converter.BinaryFeature;
import org.jpmml.converter.ContinuousFeature;
import org.jpmml.converter.Feature;
import org.jpmml.converter.Label;
import org.jpmml.converter.MissingValueFeature;
import org.jpmml.converter.Schema;
import org.jpmml.converter.visitors.NaNAsMissingDecorator;
import org.jpmml.xgboost.visitors.TreeModelCompactor;

public class Learner implements BinaryLoadable, JSONLoadable {

	private float base_score;

	private int num_feature;

	private int num_class;

	private int contain_extra_attrs;

	private int contain_eval_metrics;

	private int major_version;

	private int minor_version;

	private ObjFunction obj;

	private GBTree gbtree;

	private Map<String, String> attributes = null;

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

		if(this.major_version < 0 || this.major_version > 1){
			throw new IllegalArgumentException(this.major_version + "." + this.minor_version);
		}

		input.readReserved(27);

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
		JsonArray version = root.getAsJsonArray("version");

		this.major_version = (version.get(0)).getAsInt();
		this.minor_version = (version.get(1)).getAsInt();

		if(this.major_version < 1 || this.minor_version < 3){
			throw new IllegalArgumentException();
		}

		JsonObject learner = root.getAsJsonObject("learner");

		JsonObject learnerModelParam = learner.getAsJsonObject("learner_model_param");

		this.base_score = learnerModelParam.getAsJsonPrimitive("base_score").getAsFloat();
		this.num_feature = learnerModelParam.getAsJsonPrimitive("num_feature").getAsInt();
		this.num_class = learnerModelParam.getAsJsonPrimitive("num_class").getAsInt();

		JsonObject objective = learner.getAsJsonObject("objective");

		String name_obj = objective.getAsJsonPrimitive("name").getAsString();

		this.obj = parseObjective(name_obj);

		// Starting from 1.0.0, the base score is saved as an untransformed value
		this.base_score = this.obj.probToMargin(this.base_score) + 0f;

		JsonObject gradientBooster = learner.getAsJsonObject("gradient_booster");

		String name_gbm = gradientBooster.getAsJsonPrimitive("name").getAsString();

		this.gbtree = parseGradientBooster(name_gbm);
		this.gbtree.loadJSON(gradientBooster);
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

	public void loadJSON(InputStream is, String charset) throws IOException {
		JsonParser parser = new JsonParser();

		if(charset == null){
			charset = "UTF-8";
		}

		try(Reader reader = new InputStreamReader(is, charset)){
			JsonElement element = parser.parse(reader);

			JsonObject object = element.getAsJsonObject();

			loadJSON(object);

			int eof = is.read();
			if(eof != -1){
				throw new IOException();
			}
		}
	}

	public Schema encodeSchema(FieldName targetField, List<String> targetCategories, FeatureMap featureMap, XGBoostEncoder encoder){

		if(targetField == null){
			targetField = FieldName.create("_target");
		}

		Label label = this.obj.encodeLabel(targetField, targetCategories, encoder);

		List<Feature> features = featureMap.encodeFeatures(encoder);

		return new Schema(encoder, label, features);
	}

	public Schema toXGBoostSchema(Schema schema){
		Function<Feature, Feature> function = new Function<Feature, Feature>(){

			@Override
			public Feature apply(Feature feature){

				if(feature instanceof BaseNFeature){
					BaseNFeature baseFeature = (BaseNFeature)feature;

					return baseFeature;
				} else

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
		};

		return schema.toTransformedSchema(function);
	}

	public PMML encodePMML(Map<String, ?> options, FieldName targetField, List<String> targetCategories, FeatureMap featureMap){
		XGBoostEncoder encoder = new XGBoostEncoder();

		Boolean nanAsMissing = (Boolean)options.get(HasXGBoostOptions.OPTION_NAN_AS_MISSING);

		Schema schema = encodeSchema(targetField, targetCategories, featureMap, encoder);

		MiningModel miningModel = encodeMiningModel(options, schema);

		PMML pmml = encoder.encodePMML(miningModel);

		if((Boolean.TRUE).equals(nanAsMissing)){
 			Visitor visitor = new NaNAsMissingDecorator();

 			visitor.applyTo(pmml);
 		}

		return pmml;
	}

	public MiningModel encodeMiningModel(Map<String, ?> options, Schema schema){
		Boolean compact = (Boolean)options.get(HasXGBoostOptions.OPTION_COMPACT);
		Integer ntreeLimit = (Integer)options.get(HasXGBoostOptions.OPTION_NTREE_LIMIT);

		MiningModel miningModel = this.gbtree.encodeMiningModel(this.obj, this.base_score, ntreeLimit, schema)
			.setAlgorithmName("XGBoost (" + this.gbtree.getAlgorithmName() + ")");

		if((Boolean.TRUE).equals(compact)){
			Visitor visitor = new TreeModelCompactor();

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
			case "reg:squarederror":
			case "reg:squaredlogerror":
				return new LinearRegression();
			case "reg:logistic":
				return new LogisticRegression();
			case "reg:gamma":
			case "reg:tweedie":
				return new GeneralizedLinearRegression();
			case "count:poisson":
				return new PoissonRegression();
			case "binary:hinge":
				return new HingeClassification();
			case "binary:logistic":
				return new BinomialLogisticRegression();
			case "rank:map":
			case "rank:ndcg":
			case "rank:pairwise":
				return new LambdaMART();
			case "multi:softmax":
			case "multi:softprob":
				return new MultinomialLogisticRegression(this.num_class);
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
}