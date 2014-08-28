/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.projecttango.tangoutils.renderables;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.projecttango.tangoutils.MathUtils;

import android.opengl.GLES20;
import android.opengl.Matrix;

public class Axis extends Renderable {

	private static final int COORDS_PER_VERTEX = 3;
	
	private static final String sVertexShaderCode =
			"uniform mat4 uMVPMatrix;"
			+ "attribute vec4 vPosition;"
			+ "attribute vec4 aColor;"
			+ "varying vec4 vColor;"
			+ "void main() {"+
			"  vColor=aColor;" 
			+ "gl_Position = uMVPMatrix * vPosition;"
			+ "}";

	private static final String sFragmentShaderCode = 
			"precision mediump float;"
			+ "varying vec4 vColor;" 
			+ "void main() {"
			+ "gl_FragColor = vColor;" + 
			"}";
	
	private float[] mTranslation = new float[3];
	private float[] mQuaternion = new float[4];
	private FloatBuffer mVertexBuffer, mColorBuffer;

	private float mVertices[] = {  
			0.0f, 0.0f, 0.0f,
		    1.0f, 0.0f, 0.0f,
		    
		    0.0f, 0.0f, 0.0f,
		    0.0f, 1.0f, 0.0f,
		    
		    0.0f, 0.0f, 0.0f,
		    0.0f, 0.0f, 1.0f};

	private float mColors[] = { 
			1.0f, 0.0f, 0.0f, 1.0f,
			1.0f, 0.0f, 0.0f, 1.0f,
			
			0.0f, 1.0f, 0.0f, 1.0f,
			0.0f, 1.0f, 0.0f, 1.0f,
			
			0.0f, 0.0f, 1.0f, 1.0f,
			0.0f, 0.0f, 1.0f, 1.0f};

	private final int mProgram;
	private int mPosHandle, mColorHandle;
	private int mMVPMatrixHandle;

	public Axis() {
		// Set model matrix to the identity
		Matrix.setIdentityM(getModelMatrix(), 0);

		// Put vertices into a vertex buffer
		ByteBuffer byteBuf = ByteBuffer.allocateDirect(mVertices.length * 4);
		byteBuf.order(ByteOrder.nativeOrder());
		mVertexBuffer = byteBuf.asFloatBuffer();
		mVertexBuffer.put(mVertices);
		mVertexBuffer.position(0);

		// Put colors into a color buffer
		ByteBuffer cByteBuff = ByteBuffer.allocateDirect(mColors.length * 4);
		cByteBuff.order(ByteOrder.nativeOrder());
		mColorBuffer = cByteBuff.asFloatBuffer();
		mColorBuffer.put(mColors);
		mColorBuffer.position(0);

		// Load the vertex and fragment shaders, then link the program
		int vertexShader = RenderUtils.loadShader(GLES20.GL_VERTEX_SHADER, sVertexShaderCode);
		int fragShader = RenderUtils.loadShader(GLES20.GL_FRAGMENT_SHADER, sFragmentShaderCode);
		mProgram = GLES20.glCreateProgram();
		GLES20.glAttachShader(mProgram, vertexShader);
		GLES20.glAttachShader(mProgram, fragShader);
		GLES20.glLinkProgram(mProgram);
	}

	/**
	 * Updates the model matrix (rotation and translation) of the Axis.
	 * @param translation a three-element array of translation data.
	 * @param quaternion a four-element array of rotation data.
	 */
	public void updateModelMatrix(float[] translation, float[] quaternion) {
		mTranslation = translation;
		mQuaternion = quaternion;
		
		float[] openglQuaternion = MathUtils.convertQuaternionToOpenGl(quaternion);
		float[] quaternionMatrix = new float[16];
		
		//quaternionMatrix = MathUtils.quaternionM(openglQuaternion);
		quaternionMatrix = MathUtils.quaternionM(quaternion);		
		Matrix.setIdentityM(getModelMatrix(), 0);
		Matrix.translateM(getModelMatrix(), 0, translation[0], translation[2], -translation[1]);

		// Update the model matrix with rotation data
		if (quaternionMatrix != null) {
			float[] mTempMatrix = new float[16];
			Matrix.setIdentityM(mTempMatrix, 0);	
			Matrix.multiplyMM(mTempMatrix, 0, getModelMatrix(), 0, quaternionMatrix, 0);
			System.arraycopy(mTempMatrix, 0, getModelMatrix(), 0, 16);
		}
	};

	public void updateViewMatrix(float[] viewMatrix) {
		Matrix.setLookAtM(viewMatrix, 0, 0, 5.0f, 5.0f, mTranslation[0], mTranslation[1], 
				mTranslation[2], 0, 1, 0);
	}

	@Override
	public void draw(float[] viewMatrix, float[] projectionMatrix) {
		GLES20.glUseProgram(mProgram);
		// updateViewMatrix(viewMatrix);

		// Compose the model, view, and projection matrices into a single m-v-p matrix
		updateMvpMatrix(viewMatrix, projectionMatrix);

		// Load vertex attribute data
		mPosHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
		GLES20.glVertexAttribPointer(mPosHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, 
				mVertexBuffer);
		GLES20.glEnableVertexAttribArray(mPosHandle);

		// Load color attribute data
		mColorHandle = GLES20.glGetAttribLocation(mProgram, "aColor");
		GLES20.glVertexAttribPointer(mColorHandle, 4, GLES20.GL_FLOAT, false, 0, mColorBuffer);
		GLES20.glEnableVertexAttribArray(mColorHandle);

		// Draw the Axis
		mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
		GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, getMvpMatrix(), 0);
		GLES20.glLineWidth(5);
		GLES20.glDrawArrays(GLES20.GL_LINES, 0, 6);

	}
	
}