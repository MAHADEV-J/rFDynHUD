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
package net.ctdp.rfdynhud.widgets.standard.wear;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;

import net.ctdp.rfdynhud.gamedata.LiveGameData;
import net.ctdp.rfdynhud.gamedata.ScoringInfo;
import net.ctdp.rfdynhud.gamedata.TelemetryData;
import net.ctdp.rfdynhud.gamedata.VehiclePhysics;
import net.ctdp.rfdynhud.gamedata.VehiclePhysics.TireCompound.CompoundWheel;
import net.ctdp.rfdynhud.gamedata.VehicleScoringInfo;
import net.ctdp.rfdynhud.gamedata.VehicleSetup;
import net.ctdp.rfdynhud.gamedata.Wheel;
import net.ctdp.rfdynhud.properties.BooleanProperty;
import net.ctdp.rfdynhud.properties.EnumProperty;
import net.ctdp.rfdynhud.properties.FontProperty;
import net.ctdp.rfdynhud.properties.ImagePropertyWithTexture;
import net.ctdp.rfdynhud.properties.PropertiesContainer;
import net.ctdp.rfdynhud.properties.PropertyLoader;
import net.ctdp.rfdynhud.properties.Size;
import net.ctdp.rfdynhud.render.ByteOrderManager;
import net.ctdp.rfdynhud.render.DrawnString;
import net.ctdp.rfdynhud.render.DrawnString.Alignment;
import net.ctdp.rfdynhud.render.DrawnStringFactory;
import net.ctdp.rfdynhud.render.ImageTemplate;
import net.ctdp.rfdynhud.render.TextureImage2D;
import net.ctdp.rfdynhud.util.NumberUtil;
import net.ctdp.rfdynhud.util.PropertyWriter;
import net.ctdp.rfdynhud.util.SubTextureCollector;
import net.ctdp.rfdynhud.valuemanagers.Clock;
import net.ctdp.rfdynhud.values.FloatValue;
import net.ctdp.rfdynhud.widgets.base.widget.Widget;
import net.ctdp.rfdynhud.widgets.standard._util.StandardWidgetSet;

/**
 * The {@link WearWidget} displays wear information for the tires.
 * 
 * @author Marvin Froehlich (CTDP)
 */
public class WearWidget extends Widget
{
    private static enum HundredPercentBase
    {
        SAFE_RANGE,
        GOOD_RANGE,
        BAD_RANGE,
        MAX_RANGE,
        ;
    }
    
    private final FontProperty font2 = new FontProperty( "font2", StandardWidgetSet.SMALLER_FONT.getKey() );
    
    private final Size engineHeight = Size.newLocalSize( this, 0f, true, 10.0f, true );
    
    private final Size tireSize = Size.newLocalSize( this, 10.0f, true, 10.0f, true );
    private final Size brakeSize = Size.newLocalSize( this, 7.0f, true, 20.0f, true );
    
    private int gap = 7;
    
    private final BooleanProperty displayEngine = new BooleanProperty( "displayEngine", true );
    private final BooleanProperty displayTires = new BooleanProperty( "displayTires", true );
    private final BooleanProperty displayBrakes = new BooleanProperty( "displayBrakes", true );
    
    private Boolean displayBrakes2 = null;
    
    private final EnumProperty<HundredPercentBase> hundredPercentBase = new EnumProperty<HundredPercentBase>( "hundredPercentBase", "100% base", HundredPercentBase.SAFE_RANGE );
    
    private final ImagePropertyWithTexture estimationImageName = new ImagePropertyWithTexture( "engineEstimationImage", "estimationImage", "standard/start_finish.png", false, true );
    private final ImagePropertyWithTexture failImageName = new ImagePropertyWithTexture( "engineFailImage", "failImage", "standard/explode.png", false, true );
    
    private final ImagePropertyWithTexture tireIcon = new ImagePropertyWithTexture( "tireIcon", "icon", "standard/tire_small.png", false, true );
    private final ImagePropertyWithTexture brakeDiscIcon = new ImagePropertyWithTexture( "brakeDiscIcon", "icon", "standard/brake_disc_small.png", false, true );
    
    private int maxIconWidth = 0;
    
    private final BooleanProperty displayWearPercent = new BooleanProperty( "displayWearPercent", "display wear %", true );
    private final BooleanProperty displayCompoundName = new BooleanProperty( "displayCompoundName", "display comp. name", true );
    
    private DrawnString engineHeaderString = null;
    private DrawnString engineWearString = null;
    private int engineWearStringMaxWidth = 0;
    private DrawnString engineVarianceString = null;
    
    private final BooleanProperty swapTireWearGripMeaning = new BooleanProperty( "swapTireWearGripMeaning", "swapWearGrip", false );
    
    private DrawnString tiresHeaderString = null;
    private DrawnString tireWearFLString = null;
    private DrawnString tireWearFRString = null;
    private DrawnString tireWearRLString = null;
    private DrawnString tireWearRRString = null;
    private DrawnString tireGripFLString = null;
    private DrawnString tireGripFRString = null;
    private DrawnString tireGripRLString = null;
    private DrawnString tireGripRRString = null;
    
    private DrawnString brakesHeaderString = null;
    private DrawnString brakeWearFLString = null;
    private DrawnString brakeWearFRString = null;
    private DrawnString brakeWearRLString = null;
    private DrawnString brakeWearRRString = null;
    private DrawnString brakeWearVarianceFLString = null;
    private DrawnString brakeWearVarianceFRString = null;
    private DrawnString brakeWearVarianceRLString = null;
    private DrawnString brakeWearVarianceRRString = null;
    
    private final FloatValue engineLifetime = new FloatValue( -1f, 0.001f );
    private final FloatValue brakeDiscWearFL = new FloatValue( -1f, 0.001f );
    private final FloatValue brakeDiscWearFR = new FloatValue( -1f, 0.001f );
    private final FloatValue brakeDiscWearRL = new FloatValue( -1f, 0.001f );
    private final FloatValue brakeDiscWearRR = new FloatValue( -1f, 0.001f );
    
    private static final float gripGood = 0.95f;
    private static final float gripBad = 0.85f;
    
    private static final float wearGood = 0.5f;
    private static final float wearBad = 0.25f;
    
    private static final byte[] colorGood = new byte[ 4 ];
    private static final byte[] colorOk = new byte[ 4 ];
    private static final byte[] colorBad = new byte[ 4 ];
    static
    {
        colorGood[ByteOrderManager.RED] = (byte)0;
        colorGood[ByteOrderManager.GREEN] = (byte)255;
        colorGood[ByteOrderManager.BLUE] = (byte)0;
        colorGood[ByteOrderManager.ALPHA] = (byte)255;
        
        colorOk[ByteOrderManager.RED] = (byte)255;
        colorOk[ByteOrderManager.GREEN] = (byte)255;
        colorOk[ByteOrderManager.BLUE] = (byte)0;
        colorOk[ByteOrderManager.ALPHA] = (byte)255;
        
        colorBad[ByteOrderManager.RED] = (byte)255;
        colorBad[ByteOrderManager.GREEN] = (byte)0;
        colorBad[ByteOrderManager.BLUE] = (byte)0;
        colorBad[ByteOrderManager.ALPHA] = (byte)255;
    }
    
