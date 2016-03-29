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
import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.dmg.pmml.PMML;
import org.jpmml.model.MetroJAXBUtil;

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
		names = {"--learner-input", "--model-input"},
		description = "XGBoost learner input file",
		required = true
	)
	private File learnerInput = null;

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


	static
	public void main(String... args) throws Exception {
		Main main = new Main();

		JCommander commander = new JCommander(main);
		commander.setProgramName(Main.class.getName());

		try {
			commander.parse(args);
		} catch(ParameterException pe){
			commander.usage();

			System.exit(-1);
		}

		if(main.help){
			commander.usage();

			System.exit(0);
		}

		main.run();
	}

	private void run() throws Exception {
		Learner learner;

		try(InputStream is = new FileInputStream(this.learnerInput)){
			learner = XGBoostUtil.loadLearner(is);
		}

		FeatureMap featureMap;

		try(InputStream is = new FileInputStream(this.fmapInput)){
			featureMap = XGBoostUtil.loadFeatureMap(is);
		}

		PMML pmml = learner.encodePMML(this.targetName, this.targetCategories, featureMap);

		try(OutputStream os = new FileOutputStream(this.pmmlOutput)){
			MetroJAXBUtil.marshalPMML(pmml, os);
		}
	}
}