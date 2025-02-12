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

import com.beust.jcommander.DefaultUsageFormatter;
import com.beust.jcommander.IUsageFormatter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.dmg.pmml.PMML;
import org.jpmml.model.JAXBSerializer;
import org.jpmml.model.metro.MetroJAXBSerializer;
import org.jpmml.xgboost.ByteOrderUtil;
import org.jpmml.xgboost.FeatureMap;
import org.jpmml.xgboost.HasXGBoostOptions;
import org.jpmml.xgboost.Learner;
import org.jpmml.xgboost.XGBoostUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

	@Parameter (
		names = {"--model-input"},
		description = "XGBoost model input file",
		required = true,
		order = 1
	)
	private File modelInput = null;

	@Parameter (
		names = {"--fmap-input"},
		description = "XGBoost feature map input file",
		order = 2
	)
	private File fmapInput = null;

	@Parameter (
		names = {"--pmml-output"},
		description = "PMML output file",
		required = true,
		order = 3
	)
	private File pmmlOutput = null;

	@Parameter (
		names = {"--" + HasXGBoostOptions.OPTION_BYTE_ORDER},
		description = "Endianness of XGBoost model input file. Possible values \"BIG_ENDIAN\" (\"BE\") or \"LITTLE_ENDIAN\" (\"LE\")",
		order = 4
	)
	private String byteOrder = (ByteOrder.nativeOrder()).toString();

	@Parameter (
		names = {"--" + HasXGBoostOptions.OPTION_CHARSET},
		description = "Charset of XGBoost model input file",
		order = 5
	)
	private String charset = null;

	@Parameter (
		names = {"--json-path"},
		description = "JSONPath expression of the JSON model element",
		order = 6
	)
	private String jsonPath = "$";

	@Parameter (
		names = {"--target-name"},
		description = "Target name. Defaults to \"_target\"",
		order = 7
	)
	private String targetName = null;

	@Parameter (
		names = {"--target-categories"},
		description = "Target categories. Defaults to 0-based index [0, 1, .., num_class - 1]",
		order = 8
	)
	private List<String> targetCategories = null;

	@Parameter (
 		names = {"--X-" + HasXGBoostOptions.OPTION_MISSING},
 		description = "Missing value. Defaults to Not-a-Number (NaN) value",
 		order = 9
 	)
 	private Float missing = Float.NaN;

	@Parameter (
		names = {"--X-" + HasXGBoostOptions.OPTION_COMPACT},
		description = "Transform XGBoost-style trees to PMML-style trees",
		arity = 1,
		order = 10
	)
	private boolean compact = true;

	@Parameter (
		names = {"--X-" + HasXGBoostOptions.OPTION_INPUT_FLOAT},
		description = "Allow field data type updates",
		arity = 1,
		order = 11
	)
	private Boolean inputFloat = null;

	@Parameter (
		names = {"--X-" + HasXGBoostOptions.OPTION_NUMERIC},
		description = "Simplify non-numeric split conditions to numeric split conditions",
		arity = 1,
		order = 12
	)
	private boolean numeric = true;

	@Parameter (
		names = {"--X-" + HasXGBoostOptions.OPTION_PRUNE},
		description = "Remove unreachable nodes",
		arity = 1,
		order = 13
	)
	private boolean prune = true;

	@Parameter (
		names = {"--X-" + HasXGBoostOptions.OPTION_NTREE_LIMIT},
		description = "Limit the number of trees. Defaults to all trees",
		order = 14
	)
	private Integer ntreeLimit = null;

	@Parameter (
		names = {"--help"},
		description = "Show the list of configuration options and exit",
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
		options.put(HasXGBoostOptions.OPTION_COMPACT, this.compact);
		options.put(HasXGBoostOptions.OPTION_INPUT_FLOAT, this.inputFloat);
		options.put(HasXGBoostOptions.OPTION_NUMERIC, this.numeric);
		options.put(HasXGBoostOptions.OPTION_PRUNE, this.prune);
		options.put(HasXGBoostOptions.OPTION_NTREE_LIMIT, this.ntreeLimit);

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