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

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import com.google.common.io.CharStreams;
import com.google.common.io.LittleEndianDataInputStream;

public class XGBoostUtil {

	private XGBoostUtil(){
	}

	static
	public Learner loadLearner(InputStream is) throws IOException {
		return loadLearner(is, ByteOrder.nativeOrder(), null);
	}

	static
	public Learner loadLearner(InputStream is, ByteOrder byteOrder, String charset) throws IOException {
		return loadLearner(is, byteOrder, charset, "$");
	}

	static
	public Learner loadLearner(InputStream is, ByteOrder byteOrder, String charset, String jsonPath) throws IOException {
		is = new BufferedInputStream(is, 16 * 1024);

		if((ByteOrder.BIG_ENDIAN).equals(byteOrder)){
			return loadLearner(new DataInputStream(is), charset, jsonPath);
		} else

		if((ByteOrder.LITTLE_ENDIAN).equals(byteOrder)){
			return loadLearner(new LittleEndianDataInputStream(is), charset, jsonPath);
		} else

		{
			throw new IllegalArgumentException();
		}
	}

	static
	public <DIS extends InputStream & DataInput> Learner loadLearner(DIS is, String charset, String jsonPath) throws IOException {

		if(!is.markSupported()){
			throw new IllegalArgumentException();
		}

		String signature = readSignature(is, 16);

		Learner learner = new Learner();

		if(signature.startsWith("{")){

			if(isText(signature)){
				learner.loadJSON(is, charset, jsonPath);
			} else

			{
				learner.loadUBJSON(is, jsonPath);
			}
		} else

		{
			learner.loadBinary(is, charset);
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

	static
	private String readSignature(InputStream is, int limit) throws IOException {
		is.mark(limit);

		try {
			byte[] buffer = new byte[limit];

			int length = is.read(buffer);

			return new String(buffer, 0, length);
		} finally {
			is.reset();
		}
	}

	static
	private boolean isText(String json){

		if(!json.startsWith("{")){
			throw new IllegalArgumentException();
		}

		for(int i = 1; i < json.length(); i++){
			char c = json.charAt(i);

			if(Character.isWhitespace(c)){
				continue;
			} // End if

			if(c == '\"'){
				return true;
			} else

			{
				return false;
			}
		}

		return true;
	}

	public static final String SERIALIZATION_HEADER = "CONFIG-offset:";
	public static final String BINF_HEADER = "binf";
}