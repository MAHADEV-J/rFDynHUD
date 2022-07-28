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
package net.ctdp.rfdynhud.widgets.standard.trackposition;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;

import net.ctdp.rfdynhud.gamedata.LiveGameData;
import net.ctdp.rfdynhud.gamedata.ModInfo;
import net.ctdp.rfdynhud.gamedata.ScoringInfo;
import net.ctdp.rfdynhud.gamedata.VehicleScoringInfo;
import net.ctdp.rfdynhud.properties.BooleanProperty;
import net.ctdp.rfdynhud.properties.ColorProperty;
import net.ctdp.rfdynhud.properties.EnumProperty;
import net.ctdp.rfdynhud.properties.FontProperty;
import net.ctdp.rfdynhud.properties.IntProperty;
import net.ctdp.rfdynhud.properties.PropertiesContainer;
import net.ctdp.rfdynhud.properties.Property;
import net.ctdp.rfdynhud.properties.PropertyLoader;
import net.ctdp.rfdynhud.render.DrawnStringFactory;
import net.ctdp.rfdynhud.render.Texture2DCanvas;
import net.ctdp.rfdynhud.render.TextureImage2D;
import net.ctdp.rfdynhud.render.TransformableTexture;
import net.ctdp.rfdynhud.util.MapTools;
import net.ctdp.rfdynhud.util.PropertyWriter;
import net.ctdp.rfdynhud.util.SubTextureCollector;
import net.ctdp.rfdynhud.valuemanagers.Clock;
import net.ctdp.rfdynhud.widgets.WidgetsConfiguration;
import net.ctdp.rfdynhud.widgets.base.widget.Widget;
import net.ctdp.rfdynhud.widgets.standard._util.LabelPositioning;
import net.ctdp.rfdynhud.widgets.standard._util.StandardWidgetSet;

/**
 * The {@link TrackPositionWidget} displays all driving vehicles on a line.
 * 
 * @author Marvin Froehlich (CTDP)
 */
public class TrackPositionWidget extends Widget
{
    private static final int LINE_THICKNESS = 1;
    private static final int BASE_LINE_PADDING = 30;
    
    private final IntProperty baseItemRadius = new IntProperty( "itemRadius", "radius", 9, 1, 100, false )
    {
        @Override
        protected void onValueChanged( Integer oldValue, int newValue )
        {
            super.onValueChanged( oldValue, newValue );
            
            if ( getConfiguration() != null )
                updateItemRadius();
            
            forceAndSetDirty( false );
        }
    };
    private int itemRadius = baseItemRadius.getIntValue();
    private void updateItemRadius()
    {
        itemRadius = Math.round( baseItemRadius.getIntValue() * getConfiguration().getGameResolution().getViewportHeight() / 960f );
    }
    private int itemBlackBorderWidth = 2;
    
    private final ColorProperty lineColor = new ColorProperty( "lineColor", "#FFFFFF" );
    
    private final ColorProperty markColorNormal = new ColorProperty( "markColorNormal", StandardWidgetSet.POSITION_ITEM_COLOR_NORMAL.getKey() );
    private final ColorProperty markColorLeader = new ColorProperty( "markColorLeader", StandardWidgetSet.POSITION_ITEM_COLOR_LEADER.getKey() );
    private final ColorProperty markColorMe = new ColorProperty( "markColorMe", StandardWidgetSet.POSITION_ITEM_COLOR_ME.getKey() );
    private final BooleanProperty useMyColorForMe1st = new BooleanProperty( "useMyColorForMe1st", false );
    private final ColorProperty markColorNextInFront = new ColorProperty( "markColorNextInFront", StandardWidgetSet.POSITION_ITEM_COLOR_NEXT_IN_FRONT.getKey() );
    private final ColorProperty markColorNextBehind = new ColorProperty( "markColorNextBehind", StandardWidgetSet.POSITION_ITEM_COLOR_NEXT_BEHIND.getKey() );
    
    private final BooleanProperty displayPositionNumbers = new BooleanProperty( "displayPosNumbers", true );
    
    private final BooleanProperty displayNameLabels = new BooleanProperty( "displayNameLabels", false );
    private final EnumProperty<LabelPositioning> nameLabelPos = new EnumProperty<LabelPositioning>( "nameLabelPos", LabelPositioning.BELOW );
    private final FontProperty nameLabelFont = new FontProperty( "nameLabelFont", StandardWidgetSet.POSITION_ITEM_FONT.getKey() );
    private final ColorProperty nameLabelFontColor = new ColorProperty( "nameLabelFontColor", StandardWidgetSet.POSITION_ITEM_COLOR_NORMAL.getKey() );
    
