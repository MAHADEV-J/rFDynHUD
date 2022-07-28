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
package net.ctdp.rfdynhud.widgets.base.widget;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import net.ctdp.rfdynhud.editor.__EDPrivilegedAccess;
import net.ctdp.rfdynhud.gamedata.LiveGameData;
import net.ctdp.rfdynhud.gamedata.ScoringInfo;
import net.ctdp.rfdynhud.gamedata.SessionType;
import net.ctdp.rfdynhud.gamedata.VehicleScoringInfo;
import net.ctdp.rfdynhud.gamedata.VehicleSetup;
import net.ctdp.rfdynhud.input.InputAction;
import net.ctdp.rfdynhud.properties.AbstractPropertiesKeeper;
import net.ctdp.rfdynhud.properties.BackgroundProperty;
import net.ctdp.rfdynhud.properties.BooleanProperty;
import net.ctdp.rfdynhud.properties.BorderProperty;
import net.ctdp.rfdynhud.properties.ColorProperty;
import net.ctdp.rfdynhud.properties.FlatPropertiesContainer;
import net.ctdp.rfdynhud.properties.FontProperty;
import net.ctdp.rfdynhud.properties.GenericPropertiesIterator;
import net.ctdp.rfdynhud.properties.IntProperty;
import net.ctdp.rfdynhud.properties.PosSizeProperty;
import net.ctdp.rfdynhud.properties.Position;
import net.ctdp.rfdynhud.properties.PropertiesContainer;
import net.ctdp.rfdynhud.properties.PropertiesKeeper;
import net.ctdp.rfdynhud.properties.Property;
import net.ctdp.rfdynhud.properties.PropertyLoader;
import net.ctdp.rfdynhud.properties.Size;
import net.ctdp.rfdynhud.properties.StringProperty;
import net.ctdp.rfdynhud.render.BorderWrapper;
import net.ctdp.rfdynhud.render.DrawnString;
import net.ctdp.rfdynhud.render.DrawnStringFactory;
import net.ctdp.rfdynhud.render.Texture2DCanvas;
import net.ctdp.rfdynhud.render.TextureImage2D;
import net.ctdp.rfdynhud.render.TransformableTexture;
import net.ctdp.rfdynhud.render.__RenderPrivilegedAccess;
import net.ctdp.rfdynhud.util.PropertyWriter;
import net.ctdp.rfdynhud.util.SubTextureCollector;
import net.ctdp.rfdynhud.util.__UtilPrivilegedAccess;
import net.ctdp.rfdynhud.valuemanagers.Clock;
import net.ctdp.rfdynhud.values.GenericPositionsIterator;
import net.ctdp.rfdynhud.values.GenericSizesIterator;
import net.ctdp.rfdynhud.values.InnerSize;
import net.ctdp.rfdynhud.values.RelativePositioning;
import net.ctdp.rfdynhud.widgets.WidgetsConfiguration;
import net.ctdp.rfdynhud.widgets.__WCPrivilegedAccess;

import org.jagatoo.logging.Log;
import org.jagatoo.logging.LogChannel;
import org.jagatoo.logging.LogLevel;
import org.jagatoo.util.classes.ClassUtil;
import org.jagatoo.util.strings.StringUtils;
import org.openmali.types.twodee.Rect2i;

/**
 * This is the base for all Widgets to be drawn on the HUD.<br>
 * Any concrete extension must have a parameterless constructor.
 * 
 * @author Marvin Froehlich (CTDP)
 */
public abstract class Widget implements Cloneable, PropertiesKeeper
{
    public static final LogChannel LOG_CHANNEL = new LogChannel( "rfDynHUD-Widget" );
    
    public static final int NEEDED_DATA_TELEMETRY = 1;
    public static final int NEEDED_DATA_SCORING = 2;
    //public static final int NEEDED_DATA_SETUP = 4;
    public static final int NEEDED_DATA_ALL = NEEDED_DATA_TELEMETRY | NEEDED_DATA_SCORING/* | NEEDED_DATA_SETUP*/;
    
    private final WidgetSet widgetSet;
    private final WidgetPackage widgetPackage;
    
    private WidgetsConfiguration config = null;
    
    private boolean dirtyFlag = true;
    
    private final StringProperty type = new StringProperty( "type", this.getClass().getSimpleName(), true );
    private final StringProperty name = new StringProperty( "name", this.getClass().getSimpleName() + "1" );
    
    private WidgetController controller = null;
    
    private final BooleanProperty inputVisible = new BooleanProperty( "initialVisibility", true );
    private boolean autoVisible = true;
    private boolean updatedVisible = true;
    private boolean visibilityChangedSinceLastDraw = true;
    private boolean needsCompleteRedraw = true;
    private boolean needsCompleteClear = false;
    
    private boolean initialized = false;
    
    private final Position position;
    private final Property positioningProperty;
    private final PosSizeProperty xProperty;
    private final PosSizeProperty yProperty;
    private final Size size;
    private final PosSizeProperty widthProperty;
    private final PosSizeProperty heightProperty;
    private final InnerSize innerSize;
    private final IntProperty zIndex = new IntProperty( "zIndex", 0 );
    
    private final IntProperty paddingTop = new IntProperty( "paddingTop", "top", 0, 0, 1000, false );
    private final IntProperty paddingLeft = new IntProperty( "paddingLeft", "left", 0, 0, 1000, false );
    private final IntProperty paddingRight = new IntProperty( "paddingRight", "right", 0, 0, 1000, false );
    private final IntProperty paddingBottom = new IntProperty( "paddingBottom", "bottom", 0, 0, 1000, false );
    
    private final BorderProperty border = new BorderProperty( "border", BorderProperty.DEFAULT_BORDER.getKey(), paddingTop, paddingLeft, paddingRight, paddingBottom );
    
    /**
     * Gets the initial value for the background property.
     * 
     * @return the initial value for the background property.
     */
    protected String getInitialBackground()
    {
        return ( BackgroundProperty.COLOR_INDICATOR + ColorProperty.STANDARD_BACKGROUND_COLOR.getKey() );
    }
    
    final boolean overridesDrawBackground = ClassUtil.overridesMethod( Widget.class, this.getClass(), "drawBackground", LiveGameData.class, boolean.class, TextureImage2D.class, int.class, int.class, int.class, int.class, boolean.class );
    
    /**
     * This method is invoked when the background has changed.
     * 
     * @param imageChanged whether the image has changed
     * @param deltaScaleX the x-scale factor in as a difference between the old background image and the new one or -1 of no background image was selected
     * @param deltaScaleY the y-scale factor in as a difference between the old background image and the new one or -1 of no background image was selected
     */
    protected void onBackgroundChanged( boolean imageChanged, float deltaScaleX, float deltaScaleY ) {}
    private final BackgroundProperty backgroundProperty = canHaveBackground() || overridesDrawBackground ? new BackgroundProperty( "background", getInitialBackground() )
    {
        @Override
        protected void onValueChanged( BackgroundType oldBGType, BackgroundType newBGType, String oldValue, String newValue )
        {
            if ( ( background != null ) && ( getConfiguration() != null ) )
                background.onPropertyValueChanged( Widget.this, oldBGType, newBGType, oldValue, newValue );
        }
    } : null;
    private final WidgetBackground background = canHaveBackground() || overridesDrawBackground ? new WidgetBackground( this, backgroundProperty ) : null;
    
    private final FontProperty font = new FontProperty( "font", FontProperty.STANDARD_FONT.getKey() );
    private final ColorProperty fontColor = new ColorProperty( "fontColor", ColorProperty.STANDARD_FONT_COLOR.getKey() );
    
    private TransformableTexture[] subTextures = null;
    
    private final DrawnStringFactory drawnStringFactory = new DrawnStringFactory( this );
    
    private AbstractAssembledWidget masterWidget = null;
    
    /**
     * Logs data to the plugin's log file.
     * 
     * @param logLevel the log level
     * @param data the data to log
     */
    protected final void log( LogLevel logLevel, Object... data )
    {
        Log.println( LOG_CHANNEL, logLevel, data );
    }
    
    /**
     * Logs data to the plugin's log file.
     * 
     * @param logLevel the log level
     * @param data the data to log (comma separated)
     */
    protected final void logCS( LogLevel logLevel, Object... data )
    {
        Log.printlnCS( LOG_CHANNEL, logLevel, data );
    }
    
    /**
     * Logs data to the plugin's log file.
     * 
     * @param data the data to log
     */
    protected final void log( Object... data )
    {
        Log.println( LOG_CHANNEL, data );
    }
    
    /**
     * Logs data to the plugin's log file.
     * 
     * @param data the data to log (comma separated)
     */
    protected final void logCS( Object... data )
    {
        Log.printlnCS( LOG_CHANNEL, data );
    }
    
