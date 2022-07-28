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
package net.ctdp.rfdynhud.widgets.etv2010._base;

import java.awt.Font;
import java.io.File;
import java.io.IOException;

import net.ctdp.rfdynhud.gamedata.GameFileSystem;
import net.ctdp.rfdynhud.properties.BackgroundProperty;
import net.ctdp.rfdynhud.properties.BooleanProperty;
import net.ctdp.rfdynhud.properties.ColorProperty;
import net.ctdp.rfdynhud.properties.FilenameProperty;
import net.ctdp.rfdynhud.properties.IntProperty;
import net.ctdp.rfdynhud.properties.PropertiesContainer;
import net.ctdp.rfdynhud.properties.PropertyLoader;
import net.ctdp.rfdynhud.util.PropertyWriter;
import net.ctdp.rfdynhud.widgets.base.widget.Widget;
import net.ctdp.rfdynhud.widgets.base.widget.WidgetPackage;
import net.ctdp.rfdynhud.widgets.base.widget.WidgetSet;
import net.ctdp.rfdynhud.widgets.etv2010._util.ETVImages;
import net.ctdp.rfdynhud.widgets.etv2010._util.ETVWidgetSet;

public abstract class ETVWidgetBase extends Widget
{
    protected final BooleanProperty useImages = new BooleanProperty( "useImages", true )
    {
        @Override
        protected void onValueChanged( Boolean oldValue, boolean newValue )
        {
            images = null;
        }
    };
    
    protected final FilenameProperty imagesIni = new FilenameProperty( "imagesIni", null, "etv2010/general/etv_2010_images.ini", new String[] { "ini" }, new String[] { "ini files" }, new File( GameFileSystem.IMAGES_FOLDER_NAME ), false )
    {
        @Override
        protected void onValueChanged( String oldValue, String newValue )
        {
            images = null;
        }
    };
    
    private ETVImages images = null;
    
    protected final ColorProperty captionBackgroundColor = new ColorProperty( "captionBgColor", ETVWidgetSet.ETV_CAPTION_BACKGROUND_COLOR.getKey() );
    protected final ColorProperty captionColor = new ColorProperty( "captionColor", ETVWidgetSet.ETV_CAPTION_FONT_COLOR.getKey() );
    protected final ColorProperty dataBackgroundColor = new ColorProperty( "dataBgColor", ETVWidgetSet.ETV_DATA_BACKGROUND_COLOR.getKey() );
    
    protected final IntProperty itemGap = new IntProperty( "itemGap", 3, 0, 100 );
    
    protected final BooleanProperty showNamesInAllUppercase = new BooleanProperty( "showNamesInAllUppercase", "namesUpperCase", true );
    
    public ETVWidgetBase( WidgetSet widgetSet, WidgetPackage widgetPackage, float width, float height )
    {
        super( widgetSet, widgetPackage, width, true, height, true );
        
        getFontColorProperty().setValue( ETVWidgetSet.ETV_DATA_FONT_COLOR.getKey() );
        getFontProperty().setValue( ETVWidgetSet.ETV_FONT.getKey() );
    }
    
