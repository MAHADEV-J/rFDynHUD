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
package net.ctdp.rfdynhud.render;

import java.nio.ByteBuffer;

import net.ctdp.rfdynhud.gamedata.LiveGameData;
import net.ctdp.rfdynhud.gamedata.__GDPrivilegedAccess;
import net.ctdp.rfdynhud.properties.Position;
import net.ctdp.rfdynhud.util.RFDHLog;
import net.ctdp.rfdynhud.valuemanagers.Clock;
import net.ctdp.rfdynhud.valuemanagers.TimeBasedClock;
import net.ctdp.rfdynhud.widgets.WidgetsConfiguration;
import net.ctdp.rfdynhud.widgets.__WCPrivilegedAccess;
import net.ctdp.rfdynhud.widgets.base.widget.Widget;
import net.ctdp.rfdynhud.widgets.base.widget.WidgetController;
import net.ctdp.rfdynhud.widgets.base.widget.__WPrivilegedAccess;

/**
 * The {@link WidgetsDrawingManager} handles the drawing of all visible widgets.
 * 
 * @author Marvin Froehlich (CTDP)
 */
public class WidgetsDrawingManager
{
    private final boolean oneTextureForAllWidgets;
    
    private TransformableTexture[] textures;
    private TransformableTexture[] widgetTextures;
    private TransformableTexture[][] widgetSubTextures = null;
    private final ByteBuffer textureInfoBuffer = TransformableTexture.createByteBuffer();
    
    private long frameCounter = 0L;
    
    private final Clock clock = new TimeBasedClock( 50000000L ); // 50 ms
    
    private final WidgetsConfiguration widgetsConfig;
    
    private final WidgetsManager renderListenersManager = new WidgetsManager();
    
    public final WidgetsManager getRenderListenersManager()
    {
        return ( renderListenersManager );
    }
    
    public final WidgetsConfiguration getWidgetsConfiguration()
    {
        return ( widgetsConfig );
    }
    
    public void resizeMainTexture( int gameResX, int gameResY )
    {
        if ( oneTextureForAllWidgets )
        {
            if ( ( textures[0].getWidth() != gameResX ) || ( textures[0].getHeight() != gameResY ) )
                textures[0] = TransformableTexture.createMainTexture( gameResX, gameResY, false );
        }
        
        __GDPrivilegedAccess.setGameResolution( gameResX, gameResY, widgetsConfig );
        __WCPrivilegedAccess.setViewport( 0, 0, gameResX, gameResY, widgetsConfig );
    }
    
    public final TextureImage2D getMainTexture( int widgetIndex )
    {
        if ( oneTextureForAllWidgets )
            return ( textures[0].getTexture() );
        
        TransformableTexture tt = widgetTextures[widgetIndex];
        
        if ( tt == null )
            return ( null );
        
        return ( tt.getTexture() );
    }
    
