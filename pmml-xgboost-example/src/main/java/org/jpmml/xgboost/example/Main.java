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
package org.jpmml.xgboost.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.beust.jcommander.DefaultUsageFormatter;
import com.beust.jcommander.IUsageFormatter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.dmg.pmml.PMML;
import org.jpmml.converter.Parameters;
import org.jpmml.converter.SchemaParameters;
import org.jpmml.converter.tree.TreeModelParameters;
import org.jpmml.model.JAXBSerializer;
import org.jpmml.model.metro.MetroJAXBSerializer;
import org.jpmml.xgboost.ByteOrderUtil;
import org.jpmml.xgboost.FeatureMap;
import org.jpmml.xgboost.HasXGBoostOptions;
import org.jpmml.xgboost.Learner;
import org.jpmml.xgboost.ObjFunction;
import org.jpmml.xgboost.XGBoostUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

	@Parameter (
		names = {"--xgb-input", "--xgboost-input", "--model-input"},
		description = "XGBoost model input file",
		required = true,
		order = Parameters.BLOCK_IO + 1
	)
	private File xgbInput = null;

	@Parameter (
		names = {Parameters.NAME_PMML_OUTPUT},
		description = Parameters.DESCRIPTION_PMML_OUTPUT,
		required = true,
		order = Parameters.BLOCK_IO + 2
	)
	private File pmmlOutput = null;

	@Parameter (
		names = {"--fmap-input"},
		description = "XGBoost feature map input file",
		order = Parameters.BLOCK_IO + 3
	)
	private File fmapInput = null;

	@Parameter (
		names = {"--byte-order"},
		description = "Endianness of XGBoost model input file. Possible values \"BIG_ENDIAN\" (\"BE\") or \"LITTLE_ENDIAN\" (\"LE\")",
		order = Parameters.BLOCK_IO + 4
	)
	private String byteOrder = (ByteOrder.nativeOrder()).toString();

	@Parameter (
		names = {"--charset"},
		description = "Charset of XGBoost model input file",
		order = Parameters.BLOCK_IO + 5
	)
	private String charset = null;

	@Parameter (
		names = {"--json-path"},
		description = "JSONPath expression of the JSON model element",
		order = Parameters.BLOCK_IO + 6
	)
	private String jsonPath = "$";

	@Parameter (
		names = {SchemaParameters.NAME_TARGET_NAME},
		description = SchemaParameters.DESCRIPTION_TARGET_NAME,
		order = Parameters.BLOCK_SCHEMA + 1
	)
	private String targetName = null;

	@Parameter (
		names = {SchemaParameters.NAME_TARGET_CATEGORIES},
		description = SchemaParameters.DESCRIPTION_TARGET_CATEGORIES,
		order = Parameters.BLOCK_SCHEMA + 2
	)
	private List<String> targetCategories = null;

	@Parameter (
		names = {"--missing"},
		description = "Missing value. Defaults to Not-a-Number (NaN) value",
		order = Parameters.BLOCK_SCHEMA + 3
	)
	private Float missing = null;

	@Parameter (
		names = {"--ntree-limit"},
		description = "Limit the number of trees. Defaults to all trees",
		order = Parameters.BLOCK_MODEL + 1
	)
	private Integer ntreeLimit = null;

	@Parameter (
		names = {"--objective"},
		description = "Custom objective function",
		order = Parameters.BLOCK_MODEL + 2
	)
	private String objective = null;

	@Parameter (
		names = {SchemaParameters.NAME_INPUT_FLOAT},
		description = SchemaParameters.DESCRIPTION_INPUT_FLOAT,
		arity = 1,
		order = Parameters.BLOCK_SCHEMA_OPTIONS + 1
	)
	private Boolean inputFloat = null;

	@Parameter (
		names = {SchemaParameters.NAME_NUMERIC},
		description = SchemaParameters.DESCRIPTION_NUMERIC,
		arity = 1,
		order = Parameters.BLOCK_SCHEMA_OPTIONS + 2
	)
	private Boolean numeric = null;

	@Parameter (
		names = {TreeModelParameters.NAME_COMPACT},
		description = TreeModelParameters.DESCRIPTION_COMPACT,
		arity = 1,
		order = Parameters.BLOCK_MODEL_OPTIONS + 1
	)
	private Boolean compact = null;

	@Parameter (
		names = {TreeModelParameters.NAME_FLAT},
		description = TreeModelParameters.DESCRIPTION_FLAT,
		arity = 1,
		order = Parameters.BLOCK_MODEL_OPTIONS + 2
	)
	private Boolean flat = null;

	@Parameter (
		names = {TreeModelParameters.NAME_NODE_ID},
		description = TreeModelParameters.DESCRIPTION_NODE_ID,
		arity = 1,
		order = Parameters.BLOCK_MODEL_OPTIONS + 3
	)
	private Boolean nodeId = null;

	@Parameter (
		names = {TreeModelParameters.NAME_NODE_RECORDCOUNT},
		description = TreeModelParameters.DESCRIPTION_NODE_RECORDCOUNT,
		arity = 1,
		order = Parameters.BLOCK_MODEL_OPTIONS + 4
	)
	private Boolean nodeRecordCount = null;

	@Parameter (
		names = {TreeModelParameters.NAME_PRUNE},
		description = TreeModelParameters.DESCRIPTION_PRUNE,
		arity = 1,
		order = Parameters.BLOCK_MODEL_OPTIONS + 5
	)
	private Boolean prune = null;

	@Parameter (
		names = {Parameters.NAME_HELP},
		description = Parameters.DESCRIPTION_HELP,
		help = true,
		order = Integer.MAX_VALUE
	)
	private boolean help = false;


	static
	public void main(String... args) throws Exception {
		Main main = new Main();

		JCommander commander = new JCommander(main);
		commander.setProgramName(Main.class.getName());

		IUsageFormatter usageFormatter = new DefaultUsageFormatter(commander);

		try {
			commander.parse(args);
		} catch(ParameterException pe){
			StringBuilder sb = new StringBuilder();

			sb.append(pe.toString());
			sb.append("\n");

			usageFormatter.usage(sb);

			System.err.println(sb.toString());

			System.exit(-1);
		}

		if(main.help){
			StringBuilder sb = new StringBuilder();

			usageFormatter.usage(sb);

			System.out.println(sb.toString());

			System.exit(0);
		}

		main.run();
	}

	private void run() throws Exception {
		Learner learner;

		ByteOrder byteOrder = ByteOrderUtil.forValue(this.byteOrder);

		try(InputStream is = new FileInputStream(this.xgbInput)){
			logger.info("Parsing learner..");

			long begin = System.currentTimeMillis();
			learner = XGBoostUtil.loadLearner(is, byteOrder, this.charset, this.jsonPath);
			long end = System.currentTimeMillis();

			logger.info("Parsed learner in {} ms.", (end - begin));
		} catch(Exception e){
			logger.error("Failed to parse learner", e);

			throw e;
		}

		if(this.objective != null){
			logger.info("Setting custom objective function");

			ObjFunction obj = learner.parseObjective(this.objective, learner.num_class());

			learner.setObj(obj);
		}

		FeatureMap featureMap = null;

		if(this.fmapInput != null){

			try(InputStream is = new FileInputStream(this.fmapInput)){
				logger.info("Parsing feature map..");

				long begin = System.currentTimeMillis();
				featureMap = XGBoostUtil.loadFeatureMap(is);
				long end = System.currentTimeMillis();

				logger.info("Parsed feature map in {} ms.", (end - begin));
			} catch(Exception e){
				logger.error("Failed to parse feature map", e);

				throw e;
			}
		}

		Map<String, Object> options = new LinkedHashMap<>();
		options.put(HasXGBoostOptions.OPTION_MISSING, this.missing);
		options.put(HasXGBoostOptions.OPTION_NTREE_LIMIT, this.ntreeLimit);
		options.put(HasXGBoostOptions.OPTION_INPUT_FLOAT, this.inputFloat);
		options.put(HasXGBoostOptions.OPTION_NUMERIC, this.numeric);
		options.put(HasXGBoostOptions.OPTION_COMPACT, this.compact);
		options.put(HasXGBoostOptions.OPTION_FLAT, this.flat);
		options.put(HasXGBoostOptions.OPTION_NODE_ID, this.nodeId);
		options.put(HasXGBoostOptions.OPTION_NODE_RECORDCOUNT, this.nodeRecordCount);
		options.put(HasXGBoostOptions.OPTION_PRUNE, this.prune);

		options.values().removeIf(Objects::isNull);

		PMML pmml;

		try {
			logger.info("Converting learner to PMML..");

			long begin = System.currentTimeMillis();
			pmml = learner.encodePMML(options, this.targetName, this.targetCategories, featureMap);
			long end = System.currentTimeMillis();

			logger.info("Converted learner to PMML in {} ms.", (end - begin));
		} catch(Exception e){
			logger.error("Failed to convert learner to PMML", e);

			throw e;
		}

		try(OutputStream os = new FileOutputStream(this.pmmlOutput)){
			logger.info("Marshalling PMML..");

			JAXBSerializer jaxbSerializer = new MetroJAXBSerializer();

			long begin = System.currentTimeMillis();
			jaxbSerializer.serializePretty(pmml, os);
			long end = System.currentTimeMillis();

			logger.info("Marshalled PMML in {} ms.", (end - begin));
		} catch(Exception e){
			logger.error("Failed to marshal PMML", e);

			throw e;
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(Main.class);
}