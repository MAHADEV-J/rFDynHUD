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
package net.ctdp.rfdynhud.widgets.lessons.lesson1;

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
 * This Widget is something like the minimum, that you need to code to implement your first Widget.
 * 
 * As you can see, it is not much code, though it also doesn't do much.
 * 
 * This Widget will simply display the standard background and border with the text "Hello World".
 * 
 * @author Marvin Froehlich (CTDP)
 */
public class Lesson1Widget_HelloWorld extends Widget
{
    /*
     * DrawnString objects are used to efficiently and easily draw texts on your Widget.
     * They take care about clearing the exact area from the previous value, if the text
     * needs to be redrawn (when it has changed).
     * 
     * The instance is usually created in the initialize() method implementation (see below).
     */
    private DrawnString ds = null;
    
    public Lesson1Widget_HelloWorld()
    {
        /*
         * In the constructor we pass in the default size of the Widget.
         * We don't need to define a location, since this is done by the editor.
         * In the below notation, we use percents, which is important
         * to create proper sizes for all screen resolutions.
         */
        super( LessonsWidgetSet.INSTANCE, LessonsWidgetSet.WIDGET_PACKAGE, 10.0f, 5.0f );
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
        /*
         * This method is more or less only invoked once before the Widget is first rendered.
         * 
         * Ok, here we instantiate our DrawnString, that we will use to draw
         * the "Hello World" text.
         * 
         * You cannot instantiate a DrawnString with 'new', but have to use the DrawnStringFactory passed in here.
         * The reason is, that instances need to be cleaned up, when they are discarded. And this is done automatically
         * by the factory.
         * 
         * Instances are identified by their name (first parameter), which is only used by the factory the clean up a replaced instance.
         * So you can use any abbreviated string here. It just need to be the same, if you create a new instance, that replaces another one.
         * 
         * The string also need a position, which is to be understood relatively to the Widget (not absolute on the screen). We use top-left (0,0) here.
         * 
         * The alignment parameter tells the system to position the left end of the text at the given x-coordinate.
         * 
         * And we want to top edge of the text to be positioned at our y location (0), so we pass in a 'false' to tell it not to use the text's base line.
         * 
         * The other three parameters should be self explanatory.
         * Their values come from some standard Poroperties, that any Widget has.
         */
        ds = drawnStringFactory.newDrawnString( "ds", 0, 0, Alignment.LEFT, false, getFont(), isFontAntiAliased(), getFontColor() );
    }
    
    @Override
    protected void drawWidget( Clock clock, boolean needsCompleteRedraw, LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height )
    {
        /*
         * Here we draw our Widget.
         * 
         * Since this method is called every time, the whole HUD is rerendered,
         * we need to do something, so that our text is only drawn when it has actually changed.
         * 
         * Since we use a static text, we only need to ask, if the whole Widget is to be redrawn,
         * which also needs our text to be drawn.
         * 
         * The DrawnString needs to know the (inner) Widget location to compute the actual text location
         * from the widget relative location, that we defined up in the initialize() method
         * and a target texture to draw on.
         */
        if ( needsCompleteRedraw )
        {
            ds.draw( offsetX, offsetY, "Hello World", texture );
        }
    }
}
