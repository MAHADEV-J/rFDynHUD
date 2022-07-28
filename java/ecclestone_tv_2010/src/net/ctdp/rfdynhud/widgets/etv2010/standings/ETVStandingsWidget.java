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
package net.ctdp.rfdynhud.widgets.etv2010.standings;

import java.awt.FontMetrics;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import net.ctdp.rfdynhud.gamedata.Laptime;
import net.ctdp.rfdynhud.gamedata.LiveGameData;
import net.ctdp.rfdynhud.gamedata.ScoringInfo;
import net.ctdp.rfdynhud.gamedata.SessionType;
import net.ctdp.rfdynhud.gamedata.VehicleScoringInfo;
import net.ctdp.rfdynhud.properties.BooleanProperty;
import net.ctdp.rfdynhud.properties.ColorProperty;
import net.ctdp.rfdynhud.properties.PropertyLoader;
import net.ctdp.rfdynhud.properties.PropertiesContainer;
import net.ctdp.rfdynhud.properties.Size;
import net.ctdp.rfdynhud.render.DrawnString;
import net.ctdp.rfdynhud.render.DrawnStringFactory;
import net.ctdp.rfdynhud.render.Texture2DCanvas;
import net.ctdp.rfdynhud.render.TextureImage2D;
import net.ctdp.rfdynhud.render.TransformableTexture;
import net.ctdp.rfdynhud.render.DrawnString.Alignment;
import net.ctdp.rfdynhud.util.StandingsTools;
import net.ctdp.rfdynhud.util.SubTextureCollector;
import net.ctdp.rfdynhud.util.TimingUtil;
import net.ctdp.rfdynhud.util.PropertyWriter;
import net.ctdp.rfdynhud.valuemanagers.Clock;
import net.ctdp.rfdynhud.values.FloatValue;
import net.ctdp.rfdynhud.values.IntValue;
import net.ctdp.rfdynhud.values.StandingsView;
import net.ctdp.rfdynhud.values.StringValue;
import net.ctdp.rfdynhud.widgets.etv2010._base.ETVWidgetBase;
import net.ctdp.rfdynhud.widgets.etv2010._util.ETVUtils;
import net.ctdp.rfdynhud.widgets.etv2010._util.ETVWidgetSet;
import net.ctdp.rfdynhud.widgets.etv2010._util.ETVImages.BGType;

/**
 * The {@link ETVStandingsWidget} displays the list of drivers and gaps.
 * 
 * @author Marvin Froehlich (CTDP)
 */
public class ETVStandingsWidget extends ETVWidgetBase
{
    private final ColorProperty captionBackgroundColor1st = new ColorProperty( "captionBgColor1st", ETVWidgetSet.ETV_CAPTION_BACKGROUND_COLOR_1ST.getKey() );
    private final ColorProperty dataBackgroundColor1st = new ColorProperty( "dataBgColor1st", ETVWidgetSet.ETV_DATA_BACKGROUND_COLOR_1ST.getKey() );
    
    private final BooleanProperty forceLeaderDisplayed = new BooleanProperty( "forceLeaderDisplayed", "forceLeaderDispl", true );
    private final BooleanProperty showFastestLapsInRace = new BooleanProperty( "showFastestLapsInRace", "showFastLapsRace", true );
    
    private DrawnString[] captionStrings = null;
    private DrawnString[] nameStrings = null;
    private DrawnString[] gapStrings = null;
    
    private IntValue[] positions = null;
    private StringValue[] driverNames = null;
    private FloatValue[] gaps = null;
    
    private int maxNumItems = 0;
    
    private int oldNumItems = 0;
    
    private Boolean[] itemsVisible = null;
    
    private final Size itemHeight = Size.newGlobalSize( this, 0f, true, 2.5f, true );
    
    private TextureImage2D itemClearImage = null;
    
    private static final int NUM_FLAG_TEXTURES = 3;
    
    private TransformableTexture[] flagTextures = null;
    private final FloatValue[] laptimes = new FloatValue[ NUM_FLAG_TEXTURES ];
    private final DrawnString[] laptimeStrings = new DrawnString[ NUM_FLAG_TEXTURES ];
    
    private VehicleScoringInfo[] vehicleScoringInfos = null;
    
    private int oldNumVehicles = -1;
    
    private IntValue[] lap = null;
    private float displayTime;
    private int lastVisibleIndex = -1;
    
