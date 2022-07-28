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
package net.ctdp.rfdynhud.widgets.etv2010.telemetry;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import net.ctdp.rfdynhud.gamedata.LiveGameData;
import net.ctdp.rfdynhud.gamedata.ProfileInfo.SpeedUnits;
import net.ctdp.rfdynhud.gamedata.TelemetryData;
import net.ctdp.rfdynhud.gamedata.VehicleScoringInfo;
import net.ctdp.rfdynhud.properties.BackgroundProperty;
import net.ctdp.rfdynhud.properties.BooleanProperty;
import net.ctdp.rfdynhud.properties.ColorProperty;
import net.ctdp.rfdynhud.properties.FontProperty;
import net.ctdp.rfdynhud.properties.ImageProperty;
import net.ctdp.rfdynhud.properties.IntProperty;
import net.ctdp.rfdynhud.properties.PropertiesContainer;
import net.ctdp.rfdynhud.properties.PropertyLoader;
import net.ctdp.rfdynhud.render.ImageTemplate;
import net.ctdp.rfdynhud.render.Texture2DCanvas;
import net.ctdp.rfdynhud.render.TextureImage2D;
import net.ctdp.rfdynhud.render.TransformableTexture;
import net.ctdp.rfdynhud.util.PropertyWriter;
import net.ctdp.rfdynhud.util.SubTextureCollector;
import net.ctdp.rfdynhud.valuemanagers.Clock;
import net.ctdp.rfdynhud.widgets.base.revneedlemeter.AbstractRevNeedleMeterWidget;
import net.ctdp.rfdynhud.widgets.base.widget.Widget;
import net.ctdp.rfdynhud.widgets.etv2010._util.ETVWidgetSet;

/**
 * This {@link Widget} attempts to imitate the 2010er TV overlay for F1 telemetry
 * (revs, velocity and throttle/brake).
 * 
 * @author Marvin Froehlich (CTDP)
 */
public class ETVTelemetryWidget extends AbstractRevNeedleMeterWidget
{
    public static final int MAX_VELOCITY_LOCAL_Z_INDEX = NEEDLE_LOCAL_Z_INDEX / 2;
    public static final int CONTROLS_LOCAL_Z_INDEX = MAX_VELOCITY_LOCAL_Z_INDEX + 1;
    
    private final BooleanProperty displayVelocityNumbers = new BooleanProperty( "displayVelocityNumbers", "displayNumbers", true );
    
    private final FontProperty velocityNumberFont = new FontProperty( "velocityNumberFont", "font", ETVWidgetSet.ETV_VELOCITY_FONT.getKey(), false );
    private final ColorProperty velocityNumberFontColor = new ColorProperty( "velocityNumberFontColor", "color", ETVWidgetSet.ETV_CAPTION_FONT_COLOR.getKey(), false );
    
    private final IntProperty velocityNumber1PosX = new IntProperty( "velocityNumber1PosX", "pos1X", 270 );
    private final IntProperty velocityNumber1PosY = new IntProperty( "velocityNumber1PosY", "pos1Y", 620 );
    private final IntProperty velocityNumber2PosX = new IntProperty( "velocityNumber2PosX", "pos2X", 100 );
    private final IntProperty velocityNumber3PosX = new IntProperty( "velocityNumber3PosX", "pos3X", 100 );
    private final IntProperty velocityNumber4PosX = new IntProperty( "velocityNumber4PosX", "pos4X", 270 );
    private final IntProperty velocityNumber4PosY = new IntProperty( "velocityNumber4PosY", "pos4Y", 50 );
    private final IntProperty velocityUnitsPosX = new IntProperty( "velocityUnitsPosX", "unitsPosX", 300 );
    private final IntProperty velocityUnitsPosY = new IntProperty( "velocityUnitsPosY", "unitsPosY", 660 );
    
    private final IntProperty maxVelocity = new IntProperty( "maxVelocity", 340, 1, 1000 );
    private final IntProperty velocity2 = new IntProperty( "velocity2", 110, 1, 1000 );
    private final IntProperty velocity3 = new IntProperty( "velocity3", 220, 1, 1000 );
    