    @Override
    protected String getInitialBackground()
    {
        return ( BackgroundProperty.COLOR_INDICATOR + "#00000000" );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void prepareForMenuItem()
    {
        super.prepareForMenuItem();
        
        useImages.setBooleanValue( false );
        
        itemGap.setIntValue( 0 );
        
        getFontProperty().setFont( "Dialog", Font.PLAIN, 4, false, true );
    }
    
    @Override
    protected boolean canHaveBorder()
    {
        return ( false );
    }
    
    /*
    @Override
    protected boolean hasText()
    {
        return ( false );
    }
    */
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void saveProperties( PropertyWriter writer ) throws IOException
    {
        super.saveProperties( writer );
        
        writer.writeProperty( useImages, "Whether to use images to render the items." );
        writer.writeProperty( imagesIni, "The ini file, that configures the background images." );
        writer.writeProperty( captionBackgroundColor, "The background color for the \"Lap\" caption." );
        writer.writeProperty( captionColor, "The font color for the \"Lap\" caption." );
        writer.writeProperty( dataBackgroundColor, "The background color for the data fields." );
        writer.writeProperty( itemGap, "The gap between the elements in pixels." );
        writer.writeProperty( showNamesInAllUppercase, "Whether to display names in all upper case." );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void loadProperty( PropertyLoader loader )
    {
        super.loadProperty( loader );
        
        if ( loader.loadProperty( useImages ) );
        else if ( loader.getCurrentKey().equals( imagesIni.getName() ) )
        {
            if ( loader.getSourceVersion().getBuild() >= 91 )
                loader.loadProperty( imagesIni );
        }
        else if ( loader.loadProperty( captionBackgroundColor ) );
        else if ( loader.loadProperty( captionColor ) );
        else if ( ( loader.getSourceVersion().getBuild() < 78 ) && loader.getCurrentKey().equals( "backgroundColor" ) )
            dataBackgroundColor.loadValue( loader, loader.getCurrentValue() );
        else if ( loader.loadProperty( dataBackgroundColor ) );
        else if ( loader.loadProperty( itemGap ) );
        else if ( loader.loadProperty( showNamesInAllUppercase ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void getPropertiesForParentGroup( PropertiesContainer propsCont, boolean forceAll )
    {
        super.getPropertiesForParentGroup( propsCont, forceAll );
        
        propsCont.addProperty( showNamesInAllUppercase );
    }
    
    /**
     * 
     * @param propsCont the container to add the properties to
     * @param forceAll If <code>true</code>, all properties provided by this {@link Widget} must be added.
     *                 If <code>false</code>, only the properties, that are relevant for the current {@link Widget}'s situation have to be added, some can be ignored.
     */
    protected void getPropertiesCaptionBG( PropertiesContainer propsCont, boolean forceAll )
    {
        if ( forceAll || !useImages.getBooleanValue() )
        {
            propsCont.addProperty( captionBackgroundColor );
        }
    }
    
    /**
     * 
     * @param propsCont the container to add the properties to
     * @param forceAll If <code>true</code>, all properties provided by this {@link Widget} must be added.
     *                 If <code>false</code>, only the properties, that are relevant for the current {@link Widget}'s situation have to be added, some can be ignored.
     */
    protected void getPropertiesCaption( PropertiesContainer propsCont, boolean forceAll )
    {
        getPropertiesCaptionBG( propsCont, forceAll );
        
        propsCont.addProperty( captionColor );
    }
    
    /**
     * 
     * @param propsCont the container to add the properties to
     * @param forceAll If <code>true</code>, all properties provided by this {@link Widget} must be added.
     *                 If <code>false</code>, only the properties, that are relevant for the current {@link Widget}'s situation have to be added, some can be ignored.
     */
    protected void getPropertiesDataBG( PropertiesContainer propsCont, boolean forceAll )
    {
        if ( forceAll || !useImages.getBooleanValue() )
        {
            propsCont.addProperty( dataBackgroundColor );
        }
    }
    
    /**
     * 
     * @param propsCont the container to add the properties to
     * @param forceAll If <code>true</code>, all properties provided by this {@link Widget} must be added.
     *                 If <code>false</code>, only the properties, that are relevant for the current {@link Widget}'s situation have to be added, some can be ignored.
     */
    protected void getPropertiesData( PropertiesContainer propsCont, boolean forceAll )
    {
        getPropertiesDataBG( propsCont, forceAll );
    }
    
    /**
     * 
     * @param propsCont the container to add the properties to
     * @param forceAll If <code>true</code>, all properties provided by this {@link Widget} must be added.
     *                 If <code>false</code>, only the properties, that are relevant for the current {@link Widget}'s situation have to be added, some can be ignored.
     */
    protected void getItemGapProperty( PropertiesContainer propsCont, boolean forceAll )
    {
        propsCont.addProperty( itemGap );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void getProperties( PropertiesContainer propsCont, boolean forceAll )
    {
        super.getProperties( propsCont, forceAll );
        
        propsCont.addGroup( "Images and Colors" );
        
        propsCont.addProperty( useImages );
        
        if ( forceAll || useImages.getBooleanValue() )
        {
            propsCont.addProperty( imagesIni );
        }
        
        getPropertiesCaption( propsCont, forceAll );
        getPropertiesData( propsCont, forceAll );
        
        getItemGapProperty( propsCont, forceAll );
    }
    
    protected final ETVImages getImages()
    {
        if ( !useImages.getBooleanValue() )
            return ( null );
        
        if ( images == null )
        {
            images = new ETVImages( imagesIni.getFileValue() );
        }
        
        return ( images );
    }
    
    protected final boolean getShowNamesInAllUppercase()
    {
        return ( showNamesInAllUppercase.getBooleanValue() );
    }
}
