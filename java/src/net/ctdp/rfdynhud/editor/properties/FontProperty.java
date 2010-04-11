package net.ctdp.rfdynhud.editor.properties;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import net.ctdp.rfdynhud.widgets.WidgetsConfiguration;
import net.ctdp.rfdynhud.widgets._util.FontUtils;
import net.ctdp.rfdynhud.widgets.widget.Widget;

public class FontProperty extends Property
{
    private static final BufferedImage METRICS_PROVIDER_IMAGE = new BufferedImage( 16, 16, BufferedImage.TYPE_INT_BGR );
    private static final Graphics2D METRICS_PROVIDER = METRICS_PROVIDER_IMAGE.createGraphics();
    
    private final WidgetsConfiguration widgetsConf;
    private final Widget widget;
    
    private String fontKey;
    private Font font = null;
    private Boolean antiAliased = null;
    
    private FontMetrics metrics = null;
    
    public final Widget getWidget()
    {
        return ( widget );
    }
    
    protected void onValueChanged( String oldValue, String newValue )
    {
    }
    
    public void setFont( String fontKey )
    {
        if ( ( fontKey == null ) && ( this.fontKey == null ) )
            return;
        
        final WidgetsConfiguration widgetsConf = ( widget != null ) ? widget.getConfiguration() : this.widgetsConf;
        
        if ( widgetsConf == null )
        {
            this.fontKey = fontKey;
            this.font = null;
            this.antiAliased = null;
            this.metrics = null;
        }
        else
        {
            String oldValue = widgetsConf.getNamedFontString( this.fontKey );
            if ( oldValue == null )
                oldValue = this.fontKey;
            
            this.fontKey = fontKey;
            this.font = null;
            this.antiAliased = null;
            this.metrics = null;
            
            String newValue = widgetsConf.getNamedFontString( this.fontKey );
            if ( newValue == null )
                newValue = this.fontKey;
            
            if ( ( newValue == null ) || !newValue.equals( oldValue ) )
            {
                if ( widget != null )
                    widget.forceAndSetDirty();
                
                onValueChanged( oldValue, fontKey );
            }
        }
    }
    
    public final void setFont( Font font, boolean virtual, boolean antiAliased )
    {
        setFont( FontUtils.getFontString( font, virtual, antiAliased ) );
    }
    
    public final String getFontKey()
    {
        return ( fontKey );
    }
    
    public final Font getFont()
    {
        if ( fontKey == null )
            return ( null );
        
        if ( font == null )
        {
            font = widget.getConfiguration().getNamedFont( fontKey );
            if ( font == null )
            {
                font = FontUtils.parseFont( fontKey, widget.getConfiguration().getGameResY(), false );
            }
        }
        
        return ( font );
    }
    
    public final boolean isAntiAliased()
    {
        if ( antiAliased == null )
        {
            String fk = widget.getConfiguration().getNamedFontString( fontKey );
            if ( fk == null )
                fk = fontKey;
            
            antiAliased = FontUtils.parseAntiAliasFlag( fk, false );
        }
        
        return ( antiAliased );
    }
    
    public final FontMetrics getMetrics()
    {
        if ( metrics == null )
        {
            metrics = METRICS_PROVIDER.getFontMetrics( getFont() );
        }
        
        return ( metrics );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue( Object value )
    {
        setFont( ( value == null ) ? null : String.valueOf( value ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValue()
    {
        return ( fontKey );
    }
    
    public final boolean loadProperty( String key, String value )
    {
        if ( key.equals( getPropertyName() ) )
        {
            setValue( value );
            
            return ( true );
        }
        
        return ( false );
    }
    
    private FontProperty( WidgetsConfiguration widgetsConf, Widget widget, String propertyName, String nameForDisplay, String defaultValue, boolean readonly )
    {
        super( propertyName, nameForDisplay, readonly, PropertyEditorType.FONT, null, null );
        
        this.widgetsConf = widgetsConf;
        this.widget = widget;
        this.fontKey = defaultValue;
    }
    
    public FontProperty( WidgetsConfiguration widgetsConf, String propertyName, String nameForDisplay, String defaultValue, boolean readonly )
    {
        this( widgetsConf, null, propertyName, nameForDisplay, defaultValue, readonly );
    }
    
    public FontProperty( WidgetsConfiguration widgetsConf, String propertyName, String nameForDisplay, String defaultValue )
    {
        this( widgetsConf, propertyName, nameForDisplay, defaultValue, false );
    }
    
    public FontProperty( WidgetsConfiguration widgetsConf, String propertyName, String defaultValue, boolean readonly )
    {
        this( widgetsConf, propertyName, propertyName, defaultValue, readonly );
    }
    
    public FontProperty( WidgetsConfiguration widgetsConf, String propertyName, String defaultValue )
    {
        this( widgetsConf, propertyName, defaultValue, false );
    }
    
    public FontProperty( Widget widget, String propertyName, String nameForDisplay, String defaultValue, boolean readonly )
    {
        this( null, widget, propertyName, nameForDisplay, defaultValue, readonly );
    }
    
    public FontProperty( Widget widget, String propertyName, String nameForDisplay, String defaultValue )
    {
        this( widget, propertyName, nameForDisplay, defaultValue, false );
    }
    
    public FontProperty( Widget widget, String propertyName, String defaultValue, boolean readonly )
    {
        this( widget, propertyName, propertyName, defaultValue, readonly );
    }
    
    public FontProperty( Widget widget, String propertyName, String defaultValue )
    {
        this( widget, propertyName, defaultValue, false );
    }
}
