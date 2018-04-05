package com.example.particlefilter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Random;

/**
 * Created by Daniel on 2018. 03. 23..
 */

public class MyView extends View {   // stackoverflow code

    Paint paint;

    CopyOnWriteArrayList<Particle> particles = new CopyOnWriteArrayList<>();

    Particle heroParticle;


    public float x;
    public float y;
    public int radius = 20;

    public  static double INITIAL_ANGLE = 2.2;
    public  static double INITIAL_SPEED = 30;

    public MyView(Context context) {
        super(context);
        x = this.getX();
        y = this.getY();
        init();
    }

    public MyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        x = this.getX();
        y = this.getY();
        init();
    }

    public MyView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        x = this.getX();
        y = this.getY();
        init();
    }

    private void init() {
        // Load attributes
        paint = new Paint();
        paint.setColor(Color.RED);
        //populateArrayList();
        populateParticles();

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(0, PorterDuff.Mode.CLEAR);

        //canvas.setBitmap(imageBitmap);

        Iterator<Particle> drawiterator = particles.iterator();

        while (drawiterator.hasNext()) {

            Particle curParticle = drawiterator.next();

            canvas.drawCircle(curParticle.x, curParticle.y, (float)curParticle.weight*radius, paint);

        }
        MainActivity.getFloorplan().setImageBitmap(MainActivity.getImageBitmap());
    }


    public void populateArrayList(){
        particles.clear();
        //particles.add(new Particle(1, 782, 0, 0.2));
        //particles.add(new Particle(200, 400, 0, 0.2));
        //particles.add(new Particle(1000, 400, 0.5, 0.1,0));
        //particles.add(new Particle(1000, 400, 0, 0.4, 0));
        particles.add(new Particle(1642, 456, 0, 0.4, 0));
    }


    public int populateParticles(){
        particles.clear();

        int cntr = 0;
        int STEP = 40;
        Bitmap mask = MainActivity.getMaskBitmap();

        double[] speedvector = {-1.5,-0.75,0,0.75,1.5};
        double[] anglevector = {-0.6,-0.3,0,0.3,0.6};

        for (int i = 0; i < mask.getWidth()/3;i=i+STEP){
            for (int j = 0; j < mask.getHeight()/3;j=j+STEP){

                int color = mask.getPixel(i*3,j*3);
                int redcomponent = (color >> 16) & 0xff;

                if ((redcomponent) == 255) {

                    for (int speed = 0; speed < speedvector.length; speed++){
                        for (int angle = 0; angle < anglevector.length; angle++){

                            particles.add(new Particle(i, j, anglevector[angle], 0.1, speedvector[speed]));
                            Log.d("add "," index: "+cntr+" x: "+String.valueOf(i)+" y "+String.valueOf(j)+" angleerr: "+ String.valueOf(anglevector[angle])+" speederr: "+String.valueOf(speedvector[speed]));
                            cntr++;

                        }
                    }
                }
            }
        }

        return cntr;

    }

    public void resampling(CopyOnWriteArrayList<Particle> alive, CopyOnWriteArrayList<Particle> dead){

        Random random = new Random();

        if (alive.size() == 0) {
            alive.add(heroParticle);
            dead.remove(1);
        }


        for (int index = 0; index < dead.size(); index++){

            int randomnumber = random.nextInt(alive.size());

            Particle randomParticle = alive.get(randomnumber);
            Particle currentDeadParticle = dead.get(index);

            currentDeadParticle.x = randomParticle.x;
            currentDeadParticle.y = randomParticle.y;

            alive.add(currentDeadParticle);
        }
        particles = (CopyOnWriteArrayList<Particle>) alive.clone();
    }


    public void updatePoints(double azimuth, final Canvas canvas){

        final double angle = azimuth;

        Runnable runnable = new Runnable() {

            public void run() {

                Iterator<Particle> iterator = particles.iterator();
                int cntr = 0;
                CopyOnWriteArrayList<Particle> aliveParticles = new CopyOnWriteArrayList<>();
                CopyOnWriteArrayList<Particle> deadParticles = new CopyOnWriteArrayList<>();

                synchronized (this) {

                    while (iterator.hasNext()) {

                        Particle curParticle = iterator.next();

                        heroParticle = curParticle;


                        curParticle.x += (float) (Math.cos(angle+INITIAL_ANGLE + curParticle.angularerror) * (INITIAL_SPEED + curParticle.speederror));
                        curParticle.y += (float) (Math.sin(angle+INITIAL_ANGLE + curParticle.angularerror) * (INITIAL_SPEED + curParticle.speederror));


                        Bitmap maskBitmap = MainActivity.getMaskBitmap();

                        if(curParticle.x < 0) curParticle.x = 0;
                        if(curParticle.y < 0) curParticle.y = 0;
                        if(curParticle.x > maskBitmap.getWidth()) curParticle.x =  maskBitmap.getWidth()-1;
                        if(curParticle.y > maskBitmap.getWidth()) curParticle.y =  maskBitmap.getHeight()-1;


                        int color = maskBitmap.getPixel(((int) curParticle.x) * 3, ((int) (curParticle.y * 3)));


                        int redcomponent = (color >> 16) & 0xff;

                        //Log.d("R: ", String.valueOf(redcomponent));
                        if (redcomponent < 255) {

                            // TODO do stuff here
//
                            deadParticles.add(curParticle);
                        }
                        else{
                            aliveParticles.add(curParticle);
                        }
                        cntr++;
                    }
                    Log.d("particles: ", String.valueOf(particles.size()));
                    resampling(aliveParticles, deadParticles);

                }
            }
        };

        Thread update = new Thread(runnable);
        update.start();
        try {
            update.join();
        } catch (Exception e) {
            e.printStackTrace();
        }
        onDraw(canvas);
    }
}

class Particle{


    public float x;
    public float y;
    public double angularerror;
    public double weight;
    public double speederror;


    Particle(float x, float y, double angularerror, double weight, double speederror){
        this.x = x;
        this.y = y;
        this.angularerror = angularerror;
        this.weight = weight;
        this.speederror = speederror;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public double getAngularerror() {
        return this.angularerror;
    }

    public double getWeight() {
        return this.weight;
    }

    public double getSpeedError() {
        return this.speederror;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setAngle(double angularerror) {
        this.angularerror = angularerror;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

}