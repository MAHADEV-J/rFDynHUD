/**
 * Copyright (C) 2009-2010 Cars and Tracks Development Project (CTDP).
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
package net.ctdp.rfdynhud.editor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JPanel;

import net.ctdp.rfdynhud.gamedata.GameResolution;
import net.ctdp.rfdynhud.gamedata.LiveGameData;
import net.ctdp.rfdynhud.properties.BooleanProperty;
import net.ctdp.rfdynhud.properties.IntProperty;
import net.ctdp.rfdynhud.properties.PropertyLoader;
import net.ctdp.rfdynhud.properties.WidgetPropertiesContainer;
import net.ctdp.rfdynhud.render.TextureDirtyRectsManager;
import net.ctdp.rfdynhud.render.TextureImage2D;
import net.ctdp.rfdynhud.render.WidgetsDrawingManager;
import net.ctdp.rfdynhud.util.Logger;
import net.ctdp.rfdynhud.util.WidgetsConfigurationWriter;
import net.ctdp.rfdynhud.widgets.__WCPrivilegedAccess;
import net.ctdp.rfdynhud.widgets.widget.Widget;

import org.openmali.types.twodee.Rect2i;

/**
 * 
 * @author Marvin Froehlich (CTDP)
 */
public class EditorPanel extends JPanel
{
    private static final long serialVersionUID = -4217992603083635127L;
    
    private final RFDynHUDEditor editor;
    
    private LiveGameData gameData;
    
    private TextureImage2D overlay;
    private final WidgetsDrawingManager drawingManager;
    private final GameResolution gameResolution;
    private final ByteBuffer dirtyRectsBuffer = TextureDirtyRectsManager.createByteBuffer( 1024 );
    private final ArrayList<Boolean> dirtyFlags = new ArrayList<Boolean>();
    private final HashMap<Widget, Rect2i> oldWidgetRects = new HashMap<Widget, Rect2i>();
    
    private Widget selectedWidget = null;
    private static final java.awt.Color SELECTION_COLOR = new java.awt.Color( 255, 0, 0, 127 );
    
    private final ArrayList<WidgetSelectionListener> selectionListeners = new ArrayList<WidgetSelectionListener>();
    
    private BufferedImage backgroundImage;
    private BufferedImage cacheImage;
    private Graphics2D cacheGraphics;
    
    private boolean bgImageReloadSuppressed = false;
    
    public void setBGImageReloadSuppressed( boolean suppressed )
    {
        this.bgImageReloadSuppressed = suppressed;
    }
    
    private final IntProperty railDistanceX = new IntProperty( (Widget)null, "railDistanceX", 10, 0, 100 );
    private final IntProperty railDistanceY = new IntProperty( (Widget)null, "railDistanceY", 10, 0, 100 );
    
    private final BooleanProperty drawGrid = new BooleanProperty( (Widget)null, "drawGrid", false )
    {
        @Override
        protected void onValueChanged( boolean newValue )
        {
            if ( !bgImageReloadSuppressed && ( drawingManager != null ) )
                setBackgroundImage( editor.loadBackgroundImage( gameResolution.getViewportWidth(), gameResolution.getViewportHeight() ) );
        }
    };
    
    private final IntProperty gridOffsetX = new IntProperty( (Widget)null, "gridOffsetX", 0 )
    {
        @Override
        protected void onValueChanged( int oldValue, int newValue )
        {
            if ( !bgImageReloadSuppressed && ( drawingManager != null ) )
                setBackgroundImage( editor.loadBackgroundImage( gameResolution.getViewportWidth(), gameResolution.getViewportHeight() ) );
        }
    };
    
    private final IntProperty gridOffsetY = new IntProperty( (Widget)null, "gridOffsetY", 0 )
    {
        @Override
        protected void onValueChanged( int oldValue, int newValue )
        {
            if ( !bgImageReloadSuppressed && ( drawingManager != null ) )
                setBackgroundImage( editor.loadBackgroundImage( gameResolution.getViewportWidth(), gameResolution.getViewportHeight() ) );
        }
    };
    
