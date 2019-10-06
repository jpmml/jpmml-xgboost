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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import com.google.common.io.CharStreams;

public class XGBoostUtil {

	private XGBoostUtil(){
	}

	static
	public Learner loadLearner(InputStream is) throws IOException {
		return loadLearner(is, ByteOrder.nativeOrder(), null);
	}

	static
	public Learner loadLearner(InputStream is, ByteOrder byteOrder, String charset) throws IOException {
		XGBoostDataInput input = new XGBoostDataInput(is, byteOrder, charset);

		Learner learner = new Learner();
		learner.load(input);

		int eof = is.read();
		if(eof != -1){
			throw new IOException();
		}

		return learner;
	}

	static
	public FeatureMap loadFeatureMap(InputStream is) throws IOException {
		FeatureMap featureMap = new FeatureMap();

		Iterator<String> lines = parseFeatureMap(is);
		for(int i = 0; lines.hasNext(); i++){
			String line = lines.next();

			StringTokenizer st = new StringTokenizer(line, "\t");
			if(st.countTokens() != 3){
				throw new IllegalArgumentException(line);
			}

			String id = st.nextToken();
			String name = st.nextToken();
			String type = st.nextToken();

			if(Integer.parseInt(id) != i){
				throw new IllegalArgumentException(id);
			}

			featureMap.addEntry(name, type);
		}

		return featureMap;
	}

	static
	private Iterator<String> parseFeatureMap(InputStream is) throws IOException {
		Reader reader = new InputStreamReader(is, "UTF-8");

		List<String> lines = CharStreams.readLines(reader);

		return lines.iterator();
	}
}