    public int collectTextures( LiveGameData gameData, boolean isEditorMode )
    {
        int numTextures = 0;
        
        final int numWidgets = widgetsConfig.getNumWidgets();
        
        if ( oneTextureForAllWidgets )
        {
            numTextures = 1;
            textures[0].generateRectanglesForOneBigTexture( gameData, isEditorMode, widgetsConfig );
        }
        else
        {
            numTextures = 0;
            textures = new TransformableTexture[ numWidgets ];
            widgetTextures = new TransformableTexture[ numWidgets ];
            
            for ( int i = 0; i < numWidgets; i++ )
            {
                Widget widget = widgetsConfig.getWidget( i );
                
                if ( widget.hasMasterCanvas( isEditorMode ) )
                {
                    textures[numTextures] = TransformableTexture.createMainTexture( widget.getMaxWidth( gameData, isEditorMode ), widget.getMaxHeight( gameData, isEditorMode ), true );
                    textures[numTextures].setTranslation( widget.getPosition().getEffectiveX(), widget.getPosition().getEffectiveY() );
                    
                    widgetTextures[i] = textures[numTextures];
                    numTextures++;
                }
                else
                {
                    widgetTextures[i] = null;
                }
            }
        }
        
        widgetSubTextures = new TransformableTexture[ numWidgets ][];
        
        int j = 0;
        for ( int i = 0; i < numWidgets; i++ )
        {
            Widget widget = widgetsConfig.getWidget( i );
            
            if ( !oneTextureForAllWidgets && widget.hasMasterCanvas( isEditorMode ) )
            {
                j++;
            }
            
            TransformableTexture[] subTextures = widget.getSubTextures( gameData, isEditorMode, widget.getSize().getEffectiveWidth() - widget.getBorder().getInnerLeftWidth() - widget.getBorder().getInnerRightWidth(), widget.getSize().getEffectiveHeight() - widget.getBorder().getInnerTopHeight() - widget.getBorder().getInnerBottomHeight() );
            if ( ( subTextures != null ) && ( subTextures.length > 0 ) )
            {
                widgetSubTextures[i] = subTextures;
                
                if ( textures.length < numTextures + subTextures.length )
                {
                    TransformableTexture[] tmp = new TransformableTexture[ numTextures + subTextures.length ];
                    System.arraycopy( textures, 0, tmp, 0, numTextures );
                    textures = tmp;
                }
                
                //System.arraycopy( subTextures, 0, textures, numTextures, subTextures.length );
                
                // Shift succeeding textures, so that we can insert sub textures.
                System.arraycopy( textures, j, textures, j + subTextures.length, numTextures - j );
                
                // Insert sub textures.
                System.arraycopy( subTextures, 0, textures, j, subTextures.length );
                
                j += subTextures.length;
                numTextures += subTextures.length;
            }
            else
            {
                widgetSubTextures[i] = null;
            }
        }
        
        if ( numTextures > TransformableTexture.MAX_NUM_TEXTURES )
        {
            RFDHLog.error( "WARNING: Number of displayed textures truncated. Possible reason: maxOpponents = " + gameData.getModInfo().getMaxOpponents() );
            
            numTextures = TransformableTexture.MAX_NUM_TEXTURES;
        }
        
        if ( numTextures < textures.length )
        {
            TransformableTexture[] tmp = new TransformableTexture[ numTextures ];
            System.arraycopy( textures, 0, tmp, 0, numTextures );
            textures = tmp;
        }
        
        textureInfoBuffer.position( 0 );
        textureInfoBuffer.limit( textureInfoBuffer.capacity() );
        
        textureInfoBuffer.put( (byte)textures.length );
        
        int rectOffset = 0;
        for ( int i = 0; i < textures.length; i++ )
        {
            rectOffset = textures[i].fillBuffer( true, 0, 0, i, rectOffset, textureInfoBuffer ); // offsets are irrelevant here
            textures[i].setDirty();
        }
        
        textureInfoBuffer.position( textures.length * TransformableTexture.STRUCT_SIZE );
        textureInfoBuffer.flip();
        
        return ( textures.length );
    }
    
    public void clearCompleteTexture()
    {
        for ( int i = 0; i < textures.length; i++ )
        {
            TextureImage2D texture = textures[i].getTexture();
            if ( textures[i].isDynamic() )
            {
                texture.getTextureCanvas().setClip( 0, 0, texture.getWidth(), texture.getHeight() );
                texture.clear( true, null );
            }
        }
        
        for ( int i = 0; i < widgetsConfig.getNumWidgets(); i++ )
        {
            widgetsConfig.getWidget( i ).forceCompleteRedraw( true );
            widgetsConfig.getWidget( i ).forceReinitialization();
            // TODO: We possibly need to set this texture dirty!
        }
    }
    
