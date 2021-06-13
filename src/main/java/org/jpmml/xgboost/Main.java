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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.model.metro.MetroJAXBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

	@Parameter (
		names = {"--fmap-input"},
		description = "XGBoost feature map input file",
		required = true
	)
	private File fmapInput = null;

	@Parameter (
		names = {"--help"},
		description = "Show the list of configuration options and exit",
		help = true
	)
	private boolean help = false;

	@Parameter (
		names = {"--model-input"},
		description = "XGBoost model input file",
		required = true
	)
	private File modelInput = null;

	@Parameter (
		names = {"--byte-order"},
		description = "Endianness of XGBoost model input file. Possible values \"BIG_ENDIAN\" (\"BE\") or \"LITTLE_ENDIAN\" (\"LE\")"
	)
	private String byteOrder = (ByteOrder.nativeOrder()).toString();

	@Parameter (
		names = {"--charset"},
		description = "Charset of XGBoost model input file"
	)
	private String charset = null;

	@Parameter (
		names = {"--json-path"},
		description = "JSONPath expression of the JSON model element"
	)
	private String jsonPath = "$";

	@Parameter (
		names = {"--missing-value"},
		description = "String representation of feature value(s) that should be regarded as missing"
	)
	private String missingValue = null;

	@Parameter (
		names = {"--pmml-output"},
		description = "PMML output file",
		required = true
	)
	private File pmmlOutput = null;

	@Parameter (
		names = {"--target-name"},
		description = "Target name. Defaults to \"_target\""
	)
	private String targetName = null;

	@Parameter (
		names = {"--target-categories"},
		description = "Target categories. Defaults to 0-based index [0, 1, .., num_class - 1]"
	)
	private List<String> targetCategories = null;

	/**
	 * @see HasXGBoostOptions#OPTION_COMPACT
	 */
	@Parameter (
		names = {"--X-compact"},
		description = "Transform XGBoost-style trees to PMML-style trees",
		arity = 1
	)
	private boolean compact = true;

	/**
	 * @see HasXGBoostOptions#OPTION_NUMERIC
	 */
	@Parameter (
		names = {"--X-numeric"},
		arity = 1,
		hidden = true
	)
	private boolean numeric = true;

	/**
	 * @see HasXGBoostOptions#OPTION_PRUNE
	 */
	@Parameter (
		names = {"--X-prune"},
		arity = 1,
		hidden = true
	)
	private boolean prune = true;

	@Parameter (
 		names = {"--X-nan-as-missing"},
 		description = "Treat Not-a-Number (NaN) values as missing values",
 		arity = 1
 	)
 	private boolean nanAsMissing = true;

	/**
	 * @see HasXGBoostOptions#OPTION_NTREE_LIMIT
	 */
	@Parameter (
		names = {"--X-ntree-limit"},
		description = "Limit the number of trees. Defaults to all trees"
	)
	private Integer ntreeLimit = null;


	static
	public void main(String... args) throws Exception {
		Main main = new Main();

		JCommander commander = new JCommander(main);
		commander.setProgramName(Main.class.getName());

		try {
			commander.parse(args);
		} catch(ParameterException pe){
			StringBuilder sb = new StringBuilder();

			sb.append(pe.toString());
			sb.append("\n");

			commander.usage(sb);

			System.err.println(sb.toString());

			System.exit(-1);
		}

		if(main.help){
			StringBuilder sb = new StringBuilder();

			commander.usage(sb);

			System.out.println(sb.toString());

			System.exit(0);
		}

		main.run();
	}

	private void run() throws Exception {
		Learner learner;

		ByteOrder byteOrder = ByteOrderUtil.forValue(this.byteOrder);

		try(InputStream is = new FileInputStream(this.modelInput)){
			logger.info("Parsing learner..");

			long begin = System.currentTimeMillis();
			learner = XGBoostUtil.loadLearner(is, byteOrder, this.charset, this.jsonPath);
			long end = System.currentTimeMillis();

			logger.info("Parsed learner in {} ms.", (end - begin));
		} catch(Exception e){
			logger.error("Failed to parse learner", e);

			throw e;
		}

		FeatureMap featureMap;

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

		if(this.missingValue != null){
			featureMap.addMissingValue(this.missingValue);
		}

		Map<String, Object> options = new LinkedHashMap<>();
		options.put(HasXGBoostOptions.OPTION_COMPACT, this.compact);
		options.put(HasXGBoostOptions.OPTION_NUMERIC, this.numeric);
		options.put(HasXGBoostOptions.OPTION_PRUNE, this.prune);
		options.put(HasXGBoostOptions.OPTION_NAN_AS_MISSING, this.nanAsMissing);
		options.put(HasXGBoostOptions.OPTION_NTREE_LIMIT, this.ntreeLimit);

		PMML pmml;

		try {
			logger.info("Converting learner to PMML..");

			long begin = System.currentTimeMillis();
			pmml = learner.encodePMML(options, this.targetName != null ? FieldName.create(this.targetName) : null, this.targetCategories, featureMap);
			long end = System.currentTimeMillis();

			logger.info("Converted learner to PMML in {} ms.", (end - begin));
		} catch(Exception e){
			logger.error("Failed to convert learner to PMML", e);

			throw e;
		}

		try(OutputStream os = new FileOutputStream(this.pmmlOutput)){
			logger.info("Marshalling PMML..");

			long begin = System.currentTimeMillis();
			MetroJAXBUtil.marshalPMML(pmml, os);
			long end = System.currentTimeMillis();

			logger.info("Marshalled PMML in {} ms.", (end - begin));
		} catch(Exception e){
			logger.error("Failed to marshal PMML", e);

			throw e;
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(Main.class);
}