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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
		is = new BufferedInputStream(is, 16);

		if((ByteOrder.BIG_ENDIAN).equals(byteOrder)){
			return loadLearner(new DataInputStream(is), charset);
		} else

		if((ByteOrder.LITTLE_ENDIAN).equals(byteOrder)){
			return loadLearner(new LittleEndianDataInputStream(is), charset);
		} else

		{
			throw new IllegalArgumentException();
		}
	}

	static
	public <DIS extends InputStream & DataInput> Learner loadLearner(DIS is, String charset) throws IOException {
		byte[] magic = XGBoostUtil.SERIALIZATION_HEADER.getBytes(StandardCharsets.UTF_8);

		if(!is.markSupported()){
			throw new IllegalArgumentException();
		}

		byte[] buffer = new byte[magic.length];

		is.mark(buffer.length);

		is.readFully(buffer);

		boolean hasSerializationHeader = Arrays.equals(buffer, magic);
		if(hasSerializationHeader){
			long offset = is.readLong();

			if(offset < 0L){
				throw new IOException();
			}
		} else

		{
			is.reset();
		}

		XGBoostDataInput input = new XGBoostDataInput(is, charset);

		Learner learner = new Learner();
		learner.load(input);

		if(hasSerializationHeader){
			// Ignored
		} else

		{
			int eof = is.read();
			if(eof != -1){
				throw new IOException();
			}
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

	public static final String SERIALIZATION_HEADER = "CONFIG-offset:";
}