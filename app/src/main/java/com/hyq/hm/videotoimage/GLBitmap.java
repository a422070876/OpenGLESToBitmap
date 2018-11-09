package com.hyq.hm.videotoimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.opengl.GLES30;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by 海米 on 2018/10/25.
 */

public class GLBitmap {

    private int aPositionHandle;
    private int uMatrixHandle;
    private int uTextureSamplerHandle;
    private int aTextureCoordHandle;
    private int programId;
    private int[] textures = new int[2];

    private int[] frameBuffers = new int[2];
    private int[] frameColors = new int[1];

    private FloatBuffer vertexBuffer;
    private FloatBuffer textureVertexBuffer;
    private final float[] modelMatrix=new float[16];
    private final float[] projectionMatrix= new float[16];
    private final float[] viewMatrix = new float[16];
    private Bitmap bitmap;

    private int frameWidth,frameHeight;

    public GLBitmap(Context context, int id){
        scale = context.getResources().getDisplayMetrics().density;
        float[] vertexData = {
                1f, -1f,0,
                -1f, -1f,0,
                1f, 1f,0,
                -1f, 1f,0
        };
        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);
        float[] textureVertexData = {
                1f, 0f,//右下
                0f, 0f,//左下
                1f, 1f,//右上
                0f, 1f//左上
        };
        textureVertexBuffer = ByteBuffer.allocateDirect(textureVertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureVertexData);
        textureVertexBuffer.position(0);
        bitmap = BitmapFactory.decodeResource(context.getResources(),id);
        frameWidth = bitmap.getWidth();
        frameHeight = bitmap.getHeight();
    }

    public void surfaceCreated(){
        String vertexShader = "attribute vec4 aPosition;\n" +
                "attribute vec2 aTexCoord;\n" +
                "varying vec2 vTexCoord;\n" +
                "uniform mat4 uMatrix;\n" +
                "void main() {\n" +
                "    vTexCoord=aTexCoord;\n" +
                "    gl_Position = uMatrix*aPosition;\n" +
                "}";
        String fragmentShader = "precision mediump float;\n" +
                "varying vec2 vTexCoord;\n" +
                "uniform sampler2D sTexture;\n" +
                "void main() {\n" +
                "    gl_FragColor = texture2D(sTexture,vTexCoord);\n" +
                "}";
        programId = ShaderUtils.createProgram(vertexShader, fragmentShader);
        aPositionHandle = GLES30.glGetAttribLocation(programId, "aPosition");
        uMatrixHandle=GLES30.glGetUniformLocation(programId,"uMatrix");
        uTextureSamplerHandle=GLES30.glGetUniformLocation(programId,"sTexture");
        aTextureCoordHandle=GLES30.glGetAttribLocation(programId,"aTexCoord");



        GLES30.glGenTextures(2,textures,0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,textures[0]);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_MIN_FILTER,GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D,0,GLES30.GL_RGBA,bitmap,0);

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,textures[1]);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_MIN_FILTER,GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, frameWidth, frameHeight, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,0);


        GLES30.glGenRenderbuffers(1, frameColors, 0);
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, frameColors[0]);
        GLES30.glRenderbufferStorageMultisample(GLES30.GL_RENDERBUFFER,4,GLES30.GL_RGBA8, frameWidth, frameHeight);
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, 0);

        GLES30.glGenFramebuffers(2, frameBuffers,0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBuffers[0]);
        GLES30.glFramebufferRenderbuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,GLES30.GL_RENDERBUFFER, frameColors[0]);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBuffers[1]);
        GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D, textures[1], 0);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);

        GLES30.glUseProgram(programId);
        GLES30.glEnableVertexAttribArray(aPositionHandle);
        GLES30.glVertexAttribPointer(aPositionHandle, 2, GLES30.GL_FLOAT, false,
                12, vertexBuffer);
        GLES30.glEnableVertexAttribArray(aTextureCoordHandle);
        GLES30.glVertexAttribPointer(aTextureCoordHandle,2,GLES30.GL_FLOAT,false,8,textureVertexBuffer);
        GLES30.glUseProgram(0);

        Matrix.perspectiveM(projectionMatrix, 0, 90f, 1,  1, 50);
        Matrix.setLookAtM(viewMatrix, 0,
                0.0f, 0.0f, 2.0f,
                0.0f, 0.0f,0.0f,
                0.0f, -1.0f, 0.0f);

        int w = (int) (frameWidth*s);
        int f = (w - frameWidth)/2;
        rect.set(-f,0,w,frameHeight);
    }

    public void surfaceDestroyed(){
        GLES30.glDeleteProgram(programId);
        GLES30.glDeleteTextures(2,textures,0);
        GLES30.glDeleteRenderbuffers(1, frameColors, 0);
        GLES30.glDeleteFramebuffers(2,frameBuffers,0);
    }

    public int getTextureId() {
        return textures[1];
    }

    private int radian = 0;

    public void setRadian(int radian) {
        this.radian = radian;
    }

    private Rect rect = new Rect();
    private float scale = 1;
    private float s = 1.7f;

    public int getWidth(){
        return bitmap.getWidth();
    }
    public int getHeight(){
        return bitmap.getHeight();
    }

    void surfaceDraw(){

        Matrix.multiplyMM(modelMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        if(radian > 90){
            Matrix.rotateM(modelMatrix,0,radian - 180,0,-1,0);
        }else{
            Matrix.rotateM(modelMatrix,0,radian,0,-1,0);
        }
        Matrix.scaleM(modelMatrix,0,1.0f,s,1f);

        modelMatrix[3] = modelMatrix[3]/scale;
        modelMatrix[7] = modelMatrix[7]/scale;
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBuffers[0]);
        GLES30.glClearColor(1.0f,0.0f,0.0f,1.0f);
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glViewport(rect.left, rect.top, rect.right, rect.bottom);
        GLES30.glUseProgram(programId);
        GLES30.glUniformMatrix4fv(uMatrixHandle,1,false,modelMatrix,0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,textures[0]);
        GLES30.glUniform1i(uTextureSamplerHandle,0);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);

        GLES30.glBindFramebuffer(GLES30.GL_DRAW_FRAMEBUFFER,  frameBuffers[1]);
        GLES30.glBindFramebuffer(GLES30.GL_READ_FRAMEBUFFER, frameBuffers[0]);
        GLES30.glBlitFramebuffer(0, 0, frameWidth, frameHeight,
                0, 0,frameWidth, frameHeight,
                GLES30.GL_COLOR_BUFFER_BIT, GLES30.GL_LINEAR);
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,0);
        GLES30.glUseProgram(0);
    }
}