    private int maxDisplayedVehicles = -1;
    
    private static final int ANTI_ALIAS_RADIUS_OFFSET = 1;
    
    private TransformableTexture[] itemTextures = null;
    private int middleOffsetY = 0;
    private int[] itemLabelOffsetsY = null;
    private VehicleScoringInfo[] vsis = null;
    private int[] itemStates = null;
    private int numVehicles = -1;
    
    private int lineLength = 0;
    
    public TrackPositionWidget()
    {
        super( StandardWidgetSet.INSTANCE, StandardWidgetSet.WIDGET_PACKAGE, 35.0f, 5.859375f );
        
        getFontProperty().setFont( StandardWidgetSet.POSITION_ITEM_FONT.getKey() );
        getFontColorProperty().setColor( StandardWidgetSet.POSITION_ITEM_FONT_COLOR.getKey() );
    }
    
    @Override
    public void prepareForMenuItem()
    {
        super.prepareForMenuItem();
        
        //baseItemRadius.setIntValue( 20 );
        itemRadius = 3;
        itemBlackBorderWidth = 0;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void saveProperties( PropertyWriter writer ) throws IOException
    {
        super.saveProperties( writer );
        
        writer.writeProperty( lineColor, "Color for the base line." );
        writer.writeProperty( baseItemRadius, "The abstract radius for any displayed driver item." );
        writer.writeProperty( markColorNormal, "The color used for all, but special cars in #RRGGBBAA (hex)." );
        writer.writeProperty( markColorLeader, "The color used for the leader's car in #RRGGBBAA (hex)." );
        writer.writeProperty( markColorMe, "The color used for your own car in #RRGGBBAA (hex)." );
        writer.writeProperty( useMyColorForMe1st, "Use 'markColorMe' for my item when I am at 1st place?" );
        writer.writeProperty( markColorNextInFront, "The color used for the car in front of you in #RRGGBBAA (hex)." );
        writer.writeProperty( markColorNextBehind, "The color used for the car behind you in #RRGGBBAA (hex)." );
        writer.writeProperty( displayPositionNumbers, "Display numbers on the position markers?" );
        writer.writeProperty( displayNameLabels, "Display name label near the position markers?" );
        writer.writeProperty( nameLabelPos, "Positioning of the name labels." );
        writer.writeProperty( nameLabelFont, "Font for the name labels." );
        writer.writeProperty( nameLabelFontColor, "Font color for the name labels." );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void loadProperty( PropertyLoader loader )
    {
        super.loadProperty( loader );
        
        if ( loader.loadProperty( lineColor ) );
        else if ( loader.loadProperty( baseItemRadius ) );
        else if ( loader.loadProperty( markColorNormal ) );
        else if ( loader.loadProperty( markColorLeader ) );
        else if ( loader.loadProperty( markColorMe ) );
        else if ( loader.loadProperty( useMyColorForMe1st ) );
        else if ( loader.loadProperty( markColorNextInFront ) );
        else if ( loader.loadProperty( markColorNextBehind ) );
        else if ( loader.loadProperty( displayPositionNumbers ) );
        else if ( loader.loadProperty( displayNameLabels ) );
        else if ( loader.loadProperty( nameLabelPos ) );
        else if ( loader.loadProperty( nameLabelFont ) );
        else if ( loader.loadProperty( nameLabelFontColor ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void getProperties( PropertiesContainer propsCont, boolean forceAll )
    {
        super.getProperties( propsCont, forceAll );
        
        propsCont.addGroup( "Misc" );
        
        propsCont.addProperty( lineColor );
        
        propsCont.addProperty( baseItemRadius );
        
        propsCont.addProperty( markColorNormal );
        propsCont.addProperty( markColorLeader );
        propsCont.addProperty( markColorMe );
        propsCont.addProperty( useMyColorForMe1st );
        propsCont.addProperty( markColorNextInFront );
        propsCont.addProperty( markColorNextBehind );
        
        propsCont.addProperty( displayPositionNumbers );
        
        propsCont.addProperty( displayNameLabels );
        //if ( displayNameLabels.getBooleanValue() || forceAll )
        {
            propsCont.addProperty( nameLabelPos );
            propsCont.addProperty( nameLabelFont );
            propsCont.addProperty( nameLabelFontColor );
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onPropertyChanged( Property property, Object oldValue, Object newValue )
    {
        super.onPropertyChanged( property, oldValue, newValue );
        
        if ( itemStates != null )
        {
            for ( int i = 0; i < itemStates.length; i++ )
                itemStates[i] = 0;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void afterConfigurationLoaded( WidgetsConfiguration widgetsConfig, LiveGameData gameData, boolean isEditorMode )
    {
        super.afterConfigurationLoaded( widgetsConfig, gameData, isEditorMode );
        
        updateItemRadius();
    }
    
    private final boolean getUseClassScoring()
    {
        return ( getConfiguration().getUseClassScoring() );
    }
    
    private void initMaxDisplayedVehicles( boolean isEditorMode, ModInfo modInfo )
    {
        if ( isEditorMode )
            this.maxDisplayedVehicles = 22 + 1;
        else
            this.maxDisplayedVehicles = modInfo.getMaxOpponents() + 1;
        
        this.maxDisplayedVehicles = Math.max( 4, Math.min( maxDisplayedVehicles, 32 ) );
    }
    
    private void updateVSIs( LiveGameData gameData, boolean isEditorMode )
    {
        initMaxDisplayedVehicles( isEditorMode, gameData.getModInfo() );
        
        if ( ( vsis == null ) || ( vsis.length < maxDisplayedVehicles ) )
        {
            vsis = new VehicleScoringInfo[ maxDisplayedVehicles ];
            
            if ( itemStates == null )
            {
                itemStates = new int[ maxDisplayedVehicles ];
            }
            else
            {
                int[] tmpItemStates = new int[ maxDisplayedVehicles ];
                
                System.arraycopy( itemStates, 0, tmpItemStates, 0, itemStates.length );
                itemStates = tmpItemStates;
            }
            
            for ( int i = 0; i < itemStates.length; i++ )
                itemStates[i] = 0;
        }
        
        numVehicles = MapTools.getDisplayedVSIsForMap( gameData.getScoringInfo(), gameData.getScoringInfo().getViewedVehicleScoringInfo(), getUseClassScoring(), true, vsis );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onScoringInfoUpdated( LiveGameData gameData, boolean isEditorMode )
    {
        updateVSIs( gameData, isEditorMode );
    }
    
    @Override
    protected void initSubTextures( LiveGameData gameData, boolean isEditorMode, int widgetInnerWidth, int widgetInnerHeight, SubTextureCollector collector )
    {
        updateItemRadius();
        initMaxDisplayedVehicles( isEditorMode, gameData.getModInfo() );
        
        if ( ( itemTextures == null ) || ( itemTextures.length != maxDisplayedVehicles ) )
        {
            itemTextures = new TransformableTexture[ maxDisplayedVehicles ];
        }
        
        if ( itemTextures[0] == null )
            itemTextures[0] = new TransformableTexture( 1, 1, isEditorMode, false );
        
        java.awt.Dimension size = StandardWidgetSet.getPositionItemSize( itemRadius, null, null, false );
        int itemHeightWithoutLabel = size.height;
        
        size = StandardWidgetSet.getPositionItemSize( itemRadius, displayNameLabels.getBooleanValue() ? nameLabelPos.getEnumValue() : null, nameLabelFont.getFont(), nameLabelFont.isAntiAliased() );
        int w = size.width;
        int h = size.height;
        
        if ( nameLabelPos.getEnumValue() == LabelPositioning.ABOVE )
            middleOffsetY = ( h - itemHeightWithoutLabel ) / 2;
        else
            middleOffsetY = ( itemHeightWithoutLabel - h ) / 2;
        
        if ( ( itemTextures[maxDisplayedVehicles - 1] == null ) || ( itemTextures[maxDisplayedVehicles - 1].getWidth() != w ) || ( itemTextures[maxDisplayedVehicles - 1].getHeight() != h ) )
        {
            for ( int i = 0; i < maxDisplayedVehicles; i++ )
            {
                itemTextures[i] = TransformableTexture.getOrCreate( w, h, isEditorMode, itemTextures[i], isEditorMode );
                itemTextures[i].setVisible( false );
            }
        }
        
        for ( int i = 0; i < itemTextures.length; i++ )
            collector.add( itemTextures[i] );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void initialize( LiveGameData gameData, boolean isEditorMode, DrawnStringFactory dsf, TextureImage2D texture, int width, int height )
    {
        initMaxDisplayedVehicles( isEditorMode, gameData.getModInfo() );
        
        if ( isEditorMode )
            updateVSIs( gameData, isEditorMode );
    }
    
    @Override
    protected void drawBackground( LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height, boolean isRoot )
    {
        super.drawBackground( gameData, isEditorMode, texture, offsetX, offsetY, width, height, isRoot );
        
        Texture2DCanvas texCanvas = texture.getTextureCanvas();
        
        texCanvas.setAntialiazingEnabled( true );
        
        int lineHeight = LINE_THICKNESS;
        int linePadding = (int)( BASE_LINE_PADDING * getConfiguration().getGameResolution().getViewportHeight() / (float)1200 );
        lineLength = width - 2 * linePadding;
        
        texCanvas.setColor( lineColor.getColor() );
        
        texCanvas.fillRect( offsetX + linePadding, offsetY + ( height - lineHeight ) / 2 + middleOffsetY, lineLength, lineHeight );
    }
    
    @Override
    public void drawWidget( Clock clock, boolean needsCompleteRedraw, LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height )
    {
        final ScoringInfo scoringInfo = gameData.getScoringInfo();
        final VehicleScoringInfo viewedVSI = scoringInfo.getViewedVehicleScoringInfo();
        final boolean useClassScoring = getUseClassScoring();
        
        int linePadding = (int)( BASE_LINE_PADDING * getConfiguration().getGameResolution().getViewportWidth() / 1920f );
        
        int off2 = ( isFontAntiAliased() ? ANTI_ALIAS_RADIUS_OFFSET : 0 );
        
        short ownPlace = scoringInfo.getOwnPlace( useClassScoring );
        
        final Font font = getFont();
        final boolean posNumberFontAntiAliased = isFontAntiAliased();
        
        int n = Math.min( scoringInfo.getNumVehicles(), maxDisplayedVehicles );
        
        if ( ( itemLabelOffsetsY == null ) || ( itemLabelOffsetsY.length < itemTextures.length ) )
        {
            itemLabelOffsetsY = new int[ itemTextures.length ];
        }
        
        for ( int i = 0; i < numVehicles; i++ )
        {
            VehicleScoringInfo vsi = vsis[i];
            if ( vsi != null )
            {
                short place = vsi.getPlace( useClassScoring );
                
                TransformableTexture tt = itemTextures[i];
                itemTextures[i].setVisible( true );
                int itemState = ( place << 0 ) | ( vsi.getDriverId() << 9 );
                
                Color color = null;
                if ( ( place == 1 ) && ( !useClassScoring || ( vsi.getVehicleClassId() == viewedVSI.getVehicleClassId() ) ) )
                {
                    itemState |= 1 << 26;
                    if ( vsi.isPlayer() && useMyColorForMe1st.getBooleanValue() )
                        color = markColorMe.getColor();
                    else
                        color = markColorLeader.getColor();
                }
                else if ( vsi.isPlayer() )
                {
                    itemState |= 1 << 27;
                    color = markColorMe.getColor();
                }
                else if ( ( place == ownPlace - 1 ) && ( !useClassScoring || ( vsi.getVehicleClassId() == viewedVSI.getVehicleClassId() ) ) )
                {
                    itemState |= 1 << 28;
                    color = markColorNextInFront.getColor();
                }
                else if ( ( place == ownPlace + 1 ) && ( !useClassScoring || ( vsi.getVehicleClassId() == viewedVSI.getVehicleClassId() ) ) )
                {
                    itemState |= 1 << 29;
                    color = markColorNextBehind.getColor();
                }
                else
                {
                    itemState |= 1 << 30;
                    color = markColorNormal.getColor();
                }
                
                if ( itemStates[i] != itemState )
                {
                    itemStates[i] = itemState;
                    
                    itemLabelOffsetsY[i] = StandardWidgetSet.drawPositionItem( tt.getTexture(), 0, 0, itemRadius, place, color, itemBlackBorderWidth, displayPositionNumbers.getBooleanValue() ? font : null, posNumberFontAntiAliased, getFontColor(), displayNameLabels.getBooleanValue() ? nameLabelPos.getEnumValue() : null, vsi.getDriverNameTLC(), nameLabelFont.getFont(), nameLabelFont.isAntiAliased(), nameLabelFontColor.getColor() );
                }
                
                int yOff3 = vsi.isInPits() ? -3 : 0;
                
                tt.setTranslation( linePadding + off2 + vsi.getNormalizedLapDistance() * lineLength - itemRadius, off2 + height / 2 - itemRadius - itemLabelOffsetsY[i] + yOff3 + middleOffsetY );
            }
            else
            {
                itemTextures[i].setVisible( false );
            }
        }
        
        for ( int i = n; i < maxDisplayedVehicles; i++ )
            itemTextures[i].setVisible( false );
    }
}