    /**
     * Logs data to the plugin's log file.
     * 
     * @param data the data to log
     */
    protected final void debug( Object... data )
    {
        Log.debug( LOG_CHANNEL, data );
    }
    
    /**
     * Logs data to the plugin's log file.
     * 
     * @param data the data to log (comma separated)
     */
    protected final void debugCS( Object... data )
    {
        Log.debugCS( LOG_CHANNEL, data );
    }
    
    public void setWidgetController( WidgetController controller )
    {
        this.controller = controller;
    }
    
    public final WidgetController getWidgetController()
    {
        return ( controller );
    }
    
    protected void onVisibilityChanged( boolean visible )
    {
        if ( visible )
            this.needsCompleteRedraw = true;
        else
            this.needsCompleteClear = true;
        
        this.visibilityChangedSinceLastDraw = true;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onPropertyChanged( Property property, Object oldValue, Object newValue )
    {
        forceAndSetDirty( true );
        
        if ( __EDPrivilegedAccess.editorClassLoader != null )
        {
            if ( ( property == zIndex ) && ( getConfiguration() != null ) )
            {
                __WCPrivilegedAccess.sortWidgets( getConfiguration() );
            }
        }
    }
    
    /**
     * 
     * @param oldPositioning the old value for the positioning
     * @param oldX the old x
     * @param oldY the old y
     * @param newPositioning the new value for the positioning
     * @param newX the new x
     * @param newY the new y
     */
    protected void onPositionChanged( RelativePositioning oldPositioning, int oldX, int oldY, RelativePositioning newPositioning, int newX, int newY )
    {
        if ( __EDPrivilegedAccess.editorClassLoader != null )
        {
            if ( getMasterWidget() == null )
            {
                WidgetsConfiguration wc = getConfiguration();
                
                if ( wc != null )
                {
                    __WCPrivilegedAccess.sortWidgets( wc );
                }
            }
            else
            {
                getMasterWidget().sortParts();
                
                if ( getMasterWidget().getBackground() != null )
                    getMasterWidget().getBackground().setMergedBGDirty();
            }
        }
    }
    
    /**
     * 
     * @param oldWidth the old width
     * @param oldHeight the old height
     * @param newWidth the new width
     * @param newHeight the new height
     */
    protected void onSizeChanged( int oldWidth, int oldHeight, int newWidth, int newHeight )
    {
        if ( __EDPrivilegedAccess.editorClassLoader != null )
        {
            if ( getMasterWidget() == null )
            {
                WidgetsConfiguration wc = getConfiguration();
                
                if ( wc != null )
                {
                    __WCPrivilegedAccess.sortWidgets( wc );
                }
            }
            else
            {
                getMasterWidget().sortParts();
                
                if ( getMasterWidget().getBackground() != null )
                    getMasterWidget().getBackground().setMergedBGDirty();
            }
        }
        
        if ( getBackground() != null )
        {
            getBackground().onWidgetSizeChanged();
        }
    }
    
    protected void onCanvasSizeChanged()
    {
        forceAndSetDirty( true );
        
        if ( getBackground() != null )
        {
            getBackground().onWidgetSizeChanged();
        }
        
        FlatPropertiesContainer propsCont = new FlatPropertiesContainer();
        getProperties( propsCont, true );
        
        for ( int i = 0; i < propsCont.getList().size(); i++ )
        {
            if ( propsCont.getList().get( i ) instanceof FontProperty )
                ( (FontProperty)propsCont.getList().get( i ) ).refresh();
        }
    }
    
    /**
     * Gets the {@link WidgetSet} this {@link Widget} belongs to.
     * 
     * @return the {@link WidgetSet} this {@link Widget} belongs to.
     */
    public final WidgetSet getWidgetSet()
    {
        return ( widgetSet );
    }
    
    /**
     * Gets the package to group the Widget in the editor.
     * This can be an <code>null</code> to be displayed in the root or a slash separated path.
     * 
     * @return the package to group the Widget in the editor.
     */
    public final WidgetPackage getWidgetPackage()
    {
        return ( widgetPackage );
    }
    
    /**
     * Gets the default value for the given border alias/name.
     * 
     * @param name the border name to query
     * 
     * @return the default value for the given border alias/name.
     */
    public String getDefaultBorderValue( String name )
    {
        String result = BorderProperty.getDefaultBorderValue( name );
        
        if ( ( result == null ) && ( getWidgetSet() != null ) )
        {
            result = getWidgetSet().getDefaultBorderValue( name );
        }
        
        return ( result );
    }
    
    /**
     * Gets the default value for the given named color.
     * 
     * @param name the color name to query
     * 
     * @return the default value for the given named color.
     */
    public String getDefaultNamedColorValue( String name )
    {
        String result = ColorProperty.getDefaultNamedColorValue( name );
        
        if ( ( result == null ) && ( getWidgetSet() != null ) )
        {
            result = getWidgetSet().getDefaultNamedColorValue( name );
        }
        
        return ( result );
    }
    
    /**
     * Gets the default value for the given named font.
     * 
     * @param name the font name to query
     * 
     * @return the default value for the given named font.
     */
    public String getDefaultNamedFontValue( String name )
    {
        String result = FontProperty.getDefaultNamedFontValue( name );
        
        if ( ( result == null ) && ( getWidgetSet() != null ) )
        {
            result = getWidgetSet().getDefaultNamedFontValue( name );
        }
        
        return ( result );
    }
    
    /**
     * This event is called after this Widget was added to its {@link WidgetsConfiguration}.
     * 
     * @param config
     * @param gameData
     */
    protected void onWidgetAttached( WidgetsConfiguration config, LiveGameData gameData )
    {
    }
    
    /**
     * This event is called after this Widget was removed from its {@link WidgetsConfiguration}.
     * 
     * @param config
     * @param gameData
     */
    protected void onWidgetDetached( WidgetsConfiguration config, LiveGameData gameData )
    {
    }
    
    void setConfiguration( WidgetsConfiguration config, LiveGameData gameData )
    {
        if ( config == this.config )
            return;
        
        WidgetsConfiguration oldConfig = this.config;
        
        this.config = config;
        
        if ( this.config != null )
        {
            FlatPropertiesContainer pc = new FlatPropertiesContainer();
            
            getProperties( pc, true );
            List<Property> list = pc.getList();
            
            for ( int i = 0; i < list.size(); i++ )
            {
                if ( list.get( i ) != null )
                    AbstractPropertiesKeeper.setKeeper( list.get( i ), this );
            }
        }
        
        if ( config == null )
            onWidgetDetached( oldConfig, gameData );
        else
            onWidgetAttached( oldConfig, gameData );
    }
    
    /**
     * Gets the {@link WidgetsConfiguration}, this {@link Widget} is a member of.
     * 
     * @return the {@link WidgetsConfiguration}, this {@link Widget} is a member of.
     */
    public final WidgetsConfiguration getConfiguration()
    {
        return ( config );
    }
    
    void setMasterWidget( AbstractAssembledWidget masterWidget )
    {
        this.masterWidget = masterWidget;
    }
    
    /**
     * If this {@link Widget} is part of an {@link AbstractAssembledWidget}, this master {@link Widget} is returned.
     * 
     * @return the master {@link AbstractAssembledWidget} or <code>null</code>.
     */
    public final AbstractAssembledWidget getMasterWidget()
    {
        return ( masterWidget );
    }
    
    /**
     * Gets the InputActions, that can be bound with a Widget of this type.
     * "Override" this method to return your own custom actions.
     * 
     * @return the InputActions, that can be bound with a Widget of this type.
     */
    public InputAction[] getInputActions()
    {
        return ( null );
    }
    
    /**
     * Gets the {@link TransformableTexture}s, that this {@link Widget} keeps.
     * 
     * @param gameData the live game data
     * @param isEditorMode rendering in the editor?
     * @param widgetInnerWidth the total widget width excluding borders
     * @param widgetInnerHeight the total widget height excluding borders
     * @param collector the collector to collect all the sub textures
     */
    protected abstract void initSubTextures( LiveGameData gameData, boolean isEditorMode, int widgetInnerWidth, int widgetInnerHeight, SubTextureCollector collector );
    
    /**
     * Gets the {@link TransformableTexture}s, that this {@link Widget} keeps.
     * 
     * @param gameData the live game data
     * @param isEditorMode rendering in the editor?
     * @param widgetInnerWidth the total widget width excluding borders
     * @param widgetInnerHeight the total widget height excluding borders
     * 
     * @return the {@link TransformableTexture}s, that this {@link Widget} keeps or null for no textures.
     */
    public final TransformableTexture[] getSubTextures( LiveGameData gameData, boolean isEditorMode, int widgetInnerWidth, int widgetInnerHeight )
    {
        if ( !initialized )
        {
            SubTextureCollector collector = new SubTextureCollector();
            
            initSubTextures( gameData, isEditorMode, widgetInnerWidth, widgetInnerHeight, collector );
            subTextures = __UtilPrivilegedAccess.getSubTextureArray( collector, true );
            
            if ( subTextures != null )
            {
                for ( int i = 0; i < subTextures.length; i++ )
                {
                    if ( subTextures[i].getOwnerWidget() == null )
                        __RenderPrivilegedAccess.setOwnerWidget( this, subTextures[i] );
                }
            }
        }
        
        return ( subTextures );
    }
    
    protected void onDirtyFlagSet()
    {
    }
    
    void setDirtyFlag( boolean forwardCall )
    {
        boolean changed = !this.dirtyFlag;
        
        this.dirtyFlag = true;
        
        if ( forwardCall && ( masterWidget != null ) )
            masterWidget.setDirtyFlag( false );
        
        if ( changed )
            onDirtyFlagSet();
    }
    
    public void setDirtyFlag()
    {
        this.dirtyFlag = true;
        
        if ( masterWidget != null )
            masterWidget.setDirtyFlag();
    }
    
    public final boolean getDirtyFlag( boolean reset )
    {
        boolean result = dirtyFlag;
        
        if ( reset )
            this.dirtyFlag = false;
        
        return ( result );
    }
    
    protected void onReinitializationForced()
    {
    }
    
    final boolean isInitialized()
    {
        return ( initialized );
    }
    
    void forceReinitialization( boolean forwardCall )
    {
        boolean changed = this.initialized;
        
        this.initialized = false;
        
        if ( forwardCall && ( masterWidget != null ) )
            masterWidget.forceReinitialization( false );
        
        setDirtyFlag();
        
        if ( changed )
            onReinitializationForced();
    }
    
    public final void forceReinitialization()
    {
        forceReinitialization( true );
    }
    
    /**
     * Sets this {@link Widget}'s name.
     * 
     * @param name the new name for this {@link Widget}
     */
    public void setName( String name )
    {
        String oldName = this.name.getStringValue();
        
        this.name.setStringValue( name );
        setDirtyFlag();
        
        if ( getConfiguration() != null )
        {
            __WCPrivilegedAccess.updateNameMapping( this, oldName, getConfiguration() );
        }
    }
    
    /**
     * Gets this {@link Widget}'s name.
     * 
     * @return this {@link Widget}'s name.
     */
    public final String getName()
    {
        return ( name.getStringValue() );
    }
    
    /**
     * Gets the {@link Widget}'s position.
     * 
     * @return the {@link Widget}'s position.
     */
    public final Position getPosition()
    {
        return ( position );
    }
    
    /**
     * Gets the x-offset relative to the master Widget.
     * 
     * @return the x-offset relative to the master Widget.
     */
    public final int getOffsetXToRootMasterWidget()
    {
        if ( getMasterWidget() == null )
            return ( 0 );
        
        return ( getBorder().getInnerLeftWidth() + position.getEffectiveX() + getMasterWidget().getOffsetXToRootMasterWidget() );
    }
    
    /**
     * Gets the y-offset relative to the master Widget.
     * 
     * @return the y-offset relative to the master Widget.
     */
    public final int getOffsetYToRootMasterWidget()
    {
        if ( getMasterWidget() == null )
            return ( 0 );
        
        return ( getBorder().getInnerTopHeight() + position.getEffectiveY() + getMasterWidget().getOffsetYToRootMasterWidget() );
    }
    
    /**
     * Gets the absolute x-position relative to the configuration origin.
     * 
     * @return the absolute x-position relative to the configuration origin.
     */
    public final int getAbsoluteOffsetX()
    {
        int x = position.getEffectiveX();
        
        if ( getMasterWidget() != null )
            x += getMasterWidget().getBorder().getInnerLeftWidth() + getMasterWidget().getAbsoluteOffsetX();
        
        return ( x );
    }
    
    /**
     * Gets the absolute y-position relative to the configuration origin.
     * 
     * @return the absolute y-position relative to the configuration origin.
     */
    public final int getAbsoluteOffsetY()
    {
        int y = position.getEffectiveY();
        
        if ( getMasterWidget() != null )
            y += getMasterWidget().getBorder().getInnerTopHeight() + getMasterWidget().getAbsoluteOffsetY();
        
        return ( y );
    }
    
    /**
     * Sets the {@link Widget}'s z-index relative to other {@link Widget}s in the same configuration.
     * 
     * @param zIndex the new z-index
     */
    public void setZIndex( int zIndex )
    {
        this.zIndex.setIntValue( zIndex );
    }
    
    /**
     * Gets the {@link Widget}'s z-index relative to other {@link Widget}s in the same configuration.
     *  
     * @return the {@link Widget}'s z-index relative to other {@link Widget}s in the same configuration.
     */
    public final int getZIndex()
    {
        return ( zIndex.getIntValue() );
    }
    
    /**
     * Gets, whether this {@link Widget} has a fixed (unmodifiable) size.
     * 
     * @return whether this {@link Widget} has a fixed (unmodifiable) size.
     */
    public boolean hasFixedSize()
    {
        return ( false );
    }
    
    /**
     * Gets this {@link Widget}'s size.
     * 
     * @return this {@link Widget}'s width.
     */
    public final Size getSize()
    {
        return ( size );
    }
    
    /**
     * Gets the inner size of the {@link Widget}.
     * 
     * @return the inner size of the {@link Widget}.
     */
    public final InnerSize getInnerSize()
    {
        return ( innerSize );
    }
    
    /**
     * Gets the result of getSize().getEffectiveWidth().
     * 
     * @return the result of getSize().getEffectiveWidth().
     */
    public final int getEffectiveWidth()
    {
        return ( size.getEffectiveWidth() );
    }
    
    /**
     * Gets the result of getSize().getEffectiveHeight().
     * 
     * @return the result of getSize().getEffectiveHeight().
     */
    public final int getEffectiveHeight()
    {
        return ( size.getEffectiveHeight() );
    }
    
    /**
     * Gets the minimum width for this {@link Widget} in pixels.
     * 
     * @param gameData the live game data
     * @param isEditorMode rendering in the editor?
     * 
     * @return the minimum width for this {@link Widget} in pixels.
     */
    public int getMinWidth( LiveGameData gameData, boolean isEditorMode )
    {
        return ( 25 );
    }
    
    /**
     * Gets the minimum height for this {@link Widget} in pixels.
     * 
     * @param gameData the live game data
     * @param isEditorMode rendering in the editor?
     * 
     * @return the minimum height for this {@link Widget} in pixels.
     */
    public int getMinHeight( LiveGameData gameData, boolean isEditorMode )
    {
        return ( 25 );
    }
    
    /**
     * Gets the maximum width covered by this {@link Widget}.
     * By default this method returns the result of getEffectiveWidth(gameResX).
     * Override this method, if it will change its size during game play.
     * 
     * @param gameData the live game data
     * @param isEditorMode rendering in the editor?
     * 
     * @return the maximum width covered by this {@link Widget}.
     */
    public int getMaxWidth( LiveGameData gameData, boolean isEditorMode )
    {
        return ( size.getEffectiveWidth() );
    }
    
    /**
     * Gets the maximum height covered by this {@link Widget}.
     * By default this method returns the result of getEffectiveHeight(gameResX).
     * Override this method, if it will change its size during game play.
     * 
     * @param gameData the live game data
     * @param isEditorMode rendering in the editor?
     * 
     * @return the maximum height covered by this {@link Widget}.
     */
    public int getMaxHeight( LiveGameData gameData, boolean isEditorMode )
    {
        return ( size.getEffectiveHeight() );
    }
    
    /**
     * Bakes effective position and size to variables, so that they don't need to be recalculated
     * during runtime on each access.
     * 
     * @param convertToPixels if true, all coordinates are converted to absolute pixels and positioned to TOP_LEFT.
     */
    public void bake( boolean convertToPixels )
    {
        Iterator<Position> it1 = new GenericPositionsIterator( this );
        
        while ( it1.hasNext() )
        {
            Position pos = it1.next();
            
            /*
            if ( convertToPixels )
            {
                pos.setXToPixels();
                pos.setYToPixels();
                
                pos.setEffectivePosition( RelativePositioning.TOP_LEFT, pos.getEffectiveX(), pos.getEffectiveY() );
            }
            */
            
            pos.bake();
        }
        
        Iterator<Size> it2 = new GenericSizesIterator( this );
        
        while ( it2.hasNext() )
        {
            Size size = it2.next();
            
            /*
            if ( convertToPixels )
            {
                size.setWidthToPixels();
                size.setHeightToPixels();
            }
            */
            
            size.bake();
        }
    }
    
    public void setAllPosAndSizeToPercents()
    {
        Iterator<Position> it1 = new GenericPositionsIterator( this );
        
        while ( it1.hasNext() )
        {
            Position pos = it1.next();
            pos.setXToPercents();
            pos.setYToPercents();
        }
        
        Iterator<Size> it2 = new GenericSizesIterator( this );
        
        while ( it2.hasNext() )
        {
            Size siz = it2.next();
            siz.setWidthToPercents();
            siz.setHeightToPercents();
        }
    }
    
    public void setAllPosAndSizeToPixels()
    {
        Iterator<Position> it1 = new GenericPositionsIterator( this );
        
        while ( it1.hasNext() )
        {
            Position pos = it1.next();
            pos.setXToPixels();
            pos.setYToPixels();
        }
        
        Iterator<Size> it2 = new GenericSizesIterator( this );
        
        while ( it2.hasNext() )
        {
            Size siz = it2.next();
            siz.setWidthToPixels();
            siz.setHeightToPixels();
        }
    }
    
    public final BackgroundProperty getBackgroundProperty()
    {
        return ( backgroundProperty );
    }
    
    /**
     * Gets the {@link Widget}'s background.
     * 
     * @return the {@link Widget}'s background.
     */
    public final WidgetBackground getBackground()
    {
        return ( background );
    }
    
    public final FontProperty getFontProperty()
    {
        return ( font );
    }
    
    protected final java.awt.Font getFont()
    {
        return ( font.getFont() );
    }
    
    protected final boolean isFontAntiAliased()
    {
        return ( font.isAntiAliased() );
    }
    
    public final ColorProperty getFontColorProperty()
    {
        return ( fontColor );
    }
    
    /**
     * Gets the {@link Widget}'s font color.
     * 
     * @return the {@link Widget}'s font color.
     */
    protected final Color getFontColor()
    {
        return ( fontColor.getColor() );
    }
    
    protected final IntProperty getPaddingPropertyTop()
    {
        return ( paddingTop );
    }
    
    protected final int getPaddingTop()
    {
        if ( masterWidget != null )
            return ( 0 );
        
        return ( paddingTop.getIntValue() );
    }
    
    protected final IntProperty getPaddingPropertyLeft()
    {
        return ( paddingLeft );
    }
    
    protected final int getPaddingLeft()
    {
        if ( masterWidget != null )
            return ( 0 );
        
        return ( paddingLeft.getIntValue() );
    }
    
    protected final IntProperty getPaddingPropertyRight()
    {
        return ( paddingRight );
    }
    
    protected final int getPaddingRight()
    {
        if ( masterWidget != null )
            return ( 0 );
        
        return ( paddingRight.getIntValue() );
    }
    
    protected final IntProperty getPaddingPropertyBottom()
    {
        return ( paddingBottom );
    }
    
    protected final int getPaddingBottom()
    {
        if ( masterWidget != null )
            return ( 0 );
        
        return ( paddingBottom.getIntValue() );
    }
    
    protected final BorderProperty getBorderProperty()
    {
        return ( border );
    }
    
    /**
     * Sets padding for this Widget.
     * 
     * @param top top padding value
     * @param left left padding value
     * @param right right padding value
     * @param bottom bottom padding value
     */
    protected final void setPadding( int top, int left, int right, int bottom )
    {
        paddingTop.setIntValue( top );
        paddingLeft.setIntValue( left );
        paddingRight.setIntValue( right );
        paddingBottom.setIntValue( bottom );
    }
    
    /**
     * Sets padding for this Widget.
     * 
     * @param padding top, left, right and bottom padding value
     */
    protected final void setPadding( int padding )
    {
        setPadding( padding, padding, padding, padding );
    }
    
    /**
     * Returns a {@link BorderWrapper}, that encapsulates the actual used border with convenience wrappers for the size getters.
     * The {@link BorderWrapper} instance is never null while the border can be null.
     * 
     * @return a {@link BorderWrapper} for the used Border (never null).
     */
    public final BorderWrapper getBorder()
    {
        return ( border.getBorder() );
    }
    
    /**
     * This method is called when a complete redraw has been forced.
     */
    protected void onCompleteRedrawForced()
    {
    }
    
    void forceCompleteRedraw_( boolean mergedBackgroundToo, boolean forwardCall )
    {
        boolean changed = !this.needsCompleteRedraw;
        
        this.needsCompleteRedraw = true;
        
        if ( ( background != null ) && mergedBackgroundToo )
            background.setMergedBGDirty();
        
        if ( forwardCall && ( masterWidget != null ) )
        if ( masterWidget != null )
            masterWidget.forceCompleteRedraw_( mergedBackgroundToo, false );
        
        setDirtyFlag();
        
        if ( changed )
            onCompleteRedrawForced();
    }
    
    /**
     * Forces a complete redraw on the next render.
     * 
     * @param mergedBackgroundToo if <code>true</code>, the clear-background will be redrawn and the {@link #drawBackground(LiveGameData, boolean, TextureImage2D, int, int, int, int, boolean)} methods will be called again.
     */
    public final void forceCompleteRedraw( boolean mergedBackgroundToo )
    {
        forceCompleteRedraw_( mergedBackgroundToo, true );
    }
    
    /**
     * This simply calls {@link #forceCompleteRedraw(boolean)}, {@link #forceReinitialization()} and {@link #setDirtyFlag()}.
     * This method must be called after a value has been changed, that requires a reinitialization of all positioned strings, etc.
     * 
     * @param mergedBackgroundToo whether to set merged background dirty, too
     */
    public final void forceAndSetDirty( boolean mergedBackgroundToo )
    {
        forceCompleteRedraw( mergedBackgroundToo );
        forceReinitialization();
        setDirtyFlag();
    }
    
    /**
     * Sets this Widget's visibility usually controlled by the ToggleWidgetVisibility InputAction.<br />
     * This flag is also restored when a different configurations is loaded unlike the others.
     * 
     * @param visible visible?
     */
    void setInputVisible( boolean visible )
    {
        boolean wasVisible = isVisible();
        
        this.inputVisible.setBooleanValue( visible );
        
        if ( isVisible() != wasVisible )
            onVisibilityChanged( visible );
    }
    
    /**
     * Gets this Widget's visibility usually controlled by the ToggleWidgetVisibility InputAction.<br />
     * This flag is also restored when a different configurations is loaded unlike the others.
     * 
     * @return this Widget's visibility flag.
     */
    public final boolean getInputVisibility()
    {
        return ( inputVisible.getBooleanValue() );
    }
    
    private void setAutoVisiblility( boolean visible )
    {
        boolean wasVisible = isVisible();
        
        this.autoVisible = visible;
        
        if ( isVisible() != wasVisible )
            onVisibilityChanged( visible );
    }
    
    /**
     * Gets the automatically toggled visibility flag.
     * 
     * @return this Widget's visibility flag.
     */
    public final boolean getAutoVisibility()
    {
        return ( autoVisible );
    }
    
    private void setUpdatedVisibility( boolean visible )
    {
        boolean wasVisible = isVisible();
        
        this.updatedVisible = visible;
        
        if ( isVisible() != wasVisible )
            onVisibilityChanged( visible );
    }
    
    /**
     * Gets this Widget's user visibility flag 2. This is the one, you should toggle in your widget code.
     * 
     * @return this Widget's visibility flag.
     */
    public final boolean getUpdatedVisibility()
    {
        return ( updatedVisible );
    }
    
    /**
     * Gets this Widget's total visibility flag ({@link #getInputVisibility()} && {@link #getAutoVisibility()} && {@link #getUpdatedVisibility()}).
     * 
     * @return this Widget's visibility flag.
     */
    public final boolean isVisible()
    {
        return ( inputVisible.getBooleanValue() && autoVisible && updatedVisible );
    }
    
    public final boolean visibilityChangedSinceLastDraw()
    {
        return ( visibilityChangedSinceLastDraw );
    }
    
    /**
     * This method is called first by the rendering system each to check for visibility changes.
     * This doesn't affect the visiblity toggled by the {@link #onBoundInputStateChanged(InputAction, boolean, int, long, LiveGameData, boolean)} method.
     * Automatic visiblity can also override the result.
     * 
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     * 
     * @return <code>true</code> to set visible, <code>false</code> to set invisible, <code>null</code> for no change in visibility.
     * 
     * @see #getUpdatedVisibility()
     */
    protected Boolean updateVisibility( LiveGameData gameData, boolean isEditorMode )
    {
        return ( null );
    }
    
    final void _updateVisibility( LiveGameData gameData, boolean isEditorMode )
    {
        Boolean result;
        
        if ( controller != null )
        {
            result = controller.isWidgetVisible();
            
            if ( result == null )
                result = updateVisibility( gameData, isEditorMode );
            else
                updateVisibility( gameData, isEditorMode );
        }
        else
        {
            result = updateVisibility( gameData, isEditorMode );
        }
        
        if ( result != null )
            setUpdatedVisibility( result.booleanValue() );
    }
    
    private final boolean needsCompleteRedraw()
    {
        boolean result = needsCompleteRedraw;
        
        needsCompleteRedraw = false;
        
        return ( result );
    }
    
    /**
     * Gets, whether this Widget has just been set invisible and its area hence needs to be cleared.
     * The flag is forced to false after this method has been called.
     * 
     * @return whether this Widget has just been set invisible and its area hence needs to be cleared.
     */
    final boolean needsCompleteClear()
    {
        boolean result = needsCompleteClear;
        
        needsCompleteClear = false;
        
        return ( result );
    }
    
    /**
     * Gets the data indicators for the data needed for this {@link Widget} to be drawn (bitmask).
     * 
     * @see #NEEDED_DATA_TELEMETRY
     * @see #NEEDED_DATA_SCORING
     * @see #NEEDED_DATA_SCORING
     * 
     * @return the data indicators for the data needed for this {@link Widget} to be drawn.
     */
    public int getNeededData()
    {
        return ( 0 );
    }
    
    /**
     * Gets the {@link Widget}'s {@link DrawnStringFactory}.
     * 
     * @return the {@link Widget}'s {@link DrawnStringFactory}.
     */
    protected final DrawnStringFactory getDrawnStringFactory()
    {
        return ( drawnStringFactory );
    }
    
    /**
     * This event is fired right after the {@link WidgetsConfiguration} has been (re-)loaded.
     * 
     * @param widgetsConfig the widgets configuration
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     */
    public void afterConfigurationLoaded( WidgetsConfiguration widgetsConfig, LiveGameData gameData, boolean isEditorMode )
    {
    }
    
    /**
     * This event is fired right before the {@link WidgetsConfiguration} is cleared.
     * 
     * @param widgetsConfig the widgets configuration
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     */
    public void beforeConfigurationCleared( WidgetsConfiguration widgetsConfig, LiveGameData gameData, boolean isEditorMode )
    {
    }
    
    /**
     * This method is executed when a new track was loaded.
     * 
     * @param trackname the current track's name
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     */
    public void onTrackChanged( String trackname, LiveGameData gameData, boolean isEditorMode )
    {
    }
    
    /**
     * This method is executed when a new session was started.
     * 
     * @param sessionType the current session type
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     */
    public void onSessionStarted( SessionType sessionType, LiveGameData gameData, boolean isEditorMode )
    {
    }
    
    /**
     * This method is called when a the user entered realtime mode. If your {@link Widget} needs some data
     * to be drawn correctly, consider using {@link #onNeededDataComplete(LiveGameData, boolean)}.
     * 
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     * 
     * @deprecated replaced by {@link #onCockpitEntered(LiveGameData, boolean)}
     */
    @Deprecated
    public void onRealtimeEntered( LiveGameData gameData, boolean isEditorMode )
    {
    }
    
    /**
     * This method is called when a the user entered  the cockpit. If your {@link Widget} needs some data
     * to be drawn correctly, consider using {@link #onNeededDataComplete(LiveGameData, boolean)}.
     * 
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     */
    public void onCockpitEntered( LiveGameData gameData, boolean isEditorMode )
    {
        onRealtimeEntered( gameData, isEditorMode );
    }
    
    /**
     * This method is called when {@link ScoringInfo} have been updated (done at 2Hz).
     * 
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     */
    public void onScoringInfoUpdated( LiveGameData gameData, boolean isEditorMode )
    {
    }
    
    /**
     * This method is called when {@link VehicleSetup} has been updated.
     * 
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     */
    public void onVehicleSetupUpdated( LiveGameData gameData, boolean isEditorMode )
    {
    }
    
    /**
     * This method is called when the needed data is available in realtime mode.
     * 
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     */
    public void onNeededDataComplete( LiveGameData gameData, boolean isEditorMode )
    {
    }
    
    /**
     * This method is called when a the car entered the pits.
     * 
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     */
    public void onPitsEntered( LiveGameData gameData, boolean isEditorMode )
    {
    }
    
    /**
     * This method is called when a the car entered the garage.
     * 
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     */
    public void onGarageEntered( LiveGameData gameData, boolean isEditorMode )
    {
    }
    
    /**
     * This method is called when a the car exited the garage.
     * 
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     */
    public void onGarageExited( LiveGameData gameData, boolean isEditorMode )
    {
    }
    
    /**
     * This method is called when a the car exited the pits.
     * 
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     */
    public void onPitsExited( LiveGameData gameData, boolean isEditorMode )
    {
    }
    
    /**
     * This method is called when a the user exited realtime mode.
     * 
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     * 
     * @deprecated replaced by {@link #onCockpitExited(LiveGameData, boolean)}
     */
    @Deprecated
    public void onRealtimeExited( LiveGameData gameData, boolean isEditorMode )
    {
    }
    
    /**
     * This method is called when a the user exited the cockpit.
     * 
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     */
    public void onCockpitExited( LiveGameData gameData, boolean isEditorMode )
    {
        onRealtimeExited( gameData, isEditorMode );
    }
    
    /**
     * This method is called when either the player's vehicle control has changed or another vehicle is being viewed.
     * 
     * @param viewedVSI the currently viewed vehicle
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     * 
     * @return <code>true</code> to set visible, <code>false</code> to set invisible, <code>null</code> for no change in visibility.
     * 
     * This doesn't affect the visiblity toggled by the {@link #updateVisibility(LiveGameData, boolean)} method or {@link #onBoundInputStateChanged(InputAction, boolean, int, long, LiveGameData, boolean)} method.
     * 
     * @see #getAutoVisibility()
     */
    protected Boolean onVehicleControlChanged( VehicleScoringInfo viewedVSI, LiveGameData gameData, boolean isEditorMode )
    {
        return ( null );
    }
    
    final void _onVehicleControlChanged( VehicleScoringInfo viewedVSI, LiveGameData gameData, boolean isEditorMode )
    {
        Boolean result = onVehicleControlChanged( viewedVSI, gameData, isEditorMode );
        
        if ( result != null )
            setAutoVisiblility( result.booleanValue() );
    }
    
    /**
     * This method is called when a lap has been finished and a new one was started.
     * 
     * @param vsi the driver, who started the lap. If this is the leader and the session type is RACE, the whole race has moved on to the next lap.
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     */
    public void onLapStarted( VehicleScoringInfo vsi, LiveGameData gameData, boolean isEditorMode )
    {
    }
    
    /**
     * This event is fired, when a bound input component has changed its state.
     * 
     * @param action the triggered action
     * @param state the state of the input device component
     * @param modifierMask see {@link InputAction}
     * @param when the timestamp in nano seconds
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     * 
     * @return <code>true</code> to set visible, <code>false</code> to set invisible, <code>null</code> for no change in visibility.
     * 
     * This doesn't affect the visiblity toggled by the {@link #updateVisibility(LiveGameData, boolean)} method.
     * Automatic visiblity can also override the result.
     * 
     * @see #getInputVisibility()
     */
    protected Boolean onBoundInputStateChanged( InputAction action, boolean state, int modifierMask, long when, LiveGameData gameData, boolean isEditorMode )
    {
        return ( null );
    }
    
    final void _onBoundInputStateChanged( InputAction action, boolean state, int modifierMask, long when, LiveGameData gameData, boolean isEditorMode )
    {
        Boolean result = onBoundInputStateChanged( action, state, modifierMask, when, gameData, isEditorMode );
        
        if ( result != null )
            setInputVisible( result.booleanValue() );
    }
    
    /**
     * Returns <code>true</code>, if this {@link Widget} draws on the main texture, <code>false</code> otherwise.<br />
     * Default is <code>true</code>.
     * 
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     * 
     * @return <code>true</code>, if this {@link Widget} draws on the main texture, <code>false</code> otherwise.
     */
    public boolean hasMasterCanvas( boolean isEditorMode )
    {
        return ( true );
    }
    
    protected void clearRegion( TextureImage2D texture, int offsetX, int offsetY, int width, int height )
    {
        if ( texture == null )
            return;
        
        texture.getTextureCanvas().pushClip( offsetX, offsetY, width, height );
        
        texture.clear( offsetX, offsetY, width, height, true, null );
        
        texture.getTextureCanvas().popClip();
        
        this.visibilityChangedSinceLastDraw = false;
    }
    
    /**
     * Clears the region on the texture, that is covered by this {@link Widget}.
     * 
     * @param texture the texture image to draw on. Use {@link TextureImage2D#getTextureCanvas()} to retrieve the {@link Texture2DCanvas} for Graphics2D drawing.
     * @param offsetX the x offset of the {@link Widget} on the drawing texture
     * @param offsetY the y offset of the {@link Widget} on the drawing texture
     */
    public final void clearRegion( TextureImage2D texture, int offsetX, int offsetY )
    {
        int width = size.getEffectiveWidth();
        int height = size.getEffectiveHeight();
        
        clearRegion( texture, offsetX, offsetY, width, height );
    }
    
    /**
     * Clears the given part of the {@link Widget} area with the current background.
     * 
     * @param texture the target texture
     * @param offsetX the x offset of the {@link Widget} on the drawing texture
     * @param offsetY the y offset of the {@link Widget} on the drawing texture
     * @param localX the x coordinate of the upper left corner of the area to be cleared relative to the Widget's location
     * @param localY the y coordinate of the upper left corner of the area to be cleared relative to the Widget's location
     * @param width the width of the area to be cleared
     * @param height the height of the area to be cleared
     * @param markDirty if true, the pixel is marked dirty
     * @param dirtyRect if non null, the dirty rect is written to this instance
     * 
     * @return <code>true</code>, if this Widgets defines a background to clear with, <code>false</code> otherwise.
     */
    public boolean clearBackgroundRegion( TextureImage2D texture, int offsetX, int offsetY, int localX, int localY, int width, int height, boolean markDirty, Rect2i dirtyRect )
    {
        if ( getMasterWidget() != null )
        {
            int effX = getPosition().getEffectiveX();
            int effY = getPosition().getEffectiveY();
            
            return ( getMasterWidget().clearBackgroundRegion( texture, offsetX - effX, offsetY - effY, localX + effX, localY + effY, width, height, markDirty, dirtyRect ) );
        }
        
        final WidgetBackground background = getBackground();
        
        if ( background == null )
        {
            if ( dirtyRect != null )
                dirtyRect.set( -1, -1, 0, 0 );
            
            return ( false );
        }
        
        TextureImage2D mergedBG = background.getMergedTexture();
        if ( mergedBG != null )
        {
            texture.clear( mergedBG, localX, localY, width, height, offsetX + localX, offsetY + localY, markDirty, dirtyRect );
            
            return ( true );
        }
        
        if ( background.getType().isColor() )
        {
            texture.clear( background.getColor(), offsetX + localX, offsetY + localY, width, height, markDirty, dirtyRect );
            
            return ( true );
        }
        
        if ( background.getType().isImage() )
        {
            texture.clear( background.getTexture(), localX, localY, width, height, offsetX + localX, offsetY + localY, markDirty, dirtyRect );
            
            return ( true );
        }
        
        return ( false );
    }
    
    /**
     * This method is called once to initialized {@link DrawnString}s used on this Widget.
     * 
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     * @param drawnStringFactory a factory to get {@link DrawnString} instances from
     * @param texture the texture image to draw on. Use {@link TextureImage2D#getTextureCanvas()} to retrieve the {@link Texture2DCanvas} for Graphics2D drawing.
     * @param width the width on the texture
     * @param height the height on the texture
     */
    protected abstract void initialize( LiveGameData gameData, boolean isEditorMode, DrawnStringFactory drawnStringFactory, TextureImage2D texture, int width, int height );
    
    /**
     * Checks, if the Widget needs any changes before it is drawn. If true, {@link #drawBorder(boolean, BorderWrapper, TextureImage2D, int, int, int, int)}
     * and possibly {@link #drawBackground(LiveGameData, boolean, TextureImage2D, int, int, int, int, boolean)} are (re-)invoked.<br />
     * The original method is just an empty stub returning false.
     * 
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     * @param texture the texture image to draw on. Use {@link TextureImage2D#getTextureCanvas()} to retrieve the {@link Texture2DCanvas} for Graphics2D drawing.
     * @param width the width on the texture
     * @param height the height on the texture
     * 
     * @return true, if size has changed.
     */
    protected boolean checkForChanges( LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int width, int height )
    {
        return ( false );
    }
    
    /**
     * 
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     * @param border the border
     * @param texture the texture image to draw on. Use {@link TextureImage2D#getTextureCanvas()} to retrieve the {@link Texture2DCanvas} for Graphics2D drawing.
     * @param offsetX the x-offset on the drawing texture
     * @param offsetY the y offset on the drawing texture
     * @param width the width of the area on the drawing texture
     * @param height the height of the area on the drawing texture
     */
    protected void drawBorder( boolean isEditorMode, BorderWrapper border, TextureImage2D texture, int offsetX, int offsetY, int width, int height )
    {
        if ( hasBorder() && ( texture != null ) )
        {
            border.drawBorder( ( ( background == null ) || !background.getType().isColor() ) ? null : background.getColor(), texture, offsetX, offsetY, width, height );
        }
    }
    
    /**
     * You can use this method to directly draw static content onto your Widget's background.
     * Overriding this method makes the Widget use a background texture no matter, if the background is defined with a color only or an image.
     * 
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     * @param texture the texture image to draw on. Use {@link TextureImage2D#getTextureCanvas()} to retrieve the {@link Texture2DCanvas} for Graphics2D drawing.
     * @param offsetX the x-offset on the drawing texture
     * @param offsetY the y offset on the drawing texture
     * @param width the width of the area on the drawing texture
     * @param height the height of the area on the drawing texture
     * @param isRoot if this is true, you can possibly clear your stuff onto the texture instead of drawing it.
     */
    protected void drawBackground( LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height, boolean isRoot )
    {
        if ( canHaveBackground() )
        {
            if ( background.getType().isColor() )
            {
                if ( isRoot )
                    texture.clear( background.getColor(), offsetX, offsetY, width, height, false, null );
                else if ( background.getColor().getAlpha() > 0 )
                    texture.fillRectangle( background.getColor(), offsetX, offsetY, width, height, false, null );
            }
            else if ( background.getType().isImage() )
            {
                if ( isRoot )
                    texture.clear( background.getTexture(), 0, 0, width, height, offsetX, offsetY, width, height, false, null );
                else
                    texture.drawImage( background.getTexture(), 0, 0, width, height, offsetX, offsetY, width, height, false, null );
            }
        }
    }
    
    void _drawBackground( LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height, boolean isRoot )
    {
        texture.getTextureCanvas().pushClip( offsetX, offsetY, width, height, true );
        
        try
        {
            drawBackground( gameData, isEditorMode, texture, offsetX, offsetY, width, height, isRoot );
        }
        finally
        {
            texture.getTextureCanvas().popClip();
        }
    }
    
    /**
     * @param background never <code>null</code>!
     * @param texture the texture image to draw on. Use {@link TextureImage2D#getTextureCanvas()} to retrieve the {@link Texture2DCanvas} for Graphics2D drawing.
     * @param offsetX the x-offset on the drawing texture
     * @param offsetY the y offset on the drawing texture
     * @param width the width of the area on the drawing texture
     * @param height the height of the area on the drawing texture
     */
    private final void clearBackground( WidgetBackground background, TextureImage2D texture, int offsetX, int offsetY, int width, int height )
    {
        if ( texture != null )
        {
            TextureImage2D mergedBG = background.getMergedTexture();
            
            if ( mergedBG == null )
            {
                if ( background.getColor() != null )
                    texture.clear( background.getColor(), offsetX, offsetY, width, height, true, null );
                else if ( background.getTexture() != null )
                    texture.clear( background.getTexture(), offsetX, offsetY, width, height, true, null );
                else
                    texture.clear( offsetX, offsetY, width, height, true, null );
            }
            else
            {
                texture.clear( mergedBG, offsetX, offsetY, width, height, true, null );
            }
        }
    }
    
    /**
     * This method must contain the actual drawing code for this Widget.
     * 
     * @param clock this is a clock for very dynamic content, that needs smooth display. If 'needsCompleteRedraw' is true, clock1 is also true.
     * @param needsCompleteRedraw whether this widget needs to be completely redrawn (true) or just the changed parts (false)
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     * @param texture the texture image to draw on. Use {@link TextureImage2D#getTextureCanvas()} to retrieve the {@link Texture2DCanvas} for Graphics2D drawing.
     * @param offsetX the x-offset on the texture
     * @param offsetY the y-offset on the texture
     * @param width the width on the texture
     * @param height the height on the texture
     */
    protected abstract void drawWidget( Clock clock, boolean needsCompleteRedraw, LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, int offsetX, int offsetY, int width, int height );
    
    /**
     * This method invokes the parts of the actual drawing code for this Widget.
     * 
     * @param clock this is a clock for very dynamic content, that needs smooth display. If 'needsCompleteRedraw' is true, clock1 is also true.
     * @param completeRedrawForced whether this widget needs to be completely redrawn (true) or just the changed parts (false)
     * @param gameData the live game data
     * @param isEditorMode <code>true</code>, if the Editor is used for rendering instead of rFactor
     * @param texture the texture image to draw on. Use {@link TextureImage2D#getTextureCanvas()} to retrieve the {@link Texture2DCanvas} for Graphics2D drawing.
     * @param drawAtZero draw at position 0,0?
     */
    public final void drawWidget( Clock clock, boolean completeRedrawForced, LiveGameData gameData, boolean isEditorMode, TextureImage2D texture, boolean drawAtZero )
    {
        int offsetX = drawAtZero ? 0 : position.getEffectiveX();
        int offsetY = drawAtZero ? 0 : position.getEffectiveY();
        int width = size.getEffectiveWidth();
        int height = size.getEffectiveHeight();
        
        int borderOLW = getBorder().getOpaqueLeftWidth();
        int borderOTH = getBorder().getOpaqueTopHeight();
        int borderORW = getBorder().getOpaqueRightWidth();
        int borderOBH = getBorder().getOpaqueBottomHeight();
        
        int borderLW = getBorder().getInnerLeftWidth();
        int borderTH = getBorder().getInnerTopHeight();
        int borderRW = getBorder().getInnerRightWidth();
        int borderBH = getBorder().getInnerBottomHeight();
        
        int offsetX2 = offsetX + borderLW;
        int offsetY2 = offsetY + borderTH;
        int width2 = width - borderLW - borderRW;
        int height2 = height - borderTH - borderBH;
        
        if ( !isEditorMode && !hasMasterCanvas( isEditorMode ) )
            texture = null;
        
        final Texture2DCanvas texCanvas = ( texture == null ) ? null : texture.getTextureCanvas();
        
        if ( !initialized )
        {
            initialize( gameData, isEditorMode, drawnStringFactory, texture, width2, height2 );
            
            initialized = true;
        }
        
        if ( checkForChanges( gameData, isEditorMode, texture, width2, height2 ) )
        {
            clearRegion( texture, offsetX, offsetY, width, height );
            forceCompleteRedraw( true );
            completeRedrawForced = true;
            
            //offsetX = position.getEffectiveX();
            //offsetY = position.getEffectiveY();
            width = size.getEffectiveWidth();
            height = size.getEffectiveHeight();
            
            offsetX2 = offsetX + borderLW;
            offsetY2 = offsetY + borderTH;
            width2 = width - borderLW - borderRW;
            height2 = height - borderTH - borderBH;
        }
        
        if ( texCanvas != null )
            texCanvas.setClip( offsetX, offsetY, width, height );
        
        completeRedrawForced = needsCompleteRedraw() || completeRedrawForced;
        
        if ( completeRedrawForced )
        {
            __RenderPrivilegedAccess.onWidgetCleared( drawnStringFactory );
            
            drawBorder( isEditorMode, getBorder(), texture, offsetX, offsetY, width, height );
            
            if ( texture != null )
                texture.markDirty( offsetX, offsetY, width, height, null );
            
            if ( ( getMasterWidget() == null ) && ( background != null ) )
            {
                background.updateMergedBackground( gameData, isEditorMode );
                
                clearBackground( background, texture, offsetX2 - getBorder().getPaddingLeft(), offsetY2 - getBorder().getPaddingTop(), width2 + getBorder().getPaddingLeft() + getBorder().getPaddingRight(), height2 + getBorder().getPaddingTop() + getBorder().getPaddingBottom() );
            }
        }
        
        if ( texCanvas != null )
            texCanvas.setClip( offsetX + borderOLW, offsetY + borderOTH, width - borderOLW - borderORW, height - borderOTH - borderOBH );
        
        drawWidget( clock, completeRedrawForced, gameData, isEditorMode, texture, offsetX2, offsetY2, width2, height2 );
        
        this.visibilityChangedSinceLastDraw = false;
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void saveProperties( PropertyWriter writer ) throws IOException
    {
        writer.writeProperty( positioningProperty, "The way, position coordinates are interpreted (relative to). Valid values: TOP_LEFT, TOP_CENTER, TOP_RIGHT, CENTER_LEFT, CENTER_CENTER, CENTER_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT." );
        writer.writeProperty( xProperty, "The x-coordinate for the position." );
        writer.writeProperty( yProperty, "The y-coordinate for the position." );
        writer.writeProperty( zIndex, "A z-index, to sort Widgets by." );
        writer.writeProperty( widthProperty, "The width. Use negative values to make the Widget be sized relative to screen size." );
        writer.writeProperty( heightProperty, "The height. Use negative values to make the Widget be sized relative to screen size." );
        if ( masterWidget == null )
        {
            writer.writeProperty( border, "The widget's border." );
            writer.writeProperty( paddingTop, "top padding" );
            writer.writeProperty( paddingLeft, "left padding" );
            writer.writeProperty( paddingRight, "right padding" );
            writer.writeProperty( paddingBottom, "bottom padding" );
            writer.writeProperty( inputVisible, "The initial visibility." );
        }
        
        if ( canHaveBackground() )
        {
            writer.writeProperty( backgroundProperty, "The Widget's background (color or image)." );
        }
        
        if ( hasText() )
        {
            writer.writeProperty( font, "The used font." );
            writer.writeProperty( fontColor, "The Widget's font color in the format #RRGGBB (hex)." );
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void loadProperty( PropertyLoader loader )
    {
        if ( loader.getSourceVersion().getBuild() < 78 )
        {
            if ( loader.getCurrentKey().equals( "backgroundColor" ) && !this.getClass().getSimpleName().startsWith( "ETV" ) )
                backgroundProperty.loadValue( loader, "color:" + loader.getCurrentValue() );
            else if ( loader.getCurrentKey().equals( "backgroundImageName" ) )
                backgroundProperty.loadValue( loader, "image:" + loader.getCurrentValue() );
        }
        
        if ( loader.loadProperty( name ) );
        else if ( loader.loadProperty( positioningProperty ) );
        else if ( loader.loadProperty( xProperty ) );
        else if ( loader.loadProperty( yProperty ) );
        else if ( loader.loadProperty( zIndex ) );
        else if ( loader.loadProperty( widthProperty ) );
        else if ( loader.loadProperty( heightProperty ) );
        else if ( ( masterWidget == null ) && loader.loadProperty( border ) );
        else if ( loader.loadProperty( paddingTop ) );
        else if ( loader.loadProperty( paddingLeft ) );
        else if ( loader.loadProperty( paddingRight ) );
        else if ( loader.loadProperty( paddingBottom ) );
        else if ( loader.loadProperty( inputVisible ) );
        else if ( canHaveBackground() && loader.loadProperty( backgroundProperty ) );
        else if ( loader.loadProperty( font ) );
        else if ( loader.loadProperty( fontColor ) );
    }
    
    /**
     * This method is called instead of {@link #loadProperty(PropertyLoader)} to load the property for compatiblity reasons.
     * It simply forwards the call to {@link #loadProperty(PropertyLoader)}. So override this method, if you need game data.
     * 
     * @param loader
     * @param gameData
     */
    public void loadProperty( PropertyLoader loader, LiveGameData gameData )
    {
        loadProperty( loader );
    }
    
    /**
     * Gets an {@link Iterator} to iterate all properties, defined in this class.
     * 
     * @return an {@link Iterator} to iterate all properties, defined in this class.
     */
    public Iterator<Property> getPropertiesIterator()
    {
        return ( new GenericPropertiesIterator( this ) );
    }
    
    /**
     * Adds the type and name properties to the {@link PropertiesContainer}.
     * 
     * @param propsCont the container to add the properties to
     * @param forceAll If <code>true</code>, all properties provided by this {@link Widget} must be added.
     *                 If <code>false</code>, only the properties, that are relevant for the current {@link Widget}'s situation have to be added, some can be ignored.
     */
    protected void addTypeAndNamePropertiesToContainer( PropertiesContainer propsCont, boolean forceAll )
    {
        propsCont.addProperty( type );
        propsCont.addProperty( name );
    }
    
    /**
     * Adds the visibility properties to the {@link PropertiesContainer}.
     * 
     * @param propsCont the container to add the properties to
     * @param forceAll If <code>true</code>, all properties provided by this {@link Widget} must be added.
     *                 If <code>false</code>, only the properties, that are relevant for the current {@link Widget}'s situation have to be added, some can be ignored.
     */
    protected void addVisibilityPropertiesToContainer( PropertiesContainer propsCont, boolean forceAll )
    {
        if ( masterWidget == null )
        {
            propsCont.addProperty( inputVisible );
        }
    }
    
    /**
     * Adds the position and size properties to the {@link PropertiesContainer}.
     * 
     * @param propsCont the container to add the properties to
     * @param forceAll If <code>true</code>, all properties provided by this {@link Widget} must be added.
     *                 If <code>false</code>, only the properties, that are relevant for the current {@link Widget}'s situation have to be added, some can be ignored.
     */
    protected void addPositionAndSizePropertiesToContainer( PropertiesContainer propsCont, boolean forceAll )
    {
        propsCont.addProperty( positioningProperty );
        propsCont.addProperty( xProperty );
        propsCont.addProperty( yProperty );
        propsCont.addProperty( zIndex );
        propsCont.addProperty( widthProperty );
        propsCont.addProperty( heightProperty );
    }
    
    /**
     * Adds the border property to the container.
     * 
     * @param propsCont the container to add the properties to
     * @param forceAll If <code>true</code>, all properties provided by this {@link Widget} must be added.
     *                 If <code>false</code>, only the properties, that are relevant for the current {@link Widget}'s situation have to be added, some can be ignored.
     */
    protected void addBorderPropertyToContainer( PropertiesContainer propsCont, boolean forceAll )
    {
        propsCont.addProperty( border );
    }
    
    /**
     * Adds the padding properties to the container.
     * 
     * @param propsCont the container to add the properties to
     * @param forceAll If <code>true</code>, all properties provided by this {@link Widget} must be added.
     *                 If <code>false</code>, only the properties, that are relevant for the current {@link Widget}'s situation have to be added, some can be ignored.
     */
    protected void addPaddingPropertiesToContainer( PropertiesContainer propsCont, boolean forceAll )
    {
        propsCont.pushGroup( "Padding", false );
        
        propsCont.addProperty( paddingTop );
        propsCont.addProperty( paddingLeft );
        propsCont.addProperty( paddingRight );
        propsCont.addProperty( paddingBottom );
        
        propsCont.popGroup();
    }
    
    
    /**
     * Adds the background property to the container.
     * 
     * @param propsCont the container to add the properties to
     * @param forceAll If <code>true</code>, all properties provided by this {@link Widget} must be added.
     *                 If <code>false</code>, only the properties, that are relevant for the current {@link Widget}'s situation have to be added, some can be ignored.
     */
    protected void addBackgroundPropertyToContainer( PropertiesContainer propsCont, boolean forceAll )
    {
        propsCont.addProperty( backgroundProperty );
    }
    
    
    /**
     * Adds the font and font color properties to the container.
     * 
     * @param propsCont the container to add the properties to
     * @param forceAll If <code>true</code>, all properties provided by this {@link Widget} must be added.
     *                 If <code>false</code>, only the properties, that are relevant for the current {@link Widget}'s situation have to be added, some can be ignored.
     */
    protected void addFontPropertiesToContainer( PropertiesContainer propsCont, boolean forceAll )
    {
        propsCont.addProperty( font );
        propsCont.addProperty( fontColor );
    }
    
    /**
     * Puts all editable properties to the editor.
     * 
     * @param propsCont the container to add the properties to
     * @param forceAll If <code>true</code>, all properties provided by this {@link Widget} must be added.
     *                 If <code>false</code>, only the properties, that are relevant for the current {@link Widget}'s situation have to be added, some can be ignored.
     */
    protected void getPropertiesForParentGroup( PropertiesContainer propsCont, boolean forceAll )
    {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void getProperties( PropertiesContainer propsCont, boolean forceAll )
    {
        propsCont.addGroup( "General" );
        
        addTypeAndNamePropertiesToContainer( propsCont, forceAll );
        
        addVisibilityPropertiesToContainer( propsCont, forceAll );
        
        addPositionAndSizePropertiesToContainer( propsCont, forceAll );
        
        if ( masterWidget == null )
        {
            addPaddingPropertiesToContainer( propsCont, forceAll );
        }
        
        if ( ( masterWidget == null ) && canHaveBorder() )
        {
            addBorderPropertyToContainer( propsCont, forceAll );
        }
        
        if ( canHaveBackground() )
        {
            addBackgroundPropertyToContainer( propsCont, forceAll );
        }
        
        if ( hasText() )
        {
            addFontPropertiesToContainer( propsCont, forceAll );
        }
        
        getPropertiesForParentGroup( propsCont, forceAll );
        
        //propsCont.dump();
    }
    
    protected Widget getNewInstanceForClone()
    {
        @SuppressWarnings( "unchecked" )
        Class<Widget> clazz = (Class<Widget>)getClass();
        
        return ( WidgetFactory.createWidget( clazz, "CloneOf" + getName() ) );
    }
    
    /**
     * Clones the value of the src property to the trg property.
     * This method is invoked on the target Widget.
     * 
     * @param src
     * @param trg
     * 
     * @return <code>true</code>, if the properties list needs to be refreshed, <code>false</code> otherwise.
     */
    protected boolean cloneProperty( Property src, Property trg )
    {
        trg.setValue( src.getValue() );
        
        return ( false );
    }
    
    @Override
    public Widget clone()
    {
        Widget newWidget = getNewInstanceForClone();
        
        if ( newWidget == null )
            return ( null );
        
        FlatPropertiesContainer pcTemplate = new FlatPropertiesContainer();
        FlatPropertiesContainer pcTarget = new FlatPropertiesContainer();
        
        this.getProperties( pcTemplate, true );
        newWidget.getProperties( pcTarget, true );
        
        List<Property> lstTemplate = pcTemplate.getList();
        List<Property> lstTarget = pcTarget.getList();
        
        // We assume, that the order will be the same in both lists.
        
        for ( int i = 0; i < lstTemplate.size(); i++ )
        {
            Property p0 = lstTemplate.get( i );
            Property p1 = lstTarget.get( i );
            
            //if ( ( includePosition || ( !p0.getName().equals( "x" ) && !p0.getName().equals( "y" ) && !p0.getName().equals( "positioning" ) ) ) )
                if ( newWidget.cloneProperty( p0, p1 ) )
                {
                    pcTarget.clear();
                    newWidget.getProperties( pcTarget, true );
                }
        }
        
        return ( newWidget );
    }
    
    private String getDocumentationSource( Class<?> clazz )
    {
        URL docURL = this.getClass().getClassLoader().getResource( clazz.getPackage().getName().replace( '.', '/' ) + "/doc/widget.html" );
        
        if ( docURL == null )
        {
            if ( ( clazz.getSuperclass() != null ) && ( clazz.getSuperclass() != Object.class ) )
                return ( getDocumentationSource( clazz.getSuperclass() ) );
            
            return ( "" );
        }
        
        return ( StringUtils.loadString( docURL ) );
    }
    
    public final String getDocumentationSource()
    {
        return ( getDocumentationSource( this.getClass() ) );
    }
    
    
    
    /**
     * Defines, if this Widget type can have a border.
     * 
     * @return if this Widget type can have a border.
     */
    protected boolean canHaveBorder()
    {
        return ( true );
    }
    
    /**
     * Defines, if this Widget type can have a background.
     * 
     * @return if this Widget type can have a background.
     */
    protected boolean canHaveBackground()
    {
        return ( true );
    }
    
    /**
     * Gets whether this {@link Widget} has a border or not.
     * 
     * @return whether this {@link Widget} has a border or not.
     */
    protected final boolean hasBorder()
    {
        if ( !canHaveBorder() )
            return ( false );
        
        if ( !getBorder().hasBorder() )
            return ( false );
        
        /*
        if ( background == null )
            return ( false );
        
        return ( background.getType().isColor() );
        */
        return ( true );
    }
    
    /**
     * Defines, if a Widget type (potentially) contains any text.
     * If <code>false</code>, the editor won't provide font or font-color selection.
     * Should return a contant value.
     * 
     * @return if this Widget can contain any text.
     */
    protected boolean hasText()
    {
        return ( true );
    }
    
    /**
     * This method is called by the editor before it draws the Widget to a menu item.
     */
    public void prepareForMenuItem()
    {
        border.setBorder( null );
        setPadding( 0, 0, 0, 0 );
    }
    
    /**
     * Creates a new {@link Widget}.
     * 
     * @param widgetSet the {@link WidgetSet} this {@link Widget} belongs to
     * @param widgetPackage the package in the editor
     * @param width negative numbers for (screen_width - width)
     * @param widthPercent width parameter treated as percents
     * @param height negative numbers for (screen_height - height)
     * @param heightPercent height parameter treated as percents
     */
    protected Widget( WidgetSet widgetSet, WidgetPackage widgetPackage, float width, boolean widthPercent, float height, boolean heightPercent )
    {
        this.widgetSet = widgetSet;
        this.widgetPackage = widgetPackage;
        
        this.size = Size.newGlobalSize( this, width, widthPercent, height, heightPercent );
        this.innerSize = new InnerSize( size, border );
        this.position = Position.newGlobalPosition( this, RelativePositioning.TOP_LEFT, 0f, true, 0f, true, size );
        
        this.positioningProperty = position.getPositioningProperty( "positioning" );
        this.xProperty = position.getXProperty( "x" );
        this.yProperty = position.getYProperty( "y" );
        this.widthProperty = size.getWidthProperty( "width" );
        this.heightProperty = size.getHeightProperty( "height" );
        
        if ( !canHaveBorder() )
            border.setBorder( null );
    }
    
    /**
     * Creates a new {@link Widget}.
     * 
     * @param widgetSet the {@link WidgetSet} this {@link Widget} belongs to
     * @param widgetPackage the package in the editor
     * @param width negative numbers for (screen_width - width)
     * @param height negative numbers for (screen_height - height)
     */
    protected Widget( WidgetSet widgetSet, WidgetPackage widgetPackage, float width, float height )
    {
        this( widgetSet, widgetPackage, width, true, height, true );
    }
}