    private final IntProperty gridSizeX = new IntProperty((Widget) null, "gridSizeX", 10, 0, 5000 )
    {
        @Override
        protected void onValueChanged( int oldValue, int newValue )
        {
            if ( !bgImageReloadSuppressed && ( drawingManager != null ) )
                setBackgroundImage( editor.loadBackgroundImage( gameResolution.getViewportWidth(), gameResolution.getViewportHeight() ) );
        }
    };
    
    private final IntProperty gridSizeY = new IntProperty( (Widget)null, "gridSizeY", 10, 0, 5000 )
    {
        @Override
        protected void onValueChanged( int oldValue, int newValue )
        {
            if ( !bgImageReloadSuppressed && ( drawingManager != null ) )
                setBackgroundImage( editor.loadBackgroundImage( gameResolution.getViewportWidth(), gameResolution.getViewportHeight() ) );
        }
    };
    
    public void addWidgetSelectionListener( WidgetSelectionListener l )
    {
        selectionListeners.add( l );
    }
    
    public void removeWidgetSelectionListener( WidgetSelectionListener l )
    {
        selectionListeners.remove( l );
    }
    
    public void setDrawGrid( boolean drawGrid )
    {
        this.drawGrid.setBooleanValue( drawGrid );
    }
    
    public final int getRailDistanceX()
    {
        return ( railDistanceX.getIntValue() );
    }
    
    public final int getRailDistanceY()
    {
        return ( railDistanceY.getIntValue() );
    }
    
    public final boolean getDrawGrid()
    {
        return ( drawGrid.getBooleanValue() );
    }
    
    public final int getGridOffsetX()
    {
        return ( gridOffsetX.getIntValue() );
    }
    
    public final int getGridOffsetY()
    {
        return ( gridOffsetY.getIntValue() );
    }
    
    public final int getGridSizeX()
    {
        return ( gridSizeX.getIntValue() );
    }
    
    public final int getGridSizeY()
    {
        return ( gridSizeY.getIntValue() );
    }
    
    public void getProperties( WidgetPropertiesContainer propsCont )
    {
        propsCont.addProperty( railDistanceX );
        propsCont.addProperty( railDistanceY );
        propsCont.addProperty( drawGrid );
        propsCont.addProperty( gridOffsetX );
        propsCont.addProperty( gridOffsetY );
        propsCont.addProperty( gridSizeX );
        propsCont.addProperty( gridSizeY );
    }
    
    public void saveProperties( WidgetsConfigurationWriter writer ) throws IOException
    {
        writer.writeProperty( railDistanceX, null );
        writer.writeProperty( railDistanceY, null );
        writer.writeProperty( drawGrid, null );
        writer.writeProperty( gridOffsetX, null );
        writer.writeProperty( gridOffsetY, null );
        writer.writeProperty( gridSizeX, null );
        writer.writeProperty( gridSizeY, null );
    }
    
    public void loadProperty( PropertyLoader loader )
    {
        bgImageReloadSuppressed = true;
        
        if ( loader.loadProperty( railDistanceX ) );
        else if ( loader.loadProperty( railDistanceY ) );
        else if ( loader.loadProperty( drawGrid ) );
        else if ( loader.loadProperty( gridOffsetX ) );
        else if ( loader.loadProperty( gridOffsetY ) );
        else if ( loader.loadProperty( gridSizeX ) );
        else if ( loader.loadProperty( gridSizeY ) );
        
        bgImageReloadSuppressed = false;
    }
    
    public final boolean isGridUsed()
    {
        return ( drawGrid.getBooleanValue() && ( this.gridSizeX.getIntValue() > 1 ) && ( this.gridSizeY.getIntValue() > 1 ) );
    }
    