    public void refreshTextureInfoBuffer( boolean isEditorMode, LiveGameData gameData, boolean newConfig )
    {
        textureInfoBuffer.position( 0 );
        textureInfoBuffer.limit( textureInfoBuffer.capacity() );
        
        textureInfoBuffer.put( (byte)textures.length );
        
        final int n = widgetsConfig.getNumWidgets();
        if ( oneTextureForAllWidgets )
        {
            int j = 0;
            for ( int i = 0; i < n; i++ )
            {
                Widget widget = widgetsConfig.getWidget( i );
                
                if ( widget.hasMasterCanvas( isEditorMode ) )
                    textures[0].setRectangleVisible( j++, widget.isVisible() );
            }
        }
        
        int k = 0;
        int rectOffset = 0;
        int testRectOffset = 0;
        
        if ( oneTextureForAllWidgets )
        {
            rectOffset = textures[0].fillBuffer( true, 0, 0, k++, rectOffset, textureInfoBuffer );
            testRectOffset += textures[0].getNumUsedRectangles();
        }
        else
        {
            for ( int i = 0; i < n; i++ )
            {
                if ( widgetTextures[i] != null )
                {
                    Widget widget = widgetsConfig.getWidget( i );
                    
                    boolean dirty = false;
                    
                    if ( widget.getWidgetController() != null )
                    {
                        Position position = widget.getPosition();
                        
                        widgetTextures[i].setTranslation( position.getEffectiveX(), position.getEffectiveY() );
                        dirty = widgetTextures[i].isDirty();
                        
                        if ( !position.isBaked() )
                            position.bake();
                    }
                    
                    rectOffset = widgetTextures[i].fillBuffer( widget.isVisible(), 0, 0, k++, rectOffset, textureInfoBuffer );
                    if ( widgetSubTextures[i] != null )
                        k += widgetSubTextures[i].length; // skip sub textures
                    testRectOffset += widgetTextures[i].getNumUsedRectangles();
                    
                    if ( dirty )
                    {
                        TransformableTexture[] subTextures = widgetSubTextures[i];
                        if ( subTextures != null )
                        {
                            for ( int j = 0; j < subTextures.length; j++ )
                            {
                                subTextures[j].setDirty();
                            }
                        }
                    }
                }
                else if ( widgetSubTextures[i] != null )
                {
                    k += widgetSubTextures[i].length; // skip sub textures
                }
            }
        }
        
        k = 0;
        
        for ( int i = 0; i < widgetSubTextures.length; i++ )
        {
            if ( !oneTextureForAllWidgets && ( widgetTextures[i] != null ) )
                k++; // skip widget texture
            
            Widget widget = widgetsConfig.getWidget( i );
            TransformableTexture[] subTextures = widgetSubTextures[i];
            if ( subTextures != null )
            {
                int offX = widget.getPosition().getEffectiveX() + widget.getBorder().getInnerLeftWidth();
                int offY = widget.getPosition().getEffectiveY() + widget.getBorder().getInnerTopHeight();
                
                for ( int j = 0; j < subTextures.length; j++ )
                {
                    rectOffset = subTextures[j].fillBuffer( widget.isVisible(), offX + subTextures[j].getOffsetXToRootMasterWidget(), offY + subTextures[j].getOffsetYToRootMasterWidget(), k++, rectOffset, textureInfoBuffer );
                    testRectOffset += subTextures[j].getNumUsedRectangles();
                }
            }
        }
        
        textureInfoBuffer.position( textures.length * TransformableTexture.STRUCT_SIZE );
        textureInfoBuffer.flip();
        
        if ( newConfig && ( rectOffset < testRectOffset ) )
        {
            RFDHLog.error( "WARNING: Number of displayed textures truncated. Possible reason: maxOpponents = " + gameData.getModInfo().getMaxOpponents() );
        }
    }
    
    public final int getNumTextures()
    {
        return ( textures.length );
    }
    
    public final ByteBuffer getTextureInfoBuffer()
    {
        return ( textureInfoBuffer );
    }
    
    public final TransformableTexture getTexture( int textureIndex )
    {
        return ( textures[textureIndex] );
    }
    
    public void onCockpitEntered( LiveGameData gameData )
    {
        clock.init( gameData.getScoringInfo().getSessionNanos() );
        frameCounter = 0L;
    }
    
    private static final boolean isWidgetReady( Widget widget, boolean hasWaitingWidgets, LiveGameData gameData )
    {
        boolean ready = true;
        
        if ( hasWaitingWidgets )
        {
            int neededData = widget.getNeededData();
            
            if ( ( ( neededData & Widget.NEEDED_DATA_TELEMETRY ) != 0 ) && !gameData.getTelemetryData().isUpdatedInTimeScope() )
                ready = false;
            else if ( ( ( neededData & Widget.NEEDED_DATA_SCORING ) != 0 ) && !gameData.getScoringInfo().isUpdatedInTimeScope() )
                ready = false;
            //else if ( ( ( neededData & Widget.NEEDED_DATA_SETUP ) != 0 ) && !gameData.getSetup().isUpdatedInTimeScope() )
            //    ready = false;
        }
        
        return ( ready );
    }
    
