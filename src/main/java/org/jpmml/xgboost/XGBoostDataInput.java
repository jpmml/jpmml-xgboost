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

import java.io.Closeable;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.io.ByteStreams;

public class XGBoostDataInput implements Closeable {

	private InputStream is = null;

	private String charset = null;


	public XGBoostDataInput(InputStream is, String charset){
		this.is = is;
		this.charset = charset;
	}

	@Override
	public void close() throws IOException {
		this.is.close();
	}

	public int readInt() throws IOException {
		return asDataInput().readInt();
	}

	public int[] readIntVector() throws IOException {
		int length = (int)readLong();

		return readIntArray(length);
	}

	public int[] readIntArray(int length) throws IOException {
		DataInput dataInput = asDataInput();

		int[] result = new int[length];

		for(int i = 0; i < result.length; i++){
			result[i] = dataInput.readInt();
		}

		return result;
	}

	public long readLong() throws IOException {
		return asDataInput().readLong();
	}

	public float readFloat() throws IOException {
		return asDataInput().readFloat();
	}

	public float[] readFloatVector() throws IOException {
		int length = (int)readLong();

		return readFloatArray(length);
	}

	public float[] readFloatArray(int length) throws IOException {
		DataInput dataInput = asDataInput();

		float[] result = new float[length];

		for(int i = 0; i < result.length; i++){
			result[i] = dataInput.readFloat();
		}

		return result;
	}

	public String readString() throws IOException {
		int length = (int)readLong();

		byte[] buffer = new byte[length];

		ByteStreams.readFully(this.is, buffer);

		if(this.charset != null){
			return new String(buffer, this.charset);
		}

		return new String(buffer);
	}

	public String[] readStringVector() throws IOException {
		int length = (int)readLong();

		return readStringArray(length);
	}

	public String[] readStringArray(int length) throws IOException {
		String[] result = new String[length];

		for(int i = 0; i < result.length; i++){
			result[i] = readString();
		}

		return result;
	}

	public Map<String, String> readStringMap() throws IOException {
		int length = (int)readLong();

		Map<String, String> result = new LinkedHashMap<>();

		for(int i = 0; i < length; i++){
			result.put(readString(), readString());
		}

		return result;
	}

	public <E extends BinaryLoadable> E[] readObjectVector(Class<? extends E> clazz) throws IOException {
		int length = (int)readLong();

		return readObjectArray(clazz, length);
	}

	public <E extends BinaryLoadable> E[] readObjectArray(Class<? extends E> clazz, int length) throws IOException {
		E[] result = (E[])Array.newInstance(clazz, length);

		for(int i = 0; i < result.length; i++){
			E object;

			try {
				object = clazz.newInstance();
			} catch(ReflectiveOperationException roe){
				throw new IOException(roe);
			}

			object.loadBinary(this);

			result[i] = object;
		}

		return result;
	}

	public void readReserved(int length) throws IOException {
		int[] buffer = new int[length];

		boolean empty = true;

		for(int i = 0; i < length; i++){
			int value = readInt();

			buffer[i] = value;

			empty &= (value == 0);
		}

		if(!empty){
			throw new IOException("Expected " + length + "-element array of zeroes, got " + Arrays.toString(buffer));
		}
	}

	private DataInput asDataInput(){
		return (DataInput)this.is;
	}
}