    public final int snapXToGrid( int x )
    {
        if ( !isGridUsed() )
            return ( x );
        
        return ( gridOffsetX.getIntValue() + Math.min( Math.round( ( x - gridOffsetX.getIntValue() ) / (float)gridSizeX.getIntValue() ) * gridSizeX.getIntValue(), drawingManager.getGameResolution().getViewportWidth() - 1 ) );
    }
    
    public final int snapYToGrid( int y )
    {
        if ( !isGridUsed() )
            return ( y );
        
        return ( gridOffsetY.getIntValue() + Math.min( Math.round( ( y - gridOffsetY.getIntValue() ) / (float)gridSizeY.getIntValue() ) * gridSizeY.getIntValue(), drawingManager.getGameResolution().getViewportHeight() - 1 ) );
    }
    
    public void snapWidgetToGrid( Widget widget )
    {
        if ( !isGridUsed() )
            return;
        
        int x = widget.getPosition().getEffectiveX();
        int y = widget.getPosition().getEffectiveY();
        int w = widget.getSize().getEffectiveWidth();
        int h = widget.getSize().getEffectiveHeight();
        
        x = snapXToGrid( x );
        y = snapYToGrid( y );
        w = snapXToGrid( x + w - 1 ) + 1 - x;
        h = snapYToGrid( y + h - 1 ) + 1 - y;
        
        widget.getSize().setEffectiveSize( w, h );
        widget.getPosition().setEffectivePosition( x, y );
    }
    
    public void snapAllWidgetsToGrid()
    {
        final int n = drawingManager.getNumWidgets();
        Widget[] widgets = new Widget[ n ];
        for ( int i = 0; i < n; i++ )
            widgets[i] = drawingManager.getWidget( i );
        
        for ( int i = 0; i < n; i++ )
            snapWidgetToGrid( widgets[i] );
    }
    
    private void drawGrid()
    {
        if ( !isGridUsed() )
            return;
        
        Graphics2D g2 = backgroundImage.createGraphics();
        g2.setColor( Color.BLACK );
        
        final int gridOffsetX = this.gridOffsetX.getIntValue();
        final int gridOffsetY = this.gridOffsetY.getIntValue();
        final int gridSizeX = this.gridSizeX.getIntValue();
        final int gridSizeY = this.gridSizeY.getIntValue();
        final int gameResX = drawingManager.getGameResolution().getViewportWidth();
        final int gameResY = drawingManager.getGameResolution().getViewportHeight();
        
        for ( int x = gridSizeX - 1 + gridOffsetX; x < gameResX - gridOffsetX; x += gridSizeX )
        {
            for ( int y = gridSizeY - 1 + gridOffsetY; y < gameResY - gridOffsetY; y += gridSizeY )
            {
                g2.drawLine( x, y, x, y );
            }
        }
    }
    
    public void setBackgroundImage( BufferedImage image )
    {
        this.backgroundImage = image;
        
        if ( ( cacheImage == null ) || ( cacheImage.getWidth() != backgroundImage.getWidth() ) || ( cacheImage.getHeight() != backgroundImage.getHeight() ) )
        {
            cacheImage = new BufferedImage( backgroundImage.getWidth(), backgroundImage.getHeight(), BufferedImage.TYPE_3BYTE_BGR );
            cacheGraphics = cacheImage.createGraphics();
        }
        
        drawGrid();
        
        cacheGraphics.drawImage( backgroundImage, 0, 0, null );
        
        drawingManager.setAllDirtyFlags();
        oldWidgetRects.clear();
    }
    
    public final BufferedImage getBackgroundImage()
    {
        return ( backgroundImage );
    }
    
    public void setOverlayTexture( TextureImage2D texture )
    {
        this.overlay = texture;
    }
    
    public final TextureImage2D getOverlayTexture()
    {
        return ( overlay );
    }
    