    private final ImageProperty maxVelocityOverlay = new ImageProperty( "maxVelocityOverlay", "image", "etv2010/telemetry/max_velocity.png" );
    private TransformableTexture maxVelocityTexture = null;
    private final IntProperty maxVelocityLeftOffset = new IntProperty( "maxVelocityLeftOffset", "leftOffset", 15 );
    private final IntProperty maxVelocityTopOffset = new IntProperty( "maxVelocityTopOffset", "topOffset", 80 );
    
    private boolean throttleDirty = true;
    
    private final ImageProperty throttleImage = new ImageProperty( "throttleImage", null, "etv2010/telemetry/throttle.png", false, false )
    {
        @Override
        protected void onValueChanged( String oldValue, String newValue )
        {
            throttleDirty = true;
        }
    };
    
    private TransformableTexture texThrottle1 = null;
    private TransformableTexture texThrottle2 = null;
    
    private boolean brakeDirty = true;
    
    private final ImageProperty brakeImage = new ImageProperty( "brakeImage", null, "etv2010/telemetry/brake.png", false, false )
    {
        @Override
        protected void onValueChanged( String oldValue, String newValue )
        {
            brakeDirty = true;
        }
    };
    
    private TransformableTexture texBrake1 = null;
    private TransformableTexture texBrake2 = null;
    
    private final IntProperty controlsPosX = new IntProperty( "controlsPosX", 600 )
    {
        @Override
        protected void onValueChanged( Integer oldValue, int newValue )
        {
            throttleDirty = true;
            brakeDirty = true;
        }
    };
    private final IntProperty controlsPosY = new IntProperty( "controlsPosY", 400 )
    {
        @Override
        protected void onValueChanged( Integer oldValue, int newValue )
        {
            throttleDirty = true;
            brakeDirty = true;
        }
    };
    private final IntProperty controlsWidth = new IntProperty( "controlsWidth", 350 )
    {
        @Override
        protected void onValueChanged( Integer oldValue, int newValue )
        {
            throttleDirty = true;
            brakeDirty = true;
        }
    };
    private final IntProperty controlsHeight = new IntProperty( "controlsHeight", 80 )
    {
        @Override
        protected void onValueChanged( Integer oldValue, int newValue )
        {
            throttleDirty = true;
            brakeDirty = true;
        }
    };
    private final IntProperty controlsGap = new IntProperty( "controlsGap", "gap", 2 )
    {
        @Override
        protected void onValueChanged( Integer oldValue, int newValue )
        {
            throttleDirty = true;
            brakeDirty = true;
        }
    };
    
    private final FontProperty controlsLabelFont = new FontProperty( "controlsLabelFont", "labelFont", ETVWidgetSet.ETV_CONTROLS_LABEL_FONT.getKey() )
    {
        @Override
        protected void onValueChanged( String oldValue, String newValue )
        {
            throttleDirty = true;
            brakeDirty = true;
        }
    };
    
    private final ColorProperty controlsLabelFontColor = new ColorProperty( "controlsLabelFontColor", "labelFontColor", "#FFFFFF" )
    {
        @Override
        protected void onValueChanged( String oldValue, String newValue )
        {
            throttleDirty = true;
            brakeDirty = true;
        }
    };
    
    private final IntProperty controlsLabelOffset = new IntProperty( "controlsLabelOffset", "labelOffset", 15 )
    {
        @Override
        protected void onValueChanged( Integer oldValue, int newValue )
        {
            brakeDirty = true;
            throttleDirty = true;
        }
    };
    