    public ETVStandingsWidget()
    {
        super( ETVWidgetSet.INSTANCE, ETVWidgetSet.WIDGET_PACKAGE, 14.0f, 10f * 2.5f );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void prepareForMenuItem()
    {
        super.prepareForMenuItem();
        
        itemHeight.setEffectiveSize( itemHeight.getEffectiveWidth(), 5 );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void saveProperties( PropertyWriter writer ) throws IOException
    {
        super.saveProperties( writer );
        
        writer.writeProperty( captionBackgroundColor1st, "The background color for the \"Position\" caption for first place." );
        writer.writeProperty( dataBackgroundColor1st, "The background color for the data area, for first place." );
        writer.writeProperty( itemHeight.getHeightProperty( "itemHeight" ), "The height of one item." );
        writer.writeProperty( forceLeaderDisplayed, "Display leader regardless of maximum displayed drivers setting?" );
        writer.writeProperty( showFastestLapsInRace, "Display fastest lap flags in race session?" );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void loadProperty( PropertyLoader loader )
    {
        super.loadProperty( loader );
        
        if ( loader.loadProperty( captionBackgroundColor1st ) );
        else if ( loader.loadProperty( dataBackgroundColor1st ) );
        else if ( loader.loadProperty( itemHeight.getHeightProperty( "itemHeight" ) ) );
        else if ( loader.loadProperty( forceLeaderDisplayed ) );
        else if ( loader.loadProperty( showFastestLapsInRace ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void getPropertiesCaptionBG( PropertiesContainer propsCont, boolean forceAll )
    {
        super.getPropertiesCaptionBG( propsCont, forceAll );
        
        if ( forceAll || !useImages.getBooleanValue() )
        {
            propsCont.addProperty( captionBackgroundColor1st );
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void getPropertiesDataBG( PropertiesContainer propsCont, boolean forceAll )
    {
        super.getPropertiesDataBG( propsCont, forceAll );
        
        if ( forceAll || !useImages.getBooleanValue() )
        {
            propsCont.addProperty( dataBackgroundColor1st );
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void getProperties( PropertiesContainer propsCont, boolean forceAll )
    {
        super.getProperties( propsCont, forceAll );
        
        propsCont.addGroup( "Misc" );
        
        propsCont.addProperty( itemHeight.getHeightProperty( "itemHeight" ) );
        propsCont.addProperty( forceLeaderDisplayed );
        propsCont.addProperty( showFastestLapsInRace );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int getMinHeight( LiveGameData gameData, boolean isEditorMode )
    {
        return ( 5 );
    }
    
    private final boolean getUseClassScoring()
    {
        return ( getConfiguration().getUseClassScoring() );
    }
    
    @Override
    public void onSessionStarted( SessionType sessionType, LiveGameData gameData, boolean isEditorMode )
    {
        super.onSessionStarted( sessionType, gameData, isEditorMode );
        
        if ( driverNames != null )
        {
            for ( int i = 0; i < driverNames.length; i++ )
            {
                positions[i].reset();
                driverNames[i].reset();
                gaps[i].reset();
                lap[i].reset();
            }
        }
        
        lastVisibleIndex = -1;
        
        forceReinitialization();
    }
    
    @Override
    public void onCockpitEntered( LiveGameData gameData, boolean isEditorMode )
    {
        super.onCockpitEntered( gameData, isEditorMode );
        
        if ( laptimes != null )
        {
            for ( int i = 0; i < laptimes.length; i++ )
            {
                if ( laptimes[i] != null )
                    laptimes[i].reset();
            }
        }
        
        oldNumVehicles = -1;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void initSubTextures( LiveGameData gameData, boolean isEditorMode, int widgetInnerWidth, int widgetInnerHeight, SubTextureCollector collector )
    {
        if ( !isEditorMode && gameData.getScoringInfo().getSessionType().isRace() && !showFastestLapsInRace.getBooleanValue() )
        {
            flagTextures = null;
            
            return;
        }
        
        int itemHeight = this.itemHeight.getEffectiveHeight();
        
        if ( ( flagTextures == null ) || ( flagTextures[0].getWidth() != widgetInnerWidth ) || ( flagTextures[0].getHeight() != itemHeight ) )
        {
            flagTextures = new TransformableTexture[ NUM_FLAG_TEXTURES ];
            
            for ( int i = 0; i < NUM_FLAG_TEXTURES; i++ )
            {
                flagTextures[i] = new TransformableTexture( widgetInnerWidth, itemHeight );
            }
        }
        
        for ( int i = 0; i < flagTextures.length; i++ )
            collector.add( flagTextures[i] );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean checkForChanges( LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int width, int height )
    {
        int numVehicles = getUseClassScoring() ? gameData.getScoringInfo().getNumVehiclesInSameClass( gameData.getScoringInfo().getViewedVehicleScoringInfo() ) : gameData.getScoringInfo().getNumVehicles();
        
        boolean result = ( numVehicles != oldNumVehicles );
        
        oldNumVehicles = numVehicles;
        
        return ( result );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void initialize( LiveGameData gameData, boolean isEditorMode, DrawnStringFactory dsf, TextureImage2D texture, int width, int height )
    {
        int itemHeight = this.itemHeight.getEffectiveHeight();
        maxNumItems = ( height + itemGap.getIntValue() ) / ( itemHeight + itemGap.getIntValue() );
        
        vehicleScoringInfos = new VehicleScoringInfo[ maxNumItems ];
        
        itemClearImage = TextureImage2D.getOrCreateDrawTexture( width, itemHeight * 2, true, itemClearImage, isEditorMode );
        
        boolean useImages = this.useImages.getBooleanValue();
        
        if ( useImages )
        {
            ETVUtils.drawLabeledDataBackgroundI( 0, 0, width, itemHeight, "00", getFontProperty(), getImages(), BGType.POSITION_FIRST, itemClearImage, true );
            ETVUtils.drawLabeledDataBackgroundI( 0, itemHeight, width, itemHeight, "00", getFontProperty(), getImages(), BGType.NEUTRAL, itemClearImage, true );
        }
        else
        {
            ETVUtils.drawLabeledDataBackground( 0, 0, width, itemHeight, "00", getFontProperty(), captionBackgroundColor1st.getColor(), dataBackgroundColor1st.getColor(), itemClearImage, true );
            ETVUtils.drawLabeledDataBackground( 0, itemHeight, width, itemHeight, "00", getFontProperty(), captionBackgroundColor.getColor(), dataBackgroundColor.getColor(), itemClearImage, true );
        }
        
        Texture2DCanvas texCanvas = texture.getTextureCanvas();
        texCanvas.setFont( getFont() );
        FontMetrics metrics = texCanvas.getFontMetrics();
        
        Rectangle2D numBounds = metrics.getStringBounds( "00", texCanvas );
        
        //int capWidth = (int)Math.ceil( numBounds.getWidth() );
        int captionRight = useImages ? getImages().getLabeledDataCaptionRight( itemHeight, numBounds ) : ETVUtils.getLabeledDataCaptionRight( itemHeight, numBounds );
        int dataAreaLeft = useImages ? getImages().getLabeledDataDataLeft( itemHeight, numBounds ) : ETVUtils.getLabeledDataDataLeft( itemHeight, numBounds );
        int dataAreaRight = useImages ? getImages().getLabeledDataDataRight( width, itemHeight ) : ETVUtils.getLabeledDataDataRight( width, itemHeight );
        int vMiddle = ETVUtils.getLabeledDataVMiddle( itemHeight, numBounds );
        
        captionStrings = new DrawnString[ maxNumItems ];
        nameStrings = new DrawnString[ maxNumItems ];
        gapStrings = new DrawnString[ maxNumItems ];
        
        positions = new IntValue[ maxNumItems ];
        driverNames = new StringValue[ maxNumItems ];
        gaps = new FloatValue[ maxNumItems ];
        
        lap = new IntValue[ maxNumItems ];
        
        itemsVisible = new Boolean[ maxNumItems ];
        
        for ( int i = 0; i < maxNumItems; i++ )
        {
            captionStrings[i] = dsf.newDrawnString( "captionStrings" + i, captionRight, vMiddle, Alignment.RIGHT, false, getFont(), isFontAntiAliased(), captionColor.getColor() );
            nameStrings[i] = dsf.newDrawnString( "nameStrings" + i, dataAreaLeft, vMiddle, Alignment.LEFT, false, getFont(), isFontAntiAliased(), getFontColor() );
            gapStrings[i] = dsf.newDrawnString( "gapStrings" + i, dataAreaRight, vMiddle, Alignment.RIGHT, false, getFont(), isFontAntiAliased(), getFontColor() );
            
            positions[i] = new IntValue();
            driverNames[i] = new StringValue();
            gaps[i] = new FloatValue();
            
            lap[i] = new IntValue();
            
            itemsVisible[i] = null;
        }
        
        TransformableTexture[] flagTextures = getSubTextures( gameData, isEditorMode,  width, height );
        
        if ( flagTextures != null )
        {
            for ( int i = 0; i < NUM_FLAG_TEXTURES; i++ )
            {
                if ( useImages )
                    ETVUtils.drawDataBackgroundI( 0, 0, flagTextures[i].getWidth(), flagTextures[i].getHeight(), getImages(), BGType.NEUTRAL, flagTextures[i].getTexture(), true );
                else
                    ETVUtils.drawDataBackground( 0, 0, flagTextures[i].getWidth(), flagTextures[i].getHeight(), dataBackgroundColor.getColor(), flagTextures[i].getTexture(), true );
                
                laptimes[i] = new FloatValue();
                laptimeStrings[i] = dsf.newDrawnString( "laptimeStrings" + i, flagTextures[i].getWidth() / 2, vMiddle, Alignment.CENTER, false, getFont(), isFontAntiAliased(), getFontColor() );
            }
        }
    }
    
    @Override
    public void drawWidget( Clock clock, boolean needsCompleteRedraw, LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height )
    {
        final ScoringInfo scoringInfo = gameData.getScoringInfo();
        
        final int itemHeight = this.itemHeight.getEffectiveHeight();
        
        final int numDrivers = StandingsTools.getDisplayedVSIsForScoring( scoringInfo, scoringInfo.getViewedVehicleScoringInfo(), getUseClassScoring(), StandingsView.RELATIVE_TO_LEADER, forceLeaderDisplayed.getBooleanValue(), vehicleScoringInfos );
        int numDisplayedLaptimes = 0;
        
        if ( flagTextures != null )
        {
            for ( int i = 0; i < flagTextures.length; i++ )
            {
                flagTextures[i].setVisible( false );
            }
        }
        
        if ( scoringInfo.getSessionTime() > displayTime )
        {
            lastVisibleIndex = -1;
        }
        
        int i2 = 0;
        if ( ( numDrivers > 1 ) && ( vehicleScoringInfos[1].getPlace( getUseClassScoring() ) - vehicleScoringInfos[0].getPlace( getUseClassScoring() ) > 1 ) )
        {
            i2 = 1;
        }
        
        for ( int i = 0; i < numDrivers; i++ )
        {
            VehicleScoringInfo vsi = vehicleScoringInfos[i];
            
            lap[i].update( vsi.getCurrentLap() );
            
            if ( lap[i].hasChanged() )
            {
                if ( ( i == 0 ) || ( i == i2 ) )
                {
                    lastVisibleIndex = 0;
                    displayTime = scoringInfo.getSessionTime() + 40f;
                }
                else if ( scoringInfo.getSessionTime() <= displayTime )
                {
                    lastVisibleIndex = Math.max( lastVisibleIndex, i );
                    displayTime = Math.max( displayTime, scoringInfo.getSessionTime() + 20f );
                }
            }
        }
        
        for ( int i = 0; i < numDrivers; i++ )
        {
            VehicleScoringInfo vsi = vehicleScoringInfos[i];
            short place = vsi.getPlace( getUseClassScoring() );
            
            Boolean visible;
            if ( isEditorMode )
            {
                visible = true;
            }
            else
            {
                if ( scoringInfo.getSessionType().isRace() )
                    visible = ( i <= lastVisibleIndex );
                else
                    visible = ( vsi.getBestLapTime() > 0.0f );
            }
            
            boolean drawBackground = needsCompleteRedraw;
            boolean visibilityChanged = false;
            
            if ( visible != itemsVisible[i] )
            {
                itemsVisible[i] = visible;
                drawBackground = true;
                visibilityChanged = true;
            }
            
            int offsetY2 = i * ( itemHeight + itemGap.getIntValue() );
            int srcOffsetY = ( place == 1 ) ? 0 : itemHeight;
            
            if ( drawBackground )
            {
                if ( visible )
                    texture.clear( itemClearImage, 0, srcOffsetY, width, itemHeight, offsetX, offsetY + offsetY2, width, itemHeight, true, null );
                else
                    texture.clear( offsetX, offsetY + offsetY2, width, itemHeight, true, null );
            }
            
            positions[i].update( place );
            
            if ( ( needsCompleteRedraw || visibilityChanged || positions[i].hasChanged() ) && visible )
                captionStrings[i].draw( offsetX, offsetY + offsetY2, positions[i].getValueAsString(), captionColor.getColor(), texture, itemClearImage, offsetX, offsetY + offsetY2 - srcOffsetY );
            
            driverNames[i].update( vsi.getDriverNameTLC( getShowNamesInAllUppercase() ) );
            
            if ( ( needsCompleteRedraw || visibilityChanged || driverNames[i].hasChanged() ) && visible )
                nameStrings[i].draw( offsetX, offsetY + offsetY2, driverNames[i].getValue(), getFontColor(), texture, itemClearImage, offsetX, offsetY + offsetY2 - srcOffsetY );
            
            if ( place > 1 )
            {
                if ( scoringInfo.getSessionType().isRace() )
                    gaps[i].update( ( vsi.getLapsBehindLeader( getUseClassScoring() ) > 0 ) ? -vsi.getLapsBehindLeader( getUseClassScoring() ) - 10000 : Math.abs( vsi.getTimeBehindLeader( getUseClassScoring() ) ) );
                else
                    gaps[i].update( vsi.getBestLapTime() - scoringInfo.getLeadersVehicleScoringInfo().getBestLapTime() );
                
                if ( ( needsCompleteRedraw || visibilityChanged || gaps[i].hasChanged() ) && visible )
                {
                    String s;
                    if ( vsi.getBestLapTime() < 0.0f )
                    {
                        s = "";
                    }
                    else if ( gaps[i].getValue() < -10000f )
                    {
                        int l = ( (int)-( gaps[i].getValue() + 10000.0f ) );
                        
                        if ( l == 1 )
                            s = "+" + l + Loc.gap_lap;
                        else
                            s = "+" + l + Loc.gap_laps;
                    }
                    else
                    {
                        s = TimingUtil.getTimeAsGapString( gaps[i].getValue() );
                    }
                    
                    gapStrings[i].draw( offsetX, offsetY + offsetY2, s, getFontColor(), texture, itemClearImage, offsetX, offsetY + offsetY2 - srcOffsetY );
                }
            }
            
            if ( ( flagTextures != null ) && visible && ( numDisplayedLaptimes < flagTextures.length - 1 ) )
            {
                Laptime lt = vsi.getFastestLaptime();
                boolean show = ( ( lt != null ) && ( lt.getLap() == vsi.getCurrentLap() - 1 ) && ( vsi.getStintStartLap() != vsi.getCurrentLap() ) && ( scoringInfo.getSessionTime() - vsi.getLapStartTime() < 20.0f ) );
                if ( isEditorMode )
                    show = ( numDisplayedLaptimes < 2 );
                
                if ( show )
                {
                    int tti = numDisplayedLaptimes++;
                    TransformableTexture tt = flagTextures[tti];
                    
                    laptimes[tti].update( lt.getLapTime() );
                    
                    if ( laptimes[tti].hasChanged() )
                    {
                        laptimeStrings[tti].draw( 0, 0, TimingUtil.getTimeAsLaptimeString( laptimes[tti].getValue() ), tt.getTexture(), dataBackgroundColor.getColor() );
                    }
                    
                    int off;
                    if ( useImages.getBooleanValue() )
                    {
                        float scale = getImages().getLabeledDataImageScale( itemHeight );
                        off = (int)( ( getImages().getLabeledDataVirtualProjectionBorderRight() + getImages().getDataVirtualProjectionBorderRight() ) * scale );
                        int b1 = getImages().getLabeledDataDataBorderRight() + 1;
                        int b2 = getImages().getDataBorderLeft() + 1;
                        int b = (int)Math.floor( ( Math.min( b1, b2 ) ) * scale );
                        off -= b;
                        off += itemGap.getIntValue();
                    }
                    else
                    {
                        off = -( ETVUtils.getTriangleWidth( itemHeight ) - itemGap.getIntValue() );
                    }
                    
                    boolean isOnLeftSide = ( getPosition().getEffectiveX() < getConfiguration().getGameResolution().getViewportWidth() - getPosition().getEffectiveX() - getSize().getEffectiveWidth() );
                    if ( isOnLeftSide )
                        tt.setTranslation( width + off, offsetY2 );
                    else
                        tt.setTranslation( -width - off, offsetY2 );
                    tt.setVisible( true );
                }
            }
        }
        
        for ( int i = numDrivers; i < oldNumItems; i++ )
        {
            int offsetY2 = i * ( itemHeight + itemGap.getIntValue() );
            
            texture.clear( offsetX, offsetY + offsetY2, width, itemHeight, true, null );
        }
        
        oldNumItems = numDrivers;
    }
}
