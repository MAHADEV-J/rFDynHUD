/**
 * Copyright (C) 2009-2014 Cars and Tracks Development Project (CTDP).
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package net.ctdp.rfdynhud.widgets.lessons.lesson2a;

import java.awt.Font;

import net.ctdp.rfdynhud.gamedata.LiveGameData;
import net.ctdp.rfdynhud.render.DrawnString;
import net.ctdp.rfdynhud.render.DrawnString.Alignment;
import net.ctdp.rfdynhud.render.DrawnStringFactory;
import net.ctdp.rfdynhud.render.TextureImage2D;
import net.ctdp.rfdynhud.util.SubTextureCollector;
import net.ctdp.rfdynhud.valuemanagers.Clock;
import net.ctdp.rfdynhud.widgets.base.widget.Widget;
import net.ctdp.rfdynhud.widgets.lessons._util.LessonsWidgetSet;

/**
 * This Widget shows a first dynamic text.
 * 
 * @author Marvin Froehlich (CTDP)
 */
public class Lesson2aWidget_FirstDynamics extends Widget
{
    private DrawnString ds = null;
    
    public Lesson2aWidget_FirstDynamics()
    {
        super( LessonsWidgetSet.INSTANCE, LessonsWidgetSet.WIDGET_PACKAGE, 5.0f, 5.0f );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void prepareForMenuItem()
    {
        super.prepareForMenuItem();
        
        getFontProperty().setFont( "Dialog", Font.PLAIN, 9, false, true );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void initSubTextures( LiveGameData gameData, boolean isEditorMode, int widgetInnerWidth, int widgetInnerHeight, SubTextureCollector collector )
    {
    }
    
    @Override
    protected void initialize( LiveGameData gameData, boolean isEditorMode, DrawnStringFactory drawnStringFactory, TextureImage2D texture, int width, int height )
    {
        ds = drawnStringFactory.newDrawnString( "ds", 0, 0, Alignment.LEFT, false, getFont(), isFontAntiAliased(), getFontColor() );
    }
    
    @Override
    protected void drawWidget( Clock clock, boolean needsCompleteRedraw, LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height )
    {
        /*
         * Here you can see one of the very important tricks, that rfDynHUD uses to optimize performace.
         * 
         * Dynamic texts need to be redrawn frequently. And the changed pixel data needs to be sent to
         * the graphics card. This cannot be done every single frame, since this would eysily drop your
         * FPS by a huge amount.
         * 
         * We only need and want the text to be redrawn when it has actually changed. And we want all changed
         * pixels to be sent at certain time slots, because sending a hundred pixels at once is not much more
         * expensive then sending a single pixel. Yet, sending one pixel at a hundred frames in a row IS expensive.
         * 
         * This is, what the two clock parameters are good for. clock1 is for very dynamic content and will
         * always be 'true', when you can safely send new pixel data at a high frequency.
         * clock2 is the same, but has 1/3 of the frequency of clock1.
         * 
         * We will use the car's velocity, since this is one of the most dynamic values here.
         * 
         * All we need to do, is to redraw the text, if the whole widget needs to be redrawn or the value has changed
         * AND we hit a time spot to redraw. Since the velocity will most likely change most of the time,
         * we simply ask for that time spot (clock).
         */
        if ( needsCompleteRedraw || clock.c() )
        {
            /*
             * Here we can use getScalarVelocityKPH() or getScalarVelocityMPH() to explicitly use these units
             * or use getScalarVelocity() to use rFactor's setting to decide about that.
             */
            int velocity = (int)gameData.getTelemetryData().getScalarVelocity();
            
            ds.draw( offsetX, offsetY, String.valueOf( velocity ), texture );
        }
    }
}