    public void setSelectedWidget( Widget widget, boolean doubleClick )
    {
        boolean selectionChanged = ( widget != this.selectedWidget );
        
        if ( selectionChanged )
        {
            if ( ( this.selectedWidget != null ) && ( this.selectedWidget.getConfiguration() != null ) )
            {
                //selectedWidget.clearRegion( true, overlay );
            }
            
            this.selectedWidget = widget;
            
            this.repaint();
        }
        
        for ( int i = 0; i < selectionListeners.size(); i++ )
        {
            selectionListeners.get( i ).onWidgetSelected( selectedWidget, selectionChanged, doubleClick );
        }
        
        if ( selectionChanged )
        {
            drawingManager.setAllDirtyFlags();
        }
    }
    
    public final Widget getSelectedWidget()
    {
        return ( selectedWidget );
    }
    
    public void removeSelectedWidget()
    {
        if ( selectedWidget == null )
            return;
        
        Logger.log( "Removing selected Widget of type \"" + selectedWidget.getClass().getName() + "\" and name \"" + selectedWidget.getName() + "\"..." );
        
        selectedWidget.clearRegion( true, overlay );
        __WCPrivilegedAccess.removeWidget( drawingManager, selectedWidget );
        setSelectedWidget( null, false );
        editor.setDirtyFlag();
    }
    
    private void drawSelection( Widget widget, Graphics2D g )
    {
        int offsetX = widget.getPosition().getEffectiveX();
        int offsetY = widget.getPosition().getEffectiveY();
        int width = widget.getSize().getEffectiveWidth();
        int height = widget.getSize().getEffectiveHeight();
        
        //texture.getTextureCanvas().setClip( offsetX, offsetY, width, height );
        
        Stroke oldStroke = g.getStroke();
        g.setStroke( new BasicStroke( 2 ) );
        g.setColor( SELECTION_COLOR );
        g.drawRect( offsetX, offsetY, width, height );
        g.setStroke( oldStroke );
        //texture.clearOutline( SELECTION_COLOR, offsetX, offsetY, width, height, 2, false, null );
    }
    
    public void clearWidgetRegion( Widget widget )
    {
        if ( widget == null )
            return;
        
        widget.clearRegion( true, overlay );
    }
    
    public void clearSelectedWidgetRegion()
    {
        clearWidgetRegion( selectedWidget );
    }
    
    private long frameIndex = 0;
    
    @Override
    public void repaint()
    {
        super.repaint();
    }
    
