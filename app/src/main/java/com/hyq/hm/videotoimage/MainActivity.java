package com.hyq.hm.videotoimage;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.EGL14;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.SeekBar;

import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    private Handler bitmapHandler;
    private HandlerThread bitmapThread;
    private EGLUtils eglUtils;
    private GLBitmap glBitmap;
    private GLRenderer renderer;


    private Handler imageHandler;
    private HandlerThread imageThread;
    private ImageReader imageReader;

    private Handler glHandler;
    private HandlerThread glThread;
    private EGLUtils glEglUtils;
    private GLRenderer glRenderer;

    private int imageWidth,imageHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bitmapThread = new HandlerThread("BitmapThread");
        bitmapThread.start();
        bitmapHandler = new Handler(bitmapThread.getLooper());

        glBitmap = new GLBitmap(this,R.drawable.ic_jn);
        renderer = new GLRenderer(glBitmap.getWidth(),glBitmap.getHeight());

        glThread = new HandlerThread("GLThread");
        glThread.start();
        glHandler = new Handler(glThread.getLooper());
        glRenderer = new GLRenderer(glBitmap.getWidth(),glBitmap.getHeight());

        imageWidth = glBitmap.getWidth()/10;
        imageHeight = glBitmap.getHeight()/10;

        imageThread = new HandlerThread("ImageThread");
        imageThread.start();
        imageHandler = new Handler(imageThread.getLooper());
        final Rect src = new Rect(0,0,imageWidth,imageHeight);
        final RectF dst = new RectF(0,0,imageWidth,imageHeight);
        final ImageView imageView = findViewById(R.id.image_view);
        imageReader = ImageReader.newInstance(imageWidth,imageHeight, PixelFormat.RGBA_8888,1);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireNextImage();
                if(image != null) {
                    int width = image.getWidth();
                    int height = image.getHeight();
                    final Image.Plane[] planes = image.getPlanes();
                    final ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * width;
                    Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);
                    final Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bmp);
                    canvas.drawBitmap(bitmap, src, dst, null);
                    imageView.post(new Runnable() {
                        @Override
                        public void run() {
                            imageView.setImageBitmap(bmp);
                        }
                    });
                    image.close();
                }
            }
        },imageHandler);


        SurfaceView surfaceView = findViewById(R.id.surface_view);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(final SurfaceHolder holder) {
                bitmapHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        eglUtils = new EGLUtils();
                        eglUtils.initEGL(holder.getSurface(), EGL14.EGL_NO_CONTEXT);
                        glBitmap.surfaceCreated();
                        renderer.onSurfaceCreated();
                    }
                });
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, final int width, final int height) {
                glHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        while (true){
                            if(eglUtils != null){
                                EGLContext eglContext = eglUtils.getContext();
                                if(eglContext != EGL14.EGL_NO_CONTEXT){
                                    if(glEglUtils != null){
                                        glEglUtils.release();
                                    }else{
                                        glEglUtils = new EGLUtils();
                                    }
                                    glEglUtils.initEGL(imageReader.getSurface(),eglContext);
                                    glRenderer.onSurfaceCreated();
                                    glRenderer.onSurfaceChanged(imageWidth,imageHeight);
                                    break;
                                }
                            }
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                bitmapHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        renderer.onSurfaceChanged(width,height);
                        glBitmap.surfaceDraw();
                        renderer.onDrawFrame(glBitmap.getTextureId());
                        eglUtils.swap();
                        glHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                glRenderer.onDrawFrame(glBitmap.getTextureId());
                                glEglUtils.swap();
                            }
                        });
                    }
                });

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                bitmapHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        renderer.onSurfaceDestroyed();
                        glBitmap.surfaceDestroyed();
                        eglUtils.release();
                    }
                });
            }
        });
        SeekBar seekBar = findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                rotate(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void rotate(final int rotate){
        bitmapHandler.post(new Runnable() {
            @Override
            public void run() {
                glBitmap.setRadian(rotate);
                glBitmap.surfaceDraw();
                renderer.onDrawFrame(glBitmap.getTextureId());
                eglUtils.swap();
                glHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        glRenderer.onDrawFrame(glBitmap.getTextureId());
                        glEglUtils.swap();
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bitmapThread.quit();
        imageThread.quit();
        glHandler.post(new Runnable() {
            @Override
            public void run() {
                glRenderer.onSurfaceDestroyed();
                glEglUtils.release();
                glThread.quit();
            }
        });
        imageReader.close();
    }
}