    public ETVTelemetryWidget()
    {
        super( ETVWidgetSet.INSTANCE, ETVWidgetSet.WIDGET_PACKAGE, 19.6915f, 21.75f );
        
        minValue.setFloatValue( 4000 );
        
        displayMarkers.setBooleanValue( false );
        markersInnerRadius.setIntValue( 170 );
        markersLength.setIntValue( 50 );
        markersOnCircle.setBooleanValue( true );
        firstMarkerNumberOffset.setFloatValue( +5 );
        lastMarkerNumberOffset.setFloatValue( -5 );
        markersBigStep.setIntValue( 2000 );
        markersSmallStep.setIntValue( 1000 );
        markersFont.setFont( ETVWidgetSet.ETV_REV_MARKERS_FONT.getKey() );
        markersFontColor.setColor( "#FFFFFF" );
        markersFontDropShadowColor.setColor( "#000000" );
        markerNumbersCentered.setBooleanValue( true );
        
        needlePivotBottomOffset.setIntValue( -171 );
        peakNeedlePivotBottomOffset.setIntValue( -226 );
        
        needleMountX.setIntValue( 506 );
        needleMountY.setIntValue( 350 );
        
        needleRotationForMinValue.setFloatValue( -178 );
        needleRotationForMaxValue.setFloatValue( +72.5f );
        
        displayValue.setBooleanValue( false );
        displayGear.setBooleanValue( true );
        gearPosX.setIntValue( 510 );
        gearPosY.setIntValue( 345 );
        gearFont.setFont( ETVWidgetSet.ETV_GEAR_FONT.getKey() );
        gearFontColor.setColor( "#D9E0EB" );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void prepareForMenuItem()
    {
        super.prepareForMenuItem();
        
        controlsLabelFont.setFont( "Dialog", Font.PLAIN, 4, false, true );
        velocityNumberFont.setFont( "Dialog", Font.PLAIN, 4, false, true );
    }
    
    @Override
    protected void saveDigiValueProperties( PropertyWriter writer ) throws IOException
    {
        // We don't need these here!
    }
    
    @Override
    protected void getDigiValueProperties( PropertiesContainer propsCont, boolean forceAll )
    {
        // We don't need these here!
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void saveProperties( PropertyWriter writer ) throws IOException
    {
        super.saveProperties( writer );
        
        writer.writeProperty( displayVelocityNumbers, "Display nicely positioned velocity numbers?" );
        
        writer.writeProperty( velocityNumberFont, "The font for the velocity numbers." );
        writer.writeProperty( velocityNumberFontColor, "The font color for the velocity numbers." );
        
        writer.writeProperty( velocityNumber1PosX, "The x-position in background texture space for the first velocity number." );
        writer.writeProperty( velocityNumber1PosY, "The y-position in background texture space for the first velocity number." );
        writer.writeProperty( velocity2, "The second velocity." );
        writer.writeProperty( velocityNumber2PosX, "The x-position in background texture space for the second velocity number." );
        writer.writeProperty( velocity3, "The third velocity." );
        writer.writeProperty( velocityNumber3PosX, "The x-position in background texture space for the third velocity number." );
        writer.writeProperty( velocityNumber4PosX, "The x-position in background texture space for the fourth velocity number." );
        writer.writeProperty( velocityNumber4PosY, "The y-position in background texture space for the fourth velocity number." );
        writer.writeProperty( velocityUnitsPosX, "The x-position in background texture space for te units display." );
        writer.writeProperty( velocityUnitsPosY, "The y-position in background texture space for te units display." );
        
        writer.writeProperty( maxVelocity, "The maximum velocity in km/h." );
        writer.writeProperty( maxVelocityOverlay, "The image name for the max velocity overlay." );
        writer.writeProperty( maxVelocityLeftOffset, "The x-offset in background image space for the max velocity overlay." );
        writer.writeProperty( maxVelocityTopOffset, "The y-offset in background image space for the max velocity overlay." );
        
        writer.writeProperty( throttleImage, "The image for the throttle gauge." );
        writer.writeProperty( brakeImage, "The image for the brake gauge." );
        writer.writeProperty( controlsPosX, "The x-offset in background image space for the controls display." );
        writer.writeProperty( controlsPosY, "The y-offset in background image space for the controls display." );
        writer.writeProperty( controlsWidth, "The width in background image space for the controls display." );
        writer.writeProperty( controlsHeight, "The height in background image space for the controls display." );
        writer.writeProperty( controlsGap, "The gap in pixels between the throttle and brake bars." );
        writer.writeProperty( controlsLabelFont, "The font for the controls labels." );
        writer.writeProperty( controlsLabelFontColor, "The font color for the controls labels." );
        writer.writeProperty( controlsLabelOffset, "The offset for bar text from the left boundary of the bar." );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void loadProperty( PropertyLoader loader )
    {
        super.loadProperty( loader );
        
        if ( loader.loadProperty( displayVelocityNumbers ) );
        else if ( loader.loadProperty( velocityNumberFont ) );
        else if ( loader.loadProperty( velocityNumberFont ) );
        else if ( loader.loadProperty( velocityNumber1PosX ) );
        else if ( loader.loadProperty( velocityNumber1PosY ) );
        else if ( loader.loadProperty( velocity2 ) );
        else if ( loader.loadProperty( velocityNumber2PosX ) );
        else if ( loader.loadProperty( velocity3 ) );
        else if ( loader.loadProperty( velocityNumber3PosX ) );
        else if ( loader.loadProperty( velocityNumber4PosX ) );
        else if ( loader.loadProperty( velocityNumber4PosY ) );
        else if ( loader.loadProperty( velocityUnitsPosX ) );
        else if ( loader.loadProperty( velocityUnitsPosY ) );
        
        else if ( loader.loadProperty( maxVelocity ) );
        else if ( loader.loadProperty( maxVelocityOverlay ) );
        else if ( loader.loadProperty( maxVelocityLeftOffset ) );
        else if ( loader.loadProperty( maxVelocityTopOffset ) );
        
        else if ( loader.loadProperty( throttleImage ) );
        else if ( loader.loadProperty( brakeImage ) );
        else if ( loader.loadProperty( controlsPosX ) );
        else if ( loader.loadProperty( controlsPosY ) );
        else if ( loader.loadProperty( controlsWidth ) );
        else if ( loader.loadProperty( controlsHeight ) );
        else if ( loader.loadProperty( controlsGap ) );
        else if ( loader.loadProperty( controlsLabelFont ) );
        else if ( loader.loadProperty( controlsLabelFontColor ) );
        else if ( loader.loadProperty( controlsLabelOffset ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void getProperties( PropertiesContainer propsCont, boolean forceAll )
    {
        super.getProperties( propsCont, forceAll );
        
        propsCont.addGroup( "Velocity Numbers" );
        
        propsCont.addProperty( displayVelocityNumbers );
        
        if ( displayVelocityNumbers.getBooleanValue() || forceAll )
        {
            propsCont.addProperty( velocityNumberFont );
            propsCont.addProperty( velocityNumberFontColor );
            propsCont.addProperty( velocityNumber1PosX );
            propsCont.addProperty( velocityNumber1PosY );
            propsCont.addProperty( velocity2 );
            propsCont.addProperty( velocityNumber2PosX );
            propsCont.addProperty( velocity3 );
            propsCont.addProperty( velocityNumber3PosX );
            propsCont.addProperty( velocityNumber4PosX );
            propsCont.addProperty( velocityNumber4PosY );
            propsCont.addProperty( velocityUnitsPosX );
            propsCont.addProperty( velocityUnitsPosY );
        }
        
        propsCont.addGroup( "Max Velocity Overlay" );
        
        propsCont.addProperty( maxVelocity );
        propsCont.addProperty( maxVelocityOverlay );
        propsCont.addProperty( maxVelocityLeftOffset );
        propsCont.addProperty( maxVelocityTopOffset );
        
        propsCont.addGroup( "Controls" );
        
        propsCont.addProperty( throttleImage );
        propsCont.addProperty( brakeImage );
        propsCont.addProperty( controlsPosX );
        propsCont.addProperty( controlsPosY );
        propsCont.addProperty( controlsWidth );
        propsCont.addProperty( controlsHeight );
        propsCont.addProperty( controlsGap );
        propsCont.addProperty( controlsLabelFont );
        propsCont.addProperty( controlsLabelFontColor );
        propsCont.addProperty( controlsLabelOffset );
    }
    
    @Override
    protected String getInitialBackground()
    {
        return ( BackgroundProperty.IMAGE_INDICATOR + "etv2010/telemetry/background.png" );
    }
    
    @Override
    protected void onBackgroundChanged( boolean imageChanged, float deltaScaleX, float deltaScaleY )
    {
        super.onBackgroundChanged( imageChanged, deltaScaleX, deltaScaleY );
        
        maxVelocityLeftOffset.setIntValue( Math.round( maxVelocityLeftOffset.getIntValue() * deltaScaleX ) );
        maxVelocityTopOffset.setIntValue( Math.round( maxVelocityTopOffset.getIntValue() * deltaScaleY ) );
    }
    
    @Override
    protected String getInitialNeedleImage()
    {
        return ( "etv2010/telemetry/needle.png" );
    }
    
    @Override
    protected String getInitialPeakNeedleImage()
    {
        return ( "etv2010/telemetry/peak_needle.png" );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected Boolean onVehicleControlChanged( VehicleScoringInfo viewedVSI, LiveGameData gameData, boolean isEditorMode )
    {
        Boolean result = super.onVehicleControlChanged( viewedVSI, gameData, isEditorMode );
        
        if ( texThrottle1 != null )
            texThrottle1.setVisible( viewedVSI.isPlayer() );
        
        if ( texThrottle2 != null )
            texThrottle2.setVisible( viewedVSI.isPlayer() );
        
        if ( texBrake1 != null )
            texBrake1.setVisible( viewedVSI.isPlayer() );
        
        if ( texBrake2 != null )
            texBrake2.setVisible( viewedVSI.isPlayer() );
        
        return ( result );
    }
    
    private void drawBarLabel( String label, TextureImage2D texture, int offsetX, int offsetY, int width, int height )
    {
        Rectangle2D bounds = TextureImage2D.getStringBounds( label, controlsLabelFont );
        
        //int lblOff = Math.round( controlsLabelOffset.getIntValue() * getBackground().getScaleX() );
        int lblOff = controlsLabelOffset.getIntValue();
        
        if ( ( lblOff > -bounds.getWidth() ) && ( lblOff < width ) )
            texture.drawString( label, offsetX + lblOff, offsetY + ( height - (int)bounds.getHeight() ) / 2 - (int)bounds.getY(), bounds, controlsLabelFont.getFont(), controlsLabelFont.isAntiAliased(), controlsLabelFontColor.getColor(), true, null );
    }
    
    private void loadThrottleTexture( boolean isEditorMode, int w, int h )
    {
        if ( ( texThrottle1 == null ) || ( texThrottle1.getWidth() != w ) || ( texThrottle1.getHeight() != h ) || throttleDirty )
        {
            texThrottle1 = TransformableTexture.getOrCreate( w, h, TransformableTexture.DEFAULT_PIXEL_PERFECT_POSITIONING, texThrottle1, isEditorMode );
            texThrottle2 = TransformableTexture.getOrCreate( w, h, TransformableTexture.DEFAULT_PIXEL_PERFECT_POSITIONING, texThrottle2, isEditorMode );
            
            ImageTemplate it = throttleImage.getImage();
            it.drawScaled( 0, 0, it.getBaseWidth(), it.getBaseHeight() / 2, 0, 0, w, h, texThrottle1.getTexture(), true );
            it.drawScaled( 0, it.getBaseHeight() / 2, it.getBaseWidth(), it.getBaseHeight() / 2, 0, 0, w, h, texThrottle2.getTexture(), true );
            
            drawBarLabel( Loc.throttle_label, texThrottle1.getTexture(), 0, 0, texThrottle1.getWidth(), texThrottle1.getHeight() );
            drawBarLabel( Loc.throttle_label, texThrottle2.getTexture(), 0, 0, texThrottle2.getWidth(), texThrottle2.getHeight() );
            
            float x = controlsPosX.getFloatValue() * getBackground().getScaleX();
            float y = controlsPosY.getFloatValue() * getBackground().getScaleY();
            
            texThrottle1.setTranslation( x, y );
            texThrottle2.setTranslation( x, y );
            
            texThrottle1.setLocalZIndex( CONTROLS_LOCAL_Z_INDEX );
            texThrottle2.setLocalZIndex( CONTROLS_LOCAL_Z_INDEX + 1 );
            
            throttleDirty = false;
        }
    }
    
    private void loadBrakeTexture( boolean isEditorMode, int w, int h )
    {
        if ( ( texBrake1 == null ) || ( texBrake1.getWidth() != w ) || ( texBrake1.getHeight() != h ) || brakeDirty )
        {
            texBrake1 = TransformableTexture.getOrCreate( w, h, TransformableTexture.DEFAULT_PIXEL_PERFECT_POSITIONING, texBrake1, isEditorMode );
            texBrake2 = TransformableTexture.getOrCreate( w, h, TransformableTexture.DEFAULT_PIXEL_PERFECT_POSITIONING, texBrake2, isEditorMode );
            
            ImageTemplate it = brakeImage.getImage();
            it.drawScaled( 0, 0, it.getBaseWidth(), it.getBaseHeight() / 2, 0, 0, w, h, texBrake1.getTexture(), true );
            it.drawScaled( 0, it.getBaseHeight() / 2, it.getBaseWidth(), it.getBaseHeight() / 2, 0, 0, w, h, texBrake2.getTexture(), true );
            
            drawBarLabel( Loc.brake_label, texBrake1.getTexture(), 0, 0, texBrake1.getWidth(), texBrake1.getHeight() );
            drawBarLabel( Loc.brake_label, texBrake2.getTexture(), 0, 0, texBrake2.getWidth(), texBrake2.getHeight() );
            
            float x = controlsPosX.getFloatValue() * getBackground().getScaleX();
            float y = controlsPosY.getFloatValue() * getBackground().getScaleY() + h + controlsGap.getIntValue();
            
            texBrake1.setTranslation( x, y );
            texBrake2.setTranslation( x, y );
            
            texBrake1.setLocalZIndex( CONTROLS_LOCAL_Z_INDEX );
            texBrake2.setLocalZIndex( CONTROLS_LOCAL_Z_INDEX + 1 );
            
            brakeDirty = false;
        }
    }
    
    private void drawVelocityNumbers( SpeedUnits units, TextureImage2D texture, int offsetX, int offsetY )
    {
        if ( !displayVelocityNumbers.getBooleanValue() || ( maxVelocityTexture == null ) )
            return;
        
        float scaleX = getBackground().getScaleX();
        float scaleY = getBackground().getScaleY();
        
        Texture2DCanvas texCanvas = texture.getTextureCanvas();
        
        texCanvas.setFont( velocityNumberFont.getFont() );
        texCanvas.setAntialiazingEnabled( velocityNumberFont.isAntiAliased() );
        FontMetrics metrics = texCanvas.getFontMetrics();
        
        Color dropShadowColor = markersFontDropShadowColor.getColor();
        float dropShadowOffset = 2.2f; //numberFont.getFont().getSize() * 0.2f;
        boolean drawDropShadow = ( dropShadowColor.getAlpha() > 0 );
        
        int velo = 0;
        String s = String.valueOf( velo );
        Rectangle2D bounds = metrics.getStringBounds( s, texCanvas );
        int x = offsetX + Math.round( velocityNumber1PosX.getFloatValue() * scaleX - (float)( bounds.getWidth() / 2 ) );
        int y = offsetY + Math.round( velocityNumber1PosY.getFloatValue() * scaleY -(float)bounds.getY() - (float)( bounds.getHeight() / 2 ) );
        if ( drawDropShadow )
        {
            texCanvas.setColor( dropShadowColor );
            texCanvas.drawString( s, x + dropShadowOffset, y + dropShadowOffset );
        }
        texCanvas.setColor( velocityNumberFontColor.getColor() );
        texCanvas.drawString( s, x, y, bounds, true, null );
        
        velo = velocity2.getIntValue();
        s = String.valueOf( velo );
        bounds = metrics.getStringBounds( s, texCanvas );
        x = offsetX + Math.round( velocityNumber2PosX.getFloatValue() * scaleX - (float)( bounds.getWidth() / 2 ) );
        y = offsetY + Math.round( Math.round( maxVelocityTopOffset.getFloatValue() * scaleY + maxVelocityTexture.getHeight() - velocity2.getFloatValue() * maxVelocityTexture.getHeight() / maxVelocity.getFloatValue() ) -(float)bounds.getY() - (float)( bounds.getHeight() / 2 ) );
        if ( drawDropShadow )
        {
            texCanvas.setColor( dropShadowColor );
            texCanvas.drawString( s, x + dropShadowOffset, y + dropShadowOffset );
        }
        texCanvas.setColor( velocityNumberFontColor.getColor() );
        texCanvas.drawString( s, x, y, bounds, true, null );
        
        velo = velocity3.getIntValue();
        s = String.valueOf( velo );
        bounds = metrics.getStringBounds( s, texCanvas );
        x = offsetX + Math.round( velocityNumber3PosX.getFloatValue() * scaleX - (float)( bounds.getWidth() / 2 ) );
        y = offsetY + Math.round( Math.round( maxVelocityTopOffset.getFloatValue() * scaleY + maxVelocityTexture.getHeight() - velocity3.getFloatValue() * maxVelocityTexture.getHeight() / maxVelocity.getFloatValue() ) -(float)bounds.getY() - (float)( bounds.getHeight() / 2 ) );
        if ( drawDropShadow )
        {
            texCanvas.setColor( dropShadowColor );
            texCanvas.drawString( s, x + dropShadowOffset, y + dropShadowOffset );
        }
        texCanvas.setColor( velocityNumberFontColor.getColor() );
        texCanvas.drawString( s, x, y, bounds, true, null );
        
        velo = maxVelocity.getIntValue();
        s = String.valueOf( velo );
        bounds = metrics.getStringBounds( s, texCanvas );
        x = offsetX + Math.round( velocityNumber4PosX.getFloatValue() * scaleX - (float)( bounds.getWidth() / 2 ) );
        y = offsetY + Math.round( velocityNumber4PosY.getFloatValue() * scaleY -(float)bounds.getY() - (float)( bounds.getHeight() / 2 ) );
        if ( drawDropShadow )
        {
            texCanvas.setColor( dropShadowColor );
            texCanvas.drawString( s, x + dropShadowOffset, y + dropShadowOffset );
        }
        texCanvas.setColor( velocityNumberFontColor.getColor() );
        texCanvas.drawString( s, x, y, bounds, true, null );
        
        s = String.valueOf( units == SpeedUnits.MIH ? Loc.velocity_units_IMPERIAL : Loc.velocity_units_METRIC );
        bounds = metrics.getStringBounds( s, texCanvas );
        x = offsetX + Math.round( velocityUnitsPosX.getFloatValue() * scaleX - (float)( bounds.getWidth() / 2 ) );
        y = offsetY + Math.round( velocityUnitsPosY.getFloatValue() * scaleY -(float)bounds.getY() - (float)( bounds.getHeight() / 2 ) );
        if ( drawDropShadow )
        {
            texCanvas.setColor( dropShadowColor );
            texCanvas.drawString( s, x + dropShadowOffset, y + dropShadowOffset );
        }
        texCanvas.setColor( velocityNumberFontColor.getColor() );
        texCanvas.drawString( s, x, y, bounds, true, null );
    }
    
    private boolean loadMaxVelocityTexture( SpeedUnits speedUnits, boolean isEditorMode )
    {
        if ( maxVelocityOverlay.isNoImage() )
        {
            maxVelocityTexture = null;
            return ( false );
        }
        
        try
        {
            ImageTemplate it = maxVelocityOverlay.getImage();
            
            if ( it == null )
            {
                maxVelocityTexture = null;
                return ( false );
            }
            
            int w = Math.round( it.getBaseWidth() * getBackground().getScaleX() );
            int h = Math.round( it.getBaseHeight() * getBackground().getScaleY() );
            boolean[] changeInfo = new boolean[ 2 ];
            maxVelocityTexture = it.getScaledTransformableTexture( w, h, maxVelocityTexture, isEditorMode, changeInfo );
            //maxVelocityTexture.setDynamic( true );
            
            if ( changeInfo[1] )
                drawVelocityNumbers( speedUnits, maxVelocityTexture.getTexture(), -Math.round( maxVelocityLeftOffset.getFloatValue() * getBackground().getScaleX() ), -Math.round( maxVelocityTopOffset.getFloatValue() * getBackground().getScaleY() ) );
            
            maxVelocityTexture.setTranslation( maxVelocityLeftOffset.getFloatValue() * getBackground().getScaleX(), maxVelocityTopOffset.getFloatValue() * getBackground().getScaleY() );
            maxVelocityTexture.setLocalZIndex( MAX_VELOCITY_LOCAL_Z_INDEX );
        }
        catch ( Throwable t )
        {
            log( t );
            
            return ( false );
        }
        
        return ( true );
    }
    
    @Override
    protected void initSubTextures( LiveGameData gameData, boolean isEditorMode, int widgetInnerWidth, int widgetInnerHeight, SubTextureCollector collector )
    {
        super.initSubTextures( gameData, isEditorMode, widgetInnerWidth, widgetInnerHeight, collector );
        
        if ( loadMaxVelocityTexture( gameData.getProfileInfo().getSpeedUnits(), isEditorMode ) )
            collector.add( maxVelocityTexture );
        
        int cw = Math.round( controlsWidth.getFloatValue() * getBackground().getScaleX() );
        int ch = Math.round( controlsHeight.getFloatValue() * getBackground().getScaleY() );
        
        loadThrottleTexture( isEditorMode, cw, ch );
        collector.add( texThrottle1 );
        collector.add( texThrottle2 );
        
        loadBrakeTexture( isEditorMode, cw, ch );
        collector.add( texBrake1 );
        collector.add( texBrake2 );
    }
    
    @Override
    protected void drawBackground( LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height, boolean isRoot )
    {
        super.drawBackground( gameData, isEditorMode, texture, offsetX, offsetY, width, height, isRoot );
        
        drawVelocityNumbers( gameData.getProfileInfo().getSpeedUnits(), texture, offsetX, offsetY );
    }
    
    @Override
    protected void drawWidget( Clock clock, boolean needsCompleteRedraw, LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height )
    {
        super.drawWidget( clock, needsCompleteRedraw, gameData, isEditorMode, texture, offsetX, offsetY, width, height );
        
        TelemetryData telemData = gameData.getTelemetryData();
        
        if ( maxVelocityTexture != null )
        {
            float normVelo = Math.min( telemData.getScalarVelocityKmh(), maxVelocity.getFloatValue() ) / maxVelocity.getFloatValue();
            int h = Math.round( maxVelocityTexture.getHeight() * normVelo );
            
            maxVelocityTexture.setClipRect( 0, maxVelocityTexture.getHeight() - h, maxVelocityTexture.getWidth(), h, true );
        }
        
        float uBrake = isEditorMode ? 0.2f : telemData.getUnfilteredBrake();
        float uThrottle = isEditorMode ? 0.4f : telemData.getUnfilteredThrottle();
        
        final int w = texThrottle2.getWidth();
        int brake = (int)( w * uBrake );
        int throttle = (int)( w * uThrottle );
        
        texThrottle2.setClipRect( 0, 0, throttle, texThrottle2.getHeight(), true );
        texBrake2.setClipRect( 0, 0, brake, texBrake2.getHeight(), true );
    }
}