    private static final Color YELLOW2 = new Color( 234, 190, 37 );
    private static final Color GREEN2 = new Color( 152, 234, 13 );
    
    private int[] oldTireWear = { -1, -1, -1, -1 };
    
    private float engineLifetimeAtLapStart = -1f;
    private float engineLifetimeLossPerLap = -1f;
    private boolean testLifetimeAtLapStart = false;
    
    public WearWidget()
    {
        super( StandardWidgetSet.INSTANCE, StandardWidgetSet.WIDGET_PACKAGE_TELEMETRY, 17.8125f, 30.416667f );
    }
    
    @Override
    public void prepareForMenuItem()
    {
        super.prepareForMenuItem();
        
        gap = 1;
        getFontProperty().setFont( "Dialog", Font.PLAIN, 5, false, true );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void saveProperties( PropertyWriter writer ) throws IOException
    {
        super.saveProperties( writer );
        
        writer.writeProperty( font2, "The used (smaller) font." );
        
        writer.writeProperty( displayEngine, "Display the engine part of the Widget?" );
        writer.writeProperty( engineHeight.getHeightProperty( "engineHeight", "height" ), "The height of the engine bar." );
        writer.writeProperty( hundredPercentBase, "The value range to be used as 100% base." );
        writer.writeProperty( displayWearPercent, "Display wear in percentage numbers?" );
        writer.writeProperty( estimationImageName, "Image to display where the engine is expected to explode." );
        writer.writeProperty( failImageName, "Image to display, if the engine WILL fail before the end of the race." );
        
        writer.writeProperty( displayTires, "Display the tire part of the Widget?" );
        writer.writeProperty( displayCompoundName, "Display the tire compound name in the header?" );
        writer.writeProperty( tireIcon, "The image name for the tire icon." );
        writer.writeProperty( tireSize.getWidthProperty( "tireWidth", "width" ), "The width of a tire image." );
        writer.writeProperty( tireSize.getHeightProperty( "tireHeight", "height" ), "The height of a tire image." );
        writer.writeProperty( swapTireWearGripMeaning, "Swap bar and line display for wear and grip?" );
        
        writer.writeProperty( displayBrakes, "Display the brakes of the Widget?" );
        writer.writeProperty( brakeDiscIcon, "The image name for the brake disc icon." );
        writer.writeProperty( brakeSize.getWidthProperty( "brakeWidth", "width" ), "The width of a brake image." );
        writer.writeProperty( brakeSize.getHeightProperty( "brakeHeight", "height" ), "The height of a brake image." );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void loadProperty( PropertyLoader loader )
    {
        super.loadProperty( loader );
        
        if ( loader.loadProperty( font2 ) );
        
        else if ( loader.loadProperty( displayEngine ) );
        else if ( loader.loadProperty( engineHeight.getHeightProperty( "engineHeight", "height" ) ) );
        else if ( loader.loadProperty( hundredPercentBase ) );
        else if ( loader.loadProperty( displayWearPercent ) );
        else if ( loader.loadProperty( estimationImageName ) );
        else if ( loader.loadProperty( failImageName ) );
        
        else if ( loader.loadProperty( displayTires ) );
        else if ( loader.loadProperty( displayCompoundName ) );
        else if ( loader.loadProperty( tireIcon ) );
        else if ( loader.loadProperty( tireSize.getWidthProperty( "tireWidth", "width" ) ) );
        else if ( loader.loadProperty( tireSize.getHeightProperty( "tireHeight", "height" ) ) );
        else if ( loader.loadProperty( swapTireWearGripMeaning ) );
        
        else if ( loader.loadProperty( displayBrakes ) );
        else if ( loader.loadProperty( brakeDiscIcon ) );
        else if ( loader.loadProperty( brakeSize.getWidthProperty( "brakeWidth", "width" ) ) );
        else if ( loader.loadProperty( brakeSize.getHeightProperty( "brakeHeight", "height" ) ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void addFontPropertiesToContainer( PropertiesContainer propsCont, boolean forceAll )
    {
        super.addFontPropertiesToContainer( propsCont, forceAll );
        
        propsCont.addProperty( font2 );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void getProperties( PropertiesContainer propsCont, boolean forceAll )
    {
        super.getProperties( propsCont, forceAll );
        
        propsCont.addGroup( "Engine" );
        
        propsCont.addProperty( displayEngine );
        propsCont.addProperty( engineHeight.getHeightProperty( "engineHeight", "height" ) );
        propsCont.addProperty( hundredPercentBase );
        propsCont.addProperty( displayWearPercent );
        propsCont.addProperty( estimationImageName );
        propsCont.addProperty( failImageName );
        
        propsCont.addGroup( "Tires" );
        
        propsCont.addProperty( displayTires );
        propsCont.addProperty( displayCompoundName );
        propsCont.addProperty( tireIcon );
        propsCont.addProperty( tireSize.getWidthProperty( "tireWidth", "width" ) );
        propsCont.addProperty( tireSize.getHeightProperty( "tireHeight" ) );
        propsCont.addProperty( swapTireWearGripMeaning );
        
        propsCont.addGroup( "Brakes" );
        
        propsCont.addProperty( displayBrakes );
        propsCont.addProperty( brakeDiscIcon );
        propsCont.addProperty( brakeSize.getWidthProperty( "brakeWidth", "width" ) );
        propsCont.addProperty( brakeSize.getHeightProperty( "brakeHeight", "height" ) );
    }
    
    @Override
    public int getNeededData()
    {
        return ( Widget.NEEDED_DATA_SCORING/* | Widget.NEEDED_DATA_SETUP*/ );
    }
    
    public final boolean getDisplayWearPercent()
    {
        return ( displayWearPercent.getBooleanValue() );
    }
    
    private final boolean getDisplayWearPercent_engine()
    {
        return ( getDisplayWearPercent() );
    }
    
    private final boolean getDisplayWearPercent_tires()
    {
        return ( true );
    }
    
    private final boolean getDisplayWearPercent_brakes()
    {
        return ( true );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onCockpitEntered( LiveGameData gameData, boolean isEditorMode )
    {
        super.onCockpitEntered( gameData, isEditorMode );
        
        for ( int i = 0; i < oldTireWear.length; i++ )
            oldTireWear[i] = -1;
        
        engineLifetimeAtLapStart = -1f;
        testLifetimeAtLapStart = false;
        engineLifetimeLossPerLap = -1f;
        
        //forceReinitialization();
        
        displayBrakes2 = null;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onVehicleSetupUpdated( LiveGameData gameData, boolean isEditorMode )
    {
        super.onVehicleSetupUpdated( gameData, isEditorMode );
        
        forceCompleteRedraw( false );
        forceReinitialization();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected Boolean onVehicleControlChanged( VehicleScoringInfo viewedVSI, LiveGameData gameData, boolean isEditorMode )
    {
        super.onVehicleControlChanged( viewedVSI, gameData, isEditorMode );
        
        Boolean oldDisplayBrakes = displayBrakes2;
        displayBrakes2 = displayBrakes.getBooleanValue() && viewedVSI.getVehicleControl().isLocalPlayer();
        if ( displayBrakes2 != oldDisplayBrakes )
        {
            forceReinitialization();
            forceCompleteRedraw( false );
        }
        
        return ( viewedVSI.isPlayer() );
    }
    
    private final float getHundredPercentBaseLifetime( VehiclePhysics.Engine engine, double raceLengthPercentage )
    {
        switch ( this.hundredPercentBase.getEnumValue() )
        {
            case SAFE_RANGE:
            default:
                return ( engine.getSafeLifetimeTotal( raceLengthPercentage ) );
            case GOOD_RANGE:
                return ( engine.getGoodLifetimeTotal( raceLengthPercentage ) );
            case BAD_RANGE:
                return ( engine.getBadLifetimeTotal( raceLengthPercentage ) );
            case MAX_RANGE:
                return ( engine.getMaxLifetimeTotal( raceLengthPercentage ) );
        }
    }
    
    private final float getEngineMinLifetimePercent( VehiclePhysics.Engine engine, double raceLengthPercentage, float hundredPercentBase )
    {
        switch ( this.hundredPercentBase.getEnumValue() )
        {
            case SAFE_RANGE:
            default:
                return ( ( engine.getMaxLifetimeTotal( raceLengthPercentage ) - engine.getSafeLifetimeTotal( raceLengthPercentage ) ) * 100f / hundredPercentBase );
            case GOOD_RANGE:
                return ( ( engine.getMaxLifetimeTotal( raceLengthPercentage ) - engine.getGoodLifetimeTotal( raceLengthPercentage ) ) * 100f / hundredPercentBase );
            case BAD_RANGE:
                return ( ( engine.getMaxLifetimeTotal( raceLengthPercentage ) - engine.getBadLifetimeTotal( raceLengthPercentage ) ) * 100f / hundredPercentBase );
            case MAX_RANGE:
                return ( 0.0f );
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onLapStarted( VehicleScoringInfo vsi, LiveGameData gameData, boolean isEditorMode )
    {
        super.onLapStarted( vsi, gameData, isEditorMode );
        
        VehicleScoringInfo pvsi = gameData.getScoringInfo().getPlayersVehicleScoringInfo();
        
        if ( vsi.equals( pvsi ) && ( vsi.getLapsCompleted() >= 1 ) )
        {
            float engineLifetime = gameData.getTelemetryData().getEngineLifetime();
            
            if ( !testLifetimeAtLapStart )
            {
                engineLifetimeAtLapStart = engineLifetime;
                engineLifetimeLossPerLap = -1f;
                testLifetimeAtLapStart = true;
            }
            else if ( vsi.getLapsRemaining( gameData.getScoringInfo().getEstimatedMaxLaps( vsi ) ) > 0.1f )
            {
                engineLifetimeLossPerLap = engineLifetimeAtLapStart - engineLifetime;
                engineLifetimeAtLapStart = engineLifetime;
            }
        }
    }
    
    private void loadEstimationImage( boolean isEditorMode, int height )
    {
        if ( estimationImageName.isNoImage() )
            return;
        
        ImageTemplate it = estimationImageName.getImage();
        
        int width = Math.round( height * it.getBaseAspect() );
        
        estimationImageName.updateSize( width, height, isEditorMode );
    }
    
    private void loadFailImage( boolean isEditorMode, int height )
    {
        if ( failImageName.isNoImage() )
            return;
        
        ImageTemplate it = failImageName.getImage();
        
        int width = Math.round( height * it.getBaseAspect() );
        
        failImageName.updateSize( width, height, isEditorMode );
    }
    
    private void loadTireImage( boolean isEditorMode, int height )
    {
        if ( tireIcon.isNoImage() )
            return;
        
        ImageTemplate it = tireIcon.getImage();
        
        int width = Math.round( height * it.getBaseAspect() );
        
        tireIcon.updateSize( width, height, isEditorMode );
    }
    
    private void loadBrakeDiscImage( boolean isEditorMode, int height )
    {
        if ( brakeDiscIcon.isNoImage() )
            return;
        
        ImageTemplate it = brakeDiscIcon.getImage();
        
        int width = Math.round( height * it.getBaseAspect() );
        
        brakeDiscIcon.updateSize( width, height, isEditorMode );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void initSubTextures( LiveGameData gameData, boolean isEditorMode, int widgetInnerWidth, int widgetInnerHeight, SubTextureCollector collector )
    {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void initialize( LiveGameData gameData, boolean isEditorMode, DrawnStringFactory dsf, TextureImage2D texture, int width, int height )
    {
        final java.awt.Font font = getFont();
        final boolean fontAntiAliased = isFontAntiAliased();
        final java.awt.Font font2 = this.font2.getFont();
        final boolean font2AntiAliased = this.font2.isAntiAliased();
        final java.awt.Color fontColor = getFontColor();
        
        boolean db = ( displayBrakes2 == null ) ? displayBrakes.getBooleanValue() : displayBrakes2.booleanValue();
        
        maxIconWidth = 0;
        
        if ( displayTires.getBooleanValue() && !tireIcon.isNoImage() )
        {
            final int tireHeight = Math.min( tireSize.getEffectiveHeight(), brakeSize.getEffectiveHeight() );
            
            int tiresHeight = tireHeight * 2 + gap;
            
            loadTireImage( isEditorMode, tiresHeight );
            
            maxIconWidth = Math.max( maxIconWidth, tireIcon.getTexture().getWidth() );
        }
        
        if ( db && !brakeDiscIcon.isNoImage() )
        {
            final int brakeHeight = Math.min( tireSize.getEffectiveHeight(), brakeSize.getEffectiveHeight() );
            
            int brakesHeight = brakeHeight * 2 + gap;
            
            loadBrakeDiscImage( isEditorMode, brakesHeight );
            
            maxIconWidth = Math.max( maxIconWidth, brakeDiscIcon.getTexture().getWidth() );
        }
        
        int left0 = 2;
        //int left = left0 + maxIconWidth;
        int center = maxIconWidth + ( width - maxIconWidth ) / 2;
        int top = -2;
        DrawnString relY = null;
        
        if ( displayEngine.getBooleanValue() )
        {
            engineHeaderString = dsf.newDrawnString( "engineHeaderString", left0, top, Alignment.LEFT, false, font, fontAntiAliased, fontColor, Loc.engine_header_prefix + ": ", gameData.getVehicleInfo().getEngineName() );
            if ( getDisplayWearPercent_engine() )
            {
                engineWearString = dsf.newDrawnString( "engineWearString", null, engineHeaderString, width, 2, Alignment.RIGHT, false, font, fontAntiAliased, fontColor, null, "%" );
                engineVarianceString = dsf.newDrawnString( "engineVarianceString", null, engineWearString, width, 2, Alignment.RIGHT, false, font2, font2AntiAliased, fontColor, "(", "%)" );
                final double raceLengthPercentage = gameData.getScoringInfo().getRaceLengthPercentage();
                float minLifetime = Math.round( getEngineMinLifetimePercent( gameData.getPhysics().getEngine(), raceLengthPercentage, getHundredPercentBaseLifetime( gameData.getPhysics().getEngine(), raceLengthPercentage ) ) * 10f ) / 10f;
                if ( gameData.getPhysics().getEngine().hasLifetimeVariance() )
                    engineWearStringMaxWidth = Math.max( engineWearString.getWidth( "-" + NumberUtil.formatFloat( minLifetime, 1, true ) + "%" ), engineWearString.getWidth( "100%" ) );
                else
                    engineWearStringMaxWidth = engineWearString.getWidth( "100.0%" );
            }
            else
            {
                engineWearString = null;
                engineVarianceString = null;
            }
            
            relY = engineHeaderString;
            top = engineHeight.getEffectiveHeight() + 10;
            
            loadEstimationImage( isEditorMode, engineHeight.getEffectiveHeight() );
            loadFailImage( isEditorMode, engineHeight.getEffectiveHeight() );
        }
        
        if ( displayTires.getBooleanValue() )
        {
            final int tireWidth = tireSize.getEffectiveWidth();
            final int tireHeight = tireSize.getEffectiveHeight();
            
            tiresHeaderString = dsf.newDrawnString( "tiresHeaderString", null, relY, left0, top, Alignment.LEFT, false, font, fontAntiAliased, fontColor, Loc.tires_header_prefix + ":", null );
            
            final boolean dwpt = getDisplayWearPercent_tires();
            {
                tireWearFLString = dsf.newDrawnStringIf( dwpt, "tireWearFLString", null, tiresHeaderString, center - gap - tireWidth, 3, Alignment.RIGHT, false, font, fontAntiAliased, fontColor, null, "%" );
                tireWearFRString = dsf.newDrawnStringIf( dwpt, "tireWearFRString", null, tiresHeaderString, center + gap + tireWidth, 3, Alignment.LEFT, false, font, fontAntiAliased, fontColor, null, "%" );
                tireWearRLString = dsf.newDrawnStringIf( dwpt, "tireWearRLString", null, tiresHeaderString, center - gap - tireWidth, 3 + tireHeight + gap, Alignment.RIGHT, false, font, fontAntiAliased, fontColor, null, "%" );
                tireWearRRString = dsf.newDrawnStringIf( dwpt, "tireWearRRString", null, tiresHeaderString, center + gap + tireWidth, 3 + tireHeight + gap, Alignment.LEFT, false, font, fontAntiAliased, fontColor, null, "%" );
                tireGripFLString = dsf.newDrawnStringIf( dwpt, "tireGripFLString", null, tiresHeaderString, center - gap - tireWidth, 3 + tireHeight - 2, Alignment.RIGHT, true, font2, font2AntiAliased, fontColor, "(", "%)" );
                tireGripFRString = dsf.newDrawnStringIf( dwpt, "tireGripFRString", null, tiresHeaderString, center + gap + tireWidth, 3 + tireHeight - 2, Alignment.LEFT, true, font2, font2AntiAliased, fontColor, "(", "%)" );
                tireGripRLString = dsf.newDrawnStringIf( dwpt, "tireGripRLString", null, tiresHeaderString, center - gap - tireWidth, 3 + tireHeight - 2 + tireHeight + gap, Alignment.RIGHT, true, font2, font2AntiAliased, fontColor, "(", "%)" );
                tireGripRRString = dsf.newDrawnStringIf( dwpt, "tireGripRRString", null, tiresHeaderString, center + gap + tireWidth, 3 + tireHeight - 2 + tireHeight + gap, Alignment.LEFT, true, font2, font2AntiAliased, fontColor, "(", "%)" );
            }
            
            relY = tiresHeaderString;
            int tiresHeight = tireHeight * 2 + 15;
            top = tiresHeight;
        }
        
        if ( db )
        {
            final int tireWidth = tireSize.getEffectiveWidth();
            final int brakeWidth = brakeSize.getEffectiveWidth();
            final int brakeHeight = brakeSize.getEffectiveHeight();
            
            brakesHeaderString = dsf.newDrawnString( "brakesHeaderString", null, relY, left0, top, Alignment.LEFT, false, font, fontAntiAliased, fontColor );
            
            final boolean dwpb = getDisplayWearPercent_brakes();
            {
                int imgWidth = displayTires.getBooleanValue() && db ? Math.max( tireWidth, brakeWidth ) : ( displayTires.getBooleanValue() ? tireWidth : brakeWidth );
                
                brakeWearFLString = dsf.newDrawnStringIf( dwpb, "brakeWearFLString", null, brakesHeaderString, center - gap - imgWidth, 2, Alignment.RIGHT, false, font, fontAntiAliased, fontColor, null, "%" );
                brakeWearFRString = dsf.newDrawnStringIf( dwpb, "brakeWearFRString", null, brakesHeaderString, center + gap + imgWidth, 2, Alignment.LEFT, false, font, fontAntiAliased, fontColor, null, "%" );
                brakeWearRLString = dsf.newDrawnStringIf( dwpb, "brakeWearRLString", null, brakesHeaderString, center - gap - imgWidth, 2 + brakeHeight + gap, Alignment.RIGHT, false, font, fontAntiAliased, fontColor, null, "%" );
                brakeWearRRString = dsf.newDrawnStringIf( dwpb, "brakeWearRRString", null, brakesHeaderString, center + gap + imgWidth, 2 + brakeHeight + gap, Alignment.LEFT, false, font, fontAntiAliased, fontColor, null, "%" );
                brakeWearVarianceFLString = dsf.newDrawnStringIf( dwpb, "brakeWearVarianceFLString", null, brakeWearFLString, center - gap - imgWidth, 2, Alignment.RIGHT, false, font2, font2AntiAliased, fontColor, "(", "%)" );
                brakeWearVarianceFRString = dsf.newDrawnStringIf( dwpb, "brakeWearVarianceFRString", null, brakeWearFRString, center + gap + imgWidth, 2, Alignment.LEFT, false, font2, font2AntiAliased, fontColor, "(", "%)" );
                brakeWearVarianceRLString = dsf.newDrawnStringIf( dwpb, "brakeWearVarianceRLString", null, brakeWearRLString, center - gap - imgWidth, 2, Alignment.RIGHT, false, font2, font2AntiAliased, fontColor, "(", "%)" );
                brakeWearVarianceRRString = dsf.newDrawnStringIf( dwpb, "brakeWearVarianceRRString", null, brakeWearRRString, center + gap + imgWidth, 2, Alignment.LEFT, false, font2, font2AntiAliased, fontColor, "(", "%)" );
            }
        }
    }
    
    private static void interpolateColor( byte[] color0, byte[] color1, float alpha, byte[] result )
    {
        final float beta = 1f - alpha;
        
        result[ByteOrderManager.RED] = (byte)( ( color0[ByteOrderManager.RED] & 0xFF ) * beta + ( color1[ByteOrderManager.RED] & 0xFF ) * alpha );
        result[ByteOrderManager.GREEN] = (byte)( ( color0[ByteOrderManager.GREEN] & 0xFF ) * beta + ( color1[ByteOrderManager.GREEN] & 0xFF ) * alpha );
        result[ByteOrderManager.BLUE] = (byte)( ( color0[ByteOrderManager.BLUE] & 0xFF ) * beta + ( color1[ByteOrderManager.BLUE] & 0xFF ) * alpha );
    }
    
    private void drawEngine( ScoringInfo scoringInfo, boolean isEditorMode, float lifetime, double raceLengthMultiplier, VehiclePhysics.Engine engine, TextureImage2D texture, final int x, final int y, final int width )
    {
        final int h = engineHeight.getEffectiveHeight();
        
        texture.getTextureCanvas().pushClip( x, y, width, h, true );
        
        try
        {
            final int lowerSafeLifetime = engine.getLowerSafeLifetimeValue( raceLengthMultiplier );
            final int lowerGoodLifetime = engine.getLowerGoodLifetimeValue( raceLengthMultiplier );
            final int lowerBadLifetime = engine.getLowerBadLifetimeValue( raceLengthMultiplier );
            final int minLifetime = engine.getMinLifetimeValue( raceLengthMultiplier );
            final int safeLifetimeTotal = engine.getSafeLifetimeTotal( raceLengthMultiplier );
            final int goodLifetimeTotal = engine.getGoodLifetimeTotal( raceLengthMultiplier );
            final int badLifetimeTotal = engine.getBadLifetimeTotal( raceLengthMultiplier );
            final int maxLifetimeTotal = engine.getMaxLifetimeTotal( raceLengthMultiplier );
            final boolean hasVariance = engine.hasLifetimeVariance();
            
            if ( hasVariance )
            {
                int x0 = x;
                int x1 = -1;
                int x2 = -1;
                int x3 = -1;
                
                if ( lifetime >= lowerBadLifetime )
                {
                    int w2 = ( lowerBadLifetime - minLifetime ) * width / maxLifetimeTotal;
                    texture.clear( Color.RED, x0, y, w2, h, false, null );
                    x0 += w2;
                }
                else
                {
                    int w2 = (int)( ( lifetime - minLifetime ) * width / maxLifetimeTotal );
                    texture.clear( Color.RED, x0, y, w2, h, false, null );
                    x0 += w2;
                    x1 = x + ( maxLifetimeTotal - badLifetimeTotal ) * width / maxLifetimeTotal;
                }
                
                if ( lifetime >= lowerGoodLifetime )
                {
                    int w2 = ( lowerGoodLifetime - lowerBadLifetime ) * width / maxLifetimeTotal;
                    texture.clear( YELLOW2, x0, y, w2, h, false, null );
                    x0 += w2;
                }
                else
                {
                    if ( lifetime >= lowerBadLifetime )
                    {
                        int w2 = (int)( ( lifetime - lowerBadLifetime ) * width / maxLifetimeTotal );
                        texture.clear( YELLOW2, x0, y, w2, h, false, null );
                        x0 += w2;
                    }
                    
                    x2 = x + ( maxLifetimeTotal - goodLifetimeTotal ) * width / maxLifetimeTotal;
                }
                
                if ( lifetime >= lowerSafeLifetime )
                {
                    int w2 = ( lowerSafeLifetime - lowerGoodLifetime ) * width / maxLifetimeTotal;
                    texture.clear( GREEN2, x0, y, w2, h, false, null );
                    x0 += w2;
                    
                    int w3 = (int)( ( lifetime - lowerSafeLifetime ) * width / maxLifetimeTotal );
                    texture.clear( Color.GREEN, x0, y, w3, h, false, null );
                    x0 += w3;
                }
                else
                {
                    if ( lifetime >= lowerGoodLifetime )
                    {
                        int w2 = (int)( ( lifetime - lowerGoodLifetime ) * width / maxLifetimeTotal );
                        texture.clear( GREEN2, x0, y, w2, h, false, null );
                        x0 += w2;
                    }
                    
                    x3 = x + ( maxLifetimeTotal - safeLifetimeTotal ) * width / maxLifetimeTotal;
                }
                
                int w_ = width - x0 + x;
                if ( w_ > 0 )
                {
                    texture.clear( Color.BLACK, x0, y, w_, h, false, null );
                }
                
                if ( x1 > 0 )
                {
                    texture.getTextureCanvas().setColor( Color.RED );
                    texture.getTextureCanvas().drawLine( x1, y, x1, y + h - 1 );
                }
                
                if ( x2 > 0 )
                {
                    texture.getTextureCanvas().setColor( YELLOW2 );
                    texture.getTextureCanvas().drawLine( x2, y, x2, y + h - 1 );
                }
                
                if ( x3 > 0 )
                {
                    texture.getTextureCanvas().setColor( GREEN2 );
                    texture.getTextureCanvas().drawLine( x3, y, x3, y + h - 1 );
                }
            }
            else
            {
                int w2 = (int)( lifetime * width / maxLifetimeTotal );
                texture.clear( Color.GREEN, x, y, w2, h, false, null );
                
                int w3 = width - w2;
                if ( w3 > 0 )
                    texture.clear( Color.BLACK, x + w2, y, w3, h, false, null );
            }
            
            if ( estimationImageName.hasTexture() || failImageName.hasTexture() )
            {
                if ( isEditorMode )
                {
                    if ( estimationImageName.hasTexture() )
                        texture.drawImage( estimationImageName.getTexture(), x + 10, y, false, null );
                    else
                        texture.drawImage( failImageName.getTexture(), x + 10, y, false, null );
                }
                else if ( scoringInfo.getSessionType().isRace() && ( engineLifetimeLossPerLap > 0f ) )
                {
                    final int maxLaps = scoringInfo.getEstimatedMaxLaps( scoringInfo.getPlayersVehicleScoringInfo() );
                    if ( maxLaps > 0 )
                    {
                        int lapsRemaining = Math.max( 0, (int)scoringInfo.getPlayersVehicleScoringInfo().getLapsRemaining( maxLaps ) );
                        int x2 = (int)( ( engineLifetimeAtLapStart - ( engineLifetimeLossPerLap * ( lapsRemaining + 1 ) ) + maxLifetimeTotal - safeLifetimeTotal ) * width / maxLifetimeTotal );
                        
                        if ( ( x2 <= 0 ) && failImageName.hasTexture() )
                            texture.drawImage( failImageName.getTexture(), x, y, false, null );
                        else if ( estimationImageName.hasTexture() )
                            texture.drawImage( estimationImageName.getTexture(), x + x2 - ( estimationImageName.getTexture().getWidth() / 2 ), y, false, null );
                    }
                }
            }
            
            texture.markDirty( x, y, width, h, null );
        }
        finally
        {
            texture.getTextureCanvas().popClip();
        }
    }
    
    private void drawTire( float wear, float grip, CompoundWheel compoundWheel, TextureImage2D texture, int x, int y )
    {
        int w = tireSize.getEffectiveWidth();
        int h = tireSize.getEffectiveHeight();
        
        float barValue;
        float barGood;
        float barBad;
        float lineValue;
        
        if ( swapTireWearGripMeaning.getBooleanValue() )
        {
            barValue = wear;
            barGood = wearGood;
            barBad = wearBad;
            
            final float minGrip = compoundWheel.getMinGrip();
            float barRange = 1.0f - minGrip;
            lineValue = ( grip - minGrip ) / barRange;
        }
        else
        {
            final float minGrip = compoundWheel.getMinGrip();
            float barRange = 1.0f - minGrip;
            barValue = ( grip - minGrip ) / barRange;
            barGood = ( gripGood - minGrip ) / barRange;
            barBad = ( gripBad - minGrip ) / barRange;
            
            lineValue = wear;
        }
        
        byte[] color = new byte[4];
        color[ByteOrderManager.ALPHA] = (byte)255;
        if ( barValue <= barBad )
        {
            System.arraycopy( colorBad, 0, color, 0, 3 );
        }
        else if ( barValue < barGood )
        {
            float alpha = ( barValue - barBad ) / ( barGood - barBad );
            interpolateColor( colorBad, colorOk, alpha, color );
        }
        else
        {
            System.arraycopy( colorGood, 0, color, 0, 3 );
        }
        
        Color awtColor = new Color( color[ByteOrderManager.RED] & 0xFF, color[ByteOrderManager.GREEN] & 0xFF, color[ByteOrderManager.BLUE] & 0xFF );
        
        int barHeight = Math.min( (int)( h * barValue ), h );
        
        if ( barValue > 0.0f )
        {
            texture.clear( Color.BLACK, x, y, w, h - barHeight, false, null );
        }
        
        texture.clear( awtColor, x, y + h - barHeight, w, barHeight, false, null );
        
        byte[] pixels = new byte[ w * 4 ];
        for ( int i = 0; i < w; i++ )
        {
            System.arraycopy( colorBad, 0, pixels, i * 4, 4 );
        }
        
        texture.clearPixelLine( pixels, x, y + h - (int)( h * lineValue ), w, false, null );
        
        texture.markDirty( x, y, w, h, null );
    }
    
    private void drawBrake( float discThickness, Wheel wheel, VehiclePhysics.Brakes.WheelBrake brake, VehicleSetup setup, TextureImage2D texture, int x, int y )
    {
        int w = brakeSize.getEffectiveWidth();
        int h = brakeSize.getEffectiveHeight();
        
        final float minDiscFailure = brake.getMinDiscFailure();
        final float maxDiscFailure = brake.getMaxDiscFailure();
        final float startDiscThickness = setup.getWheelAndTire( wheel ).getBrakeDiscThickness();
        final float variance = brake.getDiscFailureVarianceRange();
        discThickness = Math.max( minDiscFailure, discThickness );
        final float positiveRange = startDiscThickness - maxDiscFailure;
        final float maxRange = startDiscThickness - minDiscFailure;
        final boolean hasVariance = brake.hasDiscFailureVariance();
        
        if ( hasVariance )
        {
            if ( discThickness >= maxDiscFailure )
            {
                int h2 = (int)( variance * h / maxRange );
                texture.clear( Color.RED, x, y + h - h2, w, h2, false, null );
                
                int h3 = (int)( ( discThickness - maxDiscFailure ) * ( h - h2 ) / positiveRange );
                texture.clear( Color.GREEN, x, y + h - h2 - h3, w, h3, false, null );
                
                int h4 = h - h3 - h2;
                if ( h4 > 0 )
                    texture.clear( Color.BLACK, x, y, w, h4, false, null );
            }
            else
            {
                int h2 = (int)( ( discThickness - minDiscFailure ) * h / maxRange );
                texture.clear( Color.RED, x, y + h - h2, w, h2, false, null );
                
                int y2 = h - (int)( variance * h / maxRange );
                
                int h3 = h - h2;
                if ( h3 > 0 )
                    texture.clear( Color.BLACK, x, y, w, h3, false, null );
                
                texture.getTextureCanvas().setColor( Color.RED );
                texture.getTextureCanvas().drawLine( x, y + y2, x + w - 1, y + y2 );
            }
        }
        else
        {
            int h2 = (int)( ( discThickness - minDiscFailure ) * h / maxRange );
            texture.clear( Color.GREEN, x, y + h - h2, w, h2, false, null );
            
            int h3 = h - h2;
            if ( h3 > 0 )
                texture.clear( Color.BLACK, x, y, w, h3, false, null );
        }
        
        texture.markDirty( x, y, w, h, null );
    }
    
    @Override
    protected void drawWidget( Clock clock, boolean needsCompleteRedraw, LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height )
    {
        final boolean c = clock.c();
        
        final VehiclePhysics physics = gameData.getPhysics();
        final TelemetryData telemData = gameData.getTelemetryData();
        final VehicleSetup setup = gameData.getSetup();
        
        //final int center = width / 2;
        
        boolean db = ( displayBrakes2 == null ) ? displayBrakes.getBooleanValue() : displayBrakes2.booleanValue();
        
        if ( needsCompleteRedraw )
        {
            if ( displayEngine.getBooleanValue() )
                engineHeaderString.draw( offsetX, offsetY, "", texture );
            if ( displayTires.getBooleanValue() )
            {
                // TODO: Try to find a way to detect tire compound change on pit stop!
                if ( displayCompoundName.getBooleanValue() )
                    tiresHeaderString.draw( offsetX, offsetY, " " + setup.getGeneral().getFrontTireCompound().getName(), texture );
                else
                    tiresHeaderString.draw( offsetX, offsetY, "", texture );
            }
            if ( db )
                brakesHeaderString.draw( offsetX, offsetY, Loc.brakes_header_prefix + ":", texture );
        }
        
        if ( displayEngine.getBooleanValue() )
        {
            final double raceLengthPercentage = gameData.getScoringInfo().getRaceLengthPercentage();
            float lifetime = gameData.getTelemetryData().getEngineLifetime();
            final int hundredPercentBase;
            switch ( this.hundredPercentBase.getEnumValue() )
            {
                case SAFE_RANGE:
                default:
                    hundredPercentBase = physics.getEngine().getSafeLifetimeTotal( raceLengthPercentage );
                    engineLifetime.update( lifetime / hundredPercentBase );
                    break;
                case GOOD_RANGE:
                    hundredPercentBase = physics.getEngine().getGoodLifetimeTotal( raceLengthPercentage );
                    engineLifetime.update( ( lifetime + hundredPercentBase - physics.getEngine().getSafeLifetimeTotal( raceLengthPercentage ) ) / hundredPercentBase );
                    break;
                case BAD_RANGE:
                    hundredPercentBase = physics.getEngine().getBadLifetimeTotal( raceLengthPercentage );
                    engineLifetime.update( ( lifetime + hundredPercentBase - physics.getEngine().getSafeLifetimeTotal( raceLengthPercentage ) ) / hundredPercentBase );
                    break;
                case MAX_RANGE:
                    hundredPercentBase = physics.getEngine().getMaxLifetimeTotal( raceLengthPercentage );
                    engineLifetime.update( ( lifetime + hundredPercentBase - physics.getEngine().getSafeLifetimeTotal( raceLengthPercentage ) ) / hundredPercentBase );
                    break;
            }
            lifetime = Math.max( -physics.getEngine().getLifetimeVarianceRange( raceLengthPercentage ), lifetime );
            
            if ( needsCompleteRedraw || engineLifetime.hasChanged() )
            {
                int engineWidth;
                if ( getDisplayWearPercent_engine() )
                {
                    boolean hasVariance = physics.getEngine().hasLifetimeVariance();
                    float lt = engineLifetime.getValue() * 100f;
                    if ( !hasVariance )
                        lt = Math.max( 0f, lt );
                    engineWearString.draw( offsetX, offsetY, NumberUtil.formatFloat( lt, 1, true ), texture );
                    if ( hasVariance )
                    {
                        final float variancePercent = getEngineMinLifetimePercent( physics.getEngine(), raceLengthPercentage, hundredPercentBase );
                        engineVarianceString.draw( offsetX, offsetY, NumberUtil.formatFloat( -variancePercent, 1, true ), texture );
                    }
                    
                    //engineWidth = width - engineHeaderString.getAbsX() - 5 - Math.max( engineVarianceString.getLastWidth(), engineWearString.getLastWidth() );
                    engineWidth = width - engineHeaderString.getAbsX() - 0 - engineWearStringMaxWidth;
                }
                else
                {
                    engineWidth = width - ( engineHeaderString.getAbsX() * 2 );
                }
                
                drawEngine( gameData.getScoringInfo(), isEditorMode, lifetime, raceLengthPercentage, physics.getEngine(), texture, offsetX + engineHeaderString.getAbsX(), offsetY + engineHeaderString.getAbsY() + engineHeaderString.getMaxHeight( false ) + 3, engineWidth );
            }
        }
        
        if ( displayTires.getBooleanValue() )
        {
            final int tireWidth = tireSize.getEffectiveWidth();
            
            int top = 0, left = 0;
            float tireWearFLf = telemData.getTireWear( Wheel.FRONT_LEFT );
            int tireWearFL = Math.round( tireWearFLf * 100f );
            if ( needsCompleteRedraw || ( c && ( tireWearFL != oldTireWear[0] ) ) )
            {
                oldTireWear[0] = tireWearFL;
                
                CompoundWheel wheel = setup.getGeneral().getFrontTireCompound().getWheel( Wheel.FRONT_LEFT );
                
                float gripf = wheel.getWearGripFactor( tireWearFLf );
                int grip = Math.round( gripf * 100f );
                
                top = 0; left = 0;
                if ( getDisplayWearPercent_tires() )
                {
                    String string = String.valueOf( tireWearFL );
                    tireWearFLString.draw( offsetX, offsetY, string, texture );
                    string = String.valueOf( grip );
                    tireGripFLString.draw( offsetX, offsetY, string, texture );
                    
                    left = tireWearFLString.getAbsX() + 3;
                    top = tireWearFLString.getAbsY() + 2;
                }
                
                drawTire( tireWearFLf, gripf, wheel, texture, offsetX + left, offsetY + top );
            }
            
            if ( needsCompleteRedraw && tireIcon.hasTexture() )
            {
                top += ( tireSize.getEffectiveHeight() * 2 + gap - tireIcon.getTexture().getHeight() ) / 2;
                texture.drawImage( tireIcon.getTexture(), offsetX, offsetY + top, true, null );
            }
            
            float tireWearFRf = telemData.getTireWear( Wheel.FRONT_RIGHT );
            int tireWearFR = Math.round( tireWearFRf * 100f );
            if ( needsCompleteRedraw || ( c && ( tireWearFR != oldTireWear[1] ) ) )
            {
                oldTireWear[1] = tireWearFR;
                
                CompoundWheel wheel = setup.getGeneral().getFrontTireCompound().getWheel( Wheel.FRONT_RIGHT );
                
                float gripf = wheel.getWearGripFactor( tireWearFRf );
                int grip = Math.round( gripf * 100f );
                
                top = 0; left = 0;
                if ( getDisplayWearPercent_tires() )
                {
                    String string = String.valueOf( tireWearFR );
                    tireWearFRString.draw( offsetX, offsetY, string, texture );
                    string = String.valueOf( grip );
                    tireGripFRString.draw( offsetX, offsetY, string, texture );
                    
                    left = tireWearFRString.getAbsX() - tireWidth - 3;
                    top = tireWearFRString.getAbsY() + 2;
                }
                
                drawTire( tireWearFRf, gripf, wheel, texture, offsetX + left, offsetY + top );
            }
            
            float tireWearRLf = telemData.getTireWear( Wheel.REAR_LEFT );
            int tireWearRL = Math.round( tireWearRLf * 100f );
            if ( needsCompleteRedraw || ( c && ( tireWearRL != oldTireWear[2] ) ) )
            {
                oldTireWear[2] = tireWearRL;
                
                CompoundWheel wheel = setup.getGeneral().getRearTireCompound().getWheel( Wheel.REAR_LEFT );
                
                float gripf = wheel.getWearGripFactor( tireWearRLf );
                int grip = Math.round( gripf * 100f );
                
                top = 0; left = 0;
                if ( getDisplayWearPercent_tires() )
                {
                    String string = String.valueOf( tireWearRL );
                    tireWearRLString.draw( offsetX, offsetY, string, texture );
                    string = String.valueOf( grip );
                    tireGripRLString.draw( offsetX, offsetY, string, texture );
                    
                    left = tireWearRLString.getAbsX() + 3;
                    top = tireWearRLString.getAbsY() + 2;
                }
                
                drawTire( tireWearRLf, gripf, wheel, texture, offsetX + left, offsetY + top );
            }
            
            float tireWearRRf = telemData.getTireWear( Wheel.REAR_RIGHT );
            int tireWearRR = Math.round( tireWearRRf * 100f );
            if ( needsCompleteRedraw || ( c && ( tireWearRR != oldTireWear[3] ) ) )
            {
                oldTireWear[3] = tireWearRR;
                
                CompoundWheel wheel = setup.getGeneral().getRearTireCompound().getWheel( Wheel.REAR_RIGHT );
                
                float gripf = wheel.getWearGripFactor( tireWearRRf );
                int grip = Math.round( gripf * 100f );
                
                top = 0; left = 0;
                if ( getDisplayWearPercent_tires() )
                {
                    String string = String.valueOf( tireWearRR );
                    tireWearRRString.draw( offsetX, offsetY, string, texture );
                    string = String.valueOf( grip );
                    tireGripRRString.draw( offsetX, offsetY, string, texture );
                    
                    left = tireWearRRString.getAbsX() - tireWidth - 3;
                    top = tireWearRRString.getAbsY() + 2;
                }
                
                drawTire( tireWearRRf, gripf, wheel, texture, offsetX + left, offsetY + top );
            }
        }
        
        if ( db )
        {
            final int brakeWidth = brakeSize.getEffectiveWidth();
            
            int left = 0, top = 0;
            Wheel wheel = Wheel.FRONT_LEFT;
            VehiclePhysics.Brakes.WheelBrake brake = physics.getBrakes().getBrake( wheel );
            float brakeDiscThickness = telemData.getBrakeDiscThickness( wheel );
            brakeDiscWearFL.update( ( brakeDiscThickness - brake.getMaxDiscFailure() ) / ( setup.getWheelAndTire( wheel ).getBrakeDiscThickness() - brake.getMaxDiscFailure() ) );
            
            if ( needsCompleteRedraw || ( c && brakeDiscWearFL.hasChanged( false ) ) )
            {
                brakeDiscWearFL.setUnchanged();
                
                left = 0; top = 0;
                if ( getDisplayWearPercent_brakes() )
                {
                    String string = NumberUtil.formatFloat( brakeDiscWearFL.getValue() * 100f, 1, true );
                    brakeWearFLString.draw( offsetX, offsetY, string, texture );
                    if ( brake.hasDiscFailureVariance() )
                    {
                        float variancePercent = brake.getDiscFailureVarianceRange() * 100f / ( setup.getWheelAndTire( wheel ).getBrakeDiscThickness() - brake.getMaxDiscFailure() );
                        brakeWearVarianceFLString.draw( offsetX, offsetY, NumberUtil.formatFloat( -variancePercent, 1, true ), texture );
                    }
                    
                    left = brakeWearFLString.getAbsX() + 3;
                    top = brakeWearFLString.getAbsY();
                }
                
                drawBrake( brakeDiscThickness, wheel, brake, setup, texture, offsetX + left, offsetY + top );
            }
            
            if ( needsCompleteRedraw && brakeDiscIcon.hasTexture() )
            {
                top += ( brakeSize.getEffectiveHeight() * 2 + gap - brakeDiscIcon.getTexture().getHeight() ) / 2;
                texture.drawImage( brakeDiscIcon.getTexture(), offsetX, offsetY + top, true, null );
            }
            
            wheel = Wheel.FRONT_RIGHT;
            brake = physics.getBrakes().getBrake( wheel );
            brakeDiscThickness = telemData.getBrakeDiscThickness( wheel );
            brakeDiscWearFR.update( ( brakeDiscThickness - brake.getMaxDiscFailure() ) / ( setup.getWheelAndTire( wheel ).getBrakeDiscThickness() - brake.getMaxDiscFailure() ) );
            
            if ( needsCompleteRedraw || ( c && brakeDiscWearFR.hasChanged( false ) ) )
            {
                brakeDiscWearFR.setUnchanged();
                
                left = 0; top = 0;
                if ( getDisplayWearPercent_brakes() )
                {
                    String string = NumberUtil.formatFloat( brakeDiscWearFR.getValue() * 100f, 1, true );
                    brakeWearFRString.draw( offsetX, offsetY, string, texture );
                    if ( brake.hasDiscFailureVariance() )
                    {
                        float variancePercent = brake.getDiscFailureVarianceRange() * 100f / ( setup.getWheelAndTire( wheel ).getBrakeDiscThickness() - brake.getMaxDiscFailure() );
                        brakeWearVarianceFRString.draw( offsetX, offsetY, NumberUtil.formatFloat( -variancePercent, 1, true ), texture );
                    }
                    left = brakeWearFRString.getAbsX() - brakeWidth - 3;
                    top = brakeWearFRString.getAbsY();
                }
                
                drawBrake( brakeDiscThickness, wheel, brake, setup, texture, offsetX + left, offsetY + top );
            }
            
            wheel = Wheel.REAR_LEFT;
            brake = physics.getBrakes().getBrake( wheel );
            brakeDiscThickness = telemData.getBrakeDiscThickness( wheel );
            brakeDiscWearRL.update( ( brakeDiscThickness - brake.getMaxDiscFailure() ) / ( setup.getWheelAndTire( wheel ).getBrakeDiscThickness() - brake.getMaxDiscFailure() ) );
            
            if ( needsCompleteRedraw || ( c && brakeDiscWearRL.hasChanged( false ) ) )
            {
                brakeDiscWearRL.setUnchanged();
                
                left = 0; top = 0;
                if ( getDisplayWearPercent_brakes() )
                {
                    String string = NumberUtil.formatFloat( brakeDiscWearRL.getValue() * 100f, 1, true );
                    brakeWearRLString.draw( offsetX, offsetY, string, texture );
                    if ( brake.hasDiscFailureVariance() )
                    {
                        float variancePercent = brake.getDiscFailureVarianceRange() * 100f / ( setup.getWheelAndTire( wheel ).getBrakeDiscThickness() - brake.getMaxDiscFailure() );
                        brakeWearVarianceRLString.draw( offsetX, offsetY, NumberUtil.formatFloat( -variancePercent, 1, true ), texture );
                    }
                    
                    left = brakeWearRLString.getAbsX() + 3;
                    top = brakeWearRLString.getAbsY();
                }
                
                drawBrake( brakeDiscThickness, wheel, brake, setup, texture, offsetX + left, offsetY + top );
            }
            
            wheel = Wheel.REAR_RIGHT;
            brake = physics.getBrakes().getBrake( wheel );
            brakeDiscThickness = telemData.getBrakeDiscThickness( wheel );
            brakeDiscWearRR.update( ( brakeDiscThickness - brake.getMaxDiscFailure() ) / ( setup.getWheelAndTire( wheel ).getBrakeDiscThickness() - brake.getMaxDiscFailure() ) );
            
            if ( needsCompleteRedraw || ( c && brakeDiscWearRR.hasChanged( false ) ) )
            {
                brakeDiscWearRR.setUnchanged();
                
                left = 0; top = 0;
                if ( getDisplayWearPercent_brakes() )
                {
                    String string = NumberUtil.formatFloat( brakeDiscWearRR.getValue() * 100f, 1, true );
                    brakeWearRRString.draw( offsetX, offsetY, string, texture );
                    if ( brake.hasDiscFailureVariance() )
                    {
                        float variancePercent = brake.getDiscFailureVarianceRange() * 100f / ( setup.getWheelAndTire( wheel ).getBrakeDiscThickness() - brake.getMaxDiscFailure() );
                        brakeWearVarianceRRString.draw( offsetX, offsetY, NumberUtil.formatFloat( -variancePercent, 1, true ), texture );
                    }
                    
                    left = brakeWearRRString.getAbsX() - brakeWidth - 3;
                    top = brakeWearRRString.getAbsY();
                }
                
                drawBrake( brakeDiscThickness, wheel, brake, setup, texture, offsetX + left, offsetY + top );
            }
        }
    }
}