    /**
     * Draws all visible {@link Widget}s in the list.
     * 
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     * @param hasWaitingWidgets
     * @param completeRedrawForced complete redraw forced?
     */
    public void drawWidgets( LiveGameData gameData, boolean isEditorMode, boolean hasWaitingWidgets, boolean completeRedrawForced )
    {
        long sessionNanos = gameData.getScoringInfo().getSessionNanos();
        
        clock.update( sessionNanos, frameCounter, completeRedrawForced );
        
        frameCounter++;
        
        renderListenersManager.fireBeforeWidgetsAreRendered( gameData, widgetsConfig, sessionNanos, frameCounter );
        
        if ( !widgetsConfig.isValid() )
            return;
        
        __WCPrivilegedAccess.checkFixAndBakeConfiguration( widgetsConfig, isEditorMode );
        
        final int n = widgetsConfig.getNumWidgets();
        
        for ( int i = 0; i < n; i++ )
        {
            Widget widget = widgetsConfig.getWidget( i );
            
            WidgetController controller = widget.getWidgetController();
            
            if ( controller != null )
            {
                try
                {
                    controller.update( widget, gameData );
                }
                catch ( Throwable t )
                {
                    RFDHLog.exception( t );
                }
                
                __GDPrivilegedAccess.setControlledVSIs( gameData.getScoringInfo(), __WPrivilegedAccess.getControlledViewedVSI( controller ), __WPrivilegedAccess.getControlledCompareVSI( controller ) );
            }
            else
            {
                __GDPrivilegedAccess.setControlledVSIs( gameData.getScoringInfo(), null, null );
            }
            
            TextureImage2D texture = getMainTexture( i );
            if ( texture != null )
                texture.getTextureCanvas().setClip( 0, 0, texture.getWidth(), texture.getHeight() );
            
            try
            {
                if ( isWidgetReady( widget, hasWaitingWidgets, gameData ) )
                {
                    __WPrivilegedAccess.updateVisibility( widget, gameData, isEditorMode );
                    
                    if ( isEditorMode )
                    {
                        if ( widget.getDirtyFlag( true ) )
                        {
                            widget.drawWidget( clock, completeRedrawForced, gameData, isEditorMode, texture, !oneTextureForAllWidgets );
                        }
                    }
                    else if ( !widget.isVisible() && widget.visibilityChangedSinceLastDraw() )
                    {
                        int offsetX = oneTextureForAllWidgets ? widget.getPosition().getEffectiveX() : 0;
                        int offsetY = oneTextureForAllWidgets ? widget.getPosition().getEffectiveY() : 0;
                        
                        widget.clearRegion( texture, offsetX, offsetY );
                    }
                }
            }
            catch ( Throwable t )
            {
                RFDHLog.exception( t );
            }
        }
        
        if ( !isEditorMode )
        {
            for ( int i = 0; i < n; i++ )
            {
                Widget widget = widgetsConfig.getWidget( i );
                TextureImage2D texture = getMainTexture( i );
                
                if ( texture != null )
                    texture.getTextureCanvas().setClip( 0, 0, texture.getWidth(), texture.getHeight() );
                
                try
                {
                    if ( isWidgetReady( widget, hasWaitingWidgets, gameData ) )
                    {
                        if ( widget.isVisible() )
                        {
                            WidgetController controller = widget.getWidgetController();
                            
                            if ( controller != null )
                                __GDPrivilegedAccess.setControlledVSIs( gameData.getScoringInfo(), __WPrivilegedAccess.getControlledViewedVSI( controller ), __WPrivilegedAccess.getControlledCompareVSI( controller ) );
                            else
                                __GDPrivilegedAccess.setControlledVSIs( gameData.getScoringInfo(), null, null );
                            
                            widget.drawWidget( clock, completeRedrawForced, gameData, isEditorMode, texture, !oneTextureForAllWidgets );
                        }
                    }
                }
                catch ( Throwable t )
                {
                    RFDHLog.exception( t );
                }
            }
        }
        
        __GDPrivilegedAccess.setControlledVSIs( gameData.getScoringInfo(), null, null );
    }
    
    /**
     * Creates a new {@link WidgetsDrawingManager}.
     * 
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     * @param gameResX the game x-resolution (current viewport)
     * @param gameResY the game y-resolution (current viewport)
     */
    public WidgetsDrawingManager( boolean isEditorMode, int gameResX, int gameResY )
    {
        this.oneTextureForAllWidgets = isEditorMode;
        
        if ( oneTextureForAllWidgets )
            this.textures = new TransformableTexture[] { TransformableTexture.createMainTexture( gameResX, gameResY, false ) };
        else
            this.textures = new TransformableTexture[] {};
        
        this.widgetsConfig = new WidgetsConfiguration( gameResX, gameResY );
    }
}