    public void drawWidgets( Graphics2D g2, boolean drawEverything, boolean drawSelection )
    {
        HashMap<Widget, Rect2i> oldWidgetRects = this.oldWidgetRects;
        
        if ( drawEverything )
        {
            oldWidgetRects = new HashMap<Widget, Rect2i>();
            
            drawingManager.setAllDirtyFlags();
        }
        
        frameIndex++;
        
        try
        {
            int n = drawingManager.getNumWidgets();
            
            //g.drawImage( backgroundImage, 0, 0, backgroundImage.getWidth(), backgroundImage.getHeight(), 0, 0, backgroundImage.getWidth(), backgroundImage.getHeight(), null );
            
            dirtyFlags.clear();
            for ( int i = 0; i < n; i++ )
            {
                dirtyFlags.add( drawingManager.getWidget( i ).getDirtyFlag( false ) );
            }
            for ( int i = 0; i < n; i++ )
            {
                if ( dirtyFlags.get( i ) )
                {
                    Widget widget = drawingManager.getWidget( i );
                    Rect2i r = new Rect2i( widget.getPosition().getEffectiveX(), widget.getPosition().getEffectiveY(), widget.getSize().getEffectiveWidth(), widget.getSize().getEffectiveHeight() );
                    Rect2i oldWidgetRect = oldWidgetRects.get( widget );
                    if ( oldWidgetRect != null )
                    {
                        r.combine( oldWidgetRect );
                    }
                    
                    for ( int j = 0; j < n; j++ )
                    {
                        Widget widget2 = drawingManager.getWidget( j );
                        Rect2i r2 = new Rect2i( widget2.getPosition().getEffectiveX(), widget2.getPosition().getEffectiveY(), widget2.getSize().getEffectiveWidth(), widget2.getSize().getEffectiveHeight() );
                        
                        if ( !dirtyFlags.get( j ) && r.intersects( r2 ) )
                        {
                            dirtyFlags.set( j, true );
                            widget2.setDirtyFlag();
                        }
                    }
                }
            }
            
            drawingManager.drawWidgets( gameData, editor.getEditorPresets(), true, overlay );
            //TextureDirtyRectsManager.drawDirtyRects( overlay );
            TextureDirtyRectsManager.getDirtyRects( frameIndex, overlay, dirtyRectsBuffer, true );
            
            short numDirtyRects = dirtyRectsBuffer.getShort();
            for ( short i = 0; i < numDirtyRects; i++ )
            {
                int drX = dirtyRectsBuffer.getShort();
                int drY = dirtyRectsBuffer.getShort();
                int drW = dirtyRectsBuffer.getShort();
                int drH = dirtyRectsBuffer.getShort();
                
                cacheGraphics.drawImage( backgroundImage, drX, drY, drX + drW, drY + drH, drX, drY, drX + drW, drY + drH, null );
            }
            
            BufferedImage bi = overlay.getBufferedImage();
            for ( int i = 0; i < n; i++ )
            {
                Widget widget = drawingManager.getWidget( i );
                
                int offsetX = widget.getPosition().getEffectiveX();
                int offsetY = widget.getPosition().getEffectiveY();
                int width = widget.getSize().getEffectiveWidth();
                int height = widget.getSize().getEffectiveHeight();
                
                if ( dirtyFlags.get( i ) )
                {
                    //cacheGraphics.drawImage( backgroundImage, offsetX, offsetY, offsetX + width, offsetY + height, offsetX, offsetY, offsetX + width, offsetY + height, null );
                    cacheGraphics.drawImage( bi, offsetX, offsetY, offsetX + width, offsetY + height, offsetX, offsetY, offsetX + width, offsetY + height, null );
                }
                
                Rect2i oldWidgetRect = oldWidgetRects.get( widget );
                if ( oldWidgetRect == null )
                {
                    oldWidgetRect = new Rect2i();
                    oldWidgetRects.put( widget, oldWidgetRect );
                }
                oldWidgetRect.set( offsetX, offsetY, width, height );
            }
            
            g2.drawImage( cacheImage, 0, 0, cacheImage.getWidth(), cacheImage.getHeight(), 0, 0, cacheImage.getWidth(), cacheImage.getHeight(), null );
            
            if ( drawSelection && ( selectedWidget != null ) )
            {
                drawSelection( selectedWidget, g2 );
            }
        }
        catch ( Throwable t )
        {
            Logger.log( t );
        }
    }
    
    @Override
    public void paintComponent( Graphics g )
    {
        //super.paintComponent( g );
        
        //System.out.println( "paintComponent()" );
        
        drawWidgets( (Graphics2D)g, false, true );
    }
    
    public EditorPanel( RFDynHUDEditor editor, LiveGameData gameData, TextureImage2D overlay, WidgetsDrawingManager drawingManager )
    {
        super();
        
        this.editor = editor;
        
        this.gameData = gameData;
        
        this.overlay = overlay;
        this.drawingManager = drawingManager;
        this.gameResolution = drawingManager.getGameResolution();
        
        EditorPanelInputHandler inputHandler = new EditorPanelInputHandler( editor, drawingManager );
        this.addMouseListener( inputHandler );
        this.addMouseMotionListener( inputHandler );
        this.addKeyListener( inputHandler );
        this.setFocusable( true );
    }
}
