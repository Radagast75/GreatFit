package com.dinodevs.greatfitwatchface.widget;

import android.app.Service;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;

import com.dinodevs.greatfitwatchface.AbstractWatchFace;
import com.dinodevs.greatfitwatchface.settings.LoadSettings;
import com.huami.watch.watchface.util.Util;
import com.ingenic.iwds.slpt.view.arc.SlptArcAnglePicView;
import com.ingenic.iwds.slpt.view.core.SlptLinearLayout;
import com.ingenic.iwds.slpt.view.core.SlptPictureView;
import com.ingenic.iwds.slpt.view.core.SlptViewComponent;
import com.ingenic.iwds.slpt.view.sport.SlptTodayCaloriesView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.dinodevs.greatfitwatchface.data.Calories;
import com.dinodevs.greatfitwatchface.data.DataType;
import com.dinodevs.greatfitwatchface.resource.ResourceManager;
import com.ingenic.iwds.slpt.view.utils.SimpleFile;


public class CaloriesWidget extends AbstractWidget {
    private TextPaint textPaint;
    private Calories calories;
    private Bitmap icon;
    private LoadSettings settings;

    private Float caloriesSweepAngle = 0f;
    private Float lastSlptUpdateCalories = 0f;
    private Integer angleLength;
    private Paint ring;
    Service mService;

    // Constructor
    public CaloriesWidget(LoadSettings settings) {
        this.settings = settings;

        if(!(settings.caloriesProg>0 && settings.caloriesProgType==0)){return;}
        if(settings.caloriesProgClockwise==1) {
            this.angleLength = (settings.caloriesProgEndAngle < settings.caloriesProgStartAngle) ? 360 - (settings.caloriesProgStartAngle - settings.caloriesProgEndAngle) : settings.caloriesProgEndAngle - settings.caloriesProgStartAngle;
        }else{
            this.angleLength = (settings.caloriesProgEndAngle > settings.caloriesProgStartAngle) ? 360 - (settings.caloriesProgStartAngle - settings.caloriesProgEndAngle) : settings.caloriesProgEndAngle - settings.caloriesProgStartAngle;
        }
    }

    // Screen-on init (runs once)
    @Override
    public void init(Service service) {
        this.mService = service;

        if(settings.calories>0) {
            // Font
            this.textPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            this.textPaint.setColor(settings.caloriesColor);
            this.textPaint.setTypeface(ResourceManager.getTypeFace(service.getResources(), ResourceManager.Font.FONT_FILE));
            this.textPaint.setTextSize(settings.caloriesFontSize);
            this.textPaint.setTextAlign((settings.caloriesAlignLeft) ? Paint.Align.LEFT : Paint.Align.CENTER);

            if (settings.caloriesIcon) {
                this.icon = Util.decodeImage(service.getResources(), "icons/calories.png");
            }
        }

        // Progress Bar Circle
        if(settings.caloriesProg>0 && settings.caloriesProgType==0){
            this.ring = new Paint(Paint.ANTI_ALIAS_FLAG);
            this.ring.setStrokeCap(Paint.Cap.ROUND);
            this.ring.setStyle(Paint.Style.STROKE);
            this.ring.setStrokeWidth(settings.caloriesProgThickness);
        }
    }

    // Register calories counter
    @Override
    public List<DataType> getDataTypes() {
        return Collections.singletonList(DataType.CALORIES);
    }

    // Calories updater
    @Override
    public void onDataUpdate(DataType type, Object value) {
        this.calories = (Calories) value;

        // Bar angle
        if(settings.caloriesProg>0 && settings.caloriesProgType==0) {
            this.caloriesSweepAngle = this.angleLength * Math.min(calories.getCalories()/settings.target_calories,1f);

            if((this.caloriesSweepAngle-this.lastSlptUpdateCalories)/settings.target_calories>0.05){
                this.lastSlptUpdateCalories = this.caloriesSweepAngle;
                ((AbstractWatchFace) this.mService).restartSlpt();
            }
        }
    }

    // Screen on
    @Override
    public void draw(Canvas canvas, float width, float height, float centerX, float centerY) {
        if(settings.calories>0) {
            if(settings.caloriesIcon){
                canvas.drawBitmap(this.icon, settings.caloriesIconLeft, settings.caloriesIconTop, settings.mGPaint);
            }

            String units = (settings.caloriesUnits) ? " kcal" : "";
            canvas.drawText(calories.getCalories() + units, settings.caloriesLeft, settings.caloriesTop, textPaint);
        }

        // Calories bar
        if(settings.caloriesProg>0 && settings.caloriesProgType==0) {
            int count = canvas.save();

            // Rotate canvas to 0 degrees = 12 o'clock
            canvas.rotate(-90, centerX, centerY);

            // Define circle
            float radius = settings.caloriesProgRadius - settings.caloriesProgThickness;
            RectF oval = new RectF(settings.caloriesProgLeft - radius, settings.caloriesProgTop - radius, settings.caloriesProgLeft + radius, settings.caloriesProgTop + radius);

            // Background
            if(settings.caloriesProgBgBool) {
                this.ring.setColor(Color.parseColor("#999999"));
                canvas.drawArc(oval, settings.caloriesProgStartAngle, this.angleLength, false, ring);
            }

            this.ring.setColor(settings.colorCodes[settings.caloriesProgColorIndex]);
            canvas.drawArc(oval, settings.caloriesProgStartAngle, this.caloriesSweepAngle, false, ring);

            canvas.restoreToCount(count);
        }

    }

    // Screen-off (SLPT)
    @Override
    public List<SlptViewComponent> buildSlptViewComponent(Service service) {
        return buildSlptViewComponent(service, false);
    }

    // Screen-off (SLPT) - Better screen quality
    public List<SlptViewComponent> buildSlptViewComponent(Service service, boolean better_resolution) {
        better_resolution = better_resolution && settings.better_resolution_when_raising_hand;
        List<SlptViewComponent> slpt_objects = new ArrayList<>();
        this.mService = service;

        if(settings.calories>0) {
            // Show or Not icon
            if (settings.battery_percentIcon) {
                SlptPictureView caloriesIcon = new SlptPictureView();
                caloriesIcon.setImagePicture(SimpleFile.readFileFromAssets(service, ((better_resolution) ? "" : "slpt_") + "icons/calories.png"));
                caloriesIcon.setStart(
                        (int) settings.caloriesIconLeft,
                        (int) settings.caloriesIconTop
                );
                slpt_objects.add(caloriesIcon);
            }

            SlptLinearLayout caloriesLayout = new SlptLinearLayout();
            caloriesLayout.add(new SlptTodayCaloriesView());
            // Show or Not Units
            if (settings.caloriesUnits) {
                SlptPictureView caloriesUnit = new SlptPictureView();
                caloriesUnit.setStringPicture(" kcal");
                caloriesLayout.add(caloriesUnit);
            }
            caloriesLayout.setTextAttrForAll(
                    settings.caloriesFontSize,
                    settings.caloriesColor,
                    ResourceManager.getTypeFace(service.getResources(), ResourceManager.Font.FONT_FILE)
            );
            // Position based on screen on
            caloriesLayout.alignX = 2;
            caloriesLayout.alignY = 0;
            int tmp_left = (int) settings.caloriesLeft;
            if (!settings.caloriesAlignLeft) {
                // If text is centered, set rectangle
                caloriesLayout.setRect(
                        (int) (2 * tmp_left + 640),
                        (int) (settings.caloriesFontSize)
                );
                tmp_left = -320;
            }
            caloriesLayout.setStart(
                    (int) tmp_left,
                    (int) (settings.caloriesTop - ((float) settings.font_ratio / 100) * settings.caloriesFontSize)
            );
            slpt_objects.add(caloriesLayout);
        }

        // Calories bar
        if(settings.caloriesProg>0 && settings.caloriesProgType==0){
            // Draw background image
            if(settings.caloriesProgBgBool) {
                SlptPictureView ring_background = new SlptPictureView();
                ring_background.setImagePicture(SimpleFile.readFileFromAssets(service, ( (better_resolution)?"":"slpt_" )+"circles/ring1_bg.png"));
                ring_background.setStart((int) (settings.caloriesProgLeft-settings.caloriesProgRadius), (int) (settings.caloriesProgTop-settings.caloriesProgRadius));
                slpt_objects.add(ring_background);
            }

            if(calories==null){calories = new Calories(0);}

            SlptArcAnglePicView localSlptArcAnglePicView = new SlptArcAnglePicView();
            localSlptArcAnglePicView.setImagePicture(SimpleFile.readFileFromAssets(service, ( (better_resolution)?"":"slpt_" )+settings.caloriesProgSlptImage));
            localSlptArcAnglePicView.setStart((int) (settings.caloriesProgLeft-settings.caloriesProgRadius), (int) (settings.caloriesProgTop-settings.caloriesProgRadius));
            localSlptArcAnglePicView.start_angle = (settings.caloriesProgClockwise==1)? settings.caloriesProgStartAngle : settings.caloriesProgEndAngle;
            localSlptArcAnglePicView.len_angle = this.angleLength * Math.min(calories.getCalories()/settings.target_calories,1);
            localSlptArcAnglePicView.full_angle = (settings.caloriesProgClockwise==1)? this.angleLength : -this.angleLength;
            localSlptArcAnglePicView.draw_clockwise = settings.caloriesProgClockwise;
            slpt_objects.add(localSlptArcAnglePicView);
        }

        return slpt_objects;
    }
}
