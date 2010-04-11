package net.ctdp.rfdynhud.widgets.timecomp;

import java.io.IOException;

import net.ctdp.rfdynhud.editor.EditorPresets;
import net.ctdp.rfdynhud.editor.properties.BooleanProperty;
import net.ctdp.rfdynhud.gamedata.Laptime;
import net.ctdp.rfdynhud.gamedata.LiveGameData;
import net.ctdp.rfdynhud.gamedata.SessionType;
import net.ctdp.rfdynhud.gamedata.VehicleScoringInfo;
import net.ctdp.rfdynhud.input.InputAction;
import net.ctdp.rfdynhud.render.TextureImage2D;
import net.ctdp.rfdynhud.widgets._util.DrawnString;
import net.ctdp.rfdynhud.widgets._util.IntValue;
import net.ctdp.rfdynhud.widgets._util.Size;
import net.ctdp.rfdynhud.widgets._util.TimingUtil;
import net.ctdp.rfdynhud.widgets._util.WidgetPropertiesContainer;
import net.ctdp.rfdynhud.widgets._util.WidgetsConfigurationWriter;
import net.ctdp.rfdynhud.widgets._util.DrawnString.Alignment;
import net.ctdp.rfdynhud.widgets.widget.Widget;

/**
 * The {@link TimeCompareWidget} displays lap- and sector times of the last few laps to compare them.
 * 
 * @author Marvin Froehlich
 */
public class TimeCompareWidget extends Widget
{
    private final BooleanProperty abbreviate = new BooleanProperty( this, "abbreviate", false );
    private final BooleanProperty displaySectors = new BooleanProperty( this, "displaySectors", true );
    
    private DrawnString headerString = null;
    private DrawnString[] timeStrings = null;
    
    private final IntValue lap = new IntValue();
    
    private int numDisplayedLaps = 0;
    
    private static final Alignment[] colAligns = { Alignment.RIGHT, Alignment.RIGHT, Alignment.RIGHT, Alignment.RIGHT, Alignment.RIGHT };
    private static final int colPadding = 10;
    private int[] colWidths = null;
    
    @Override
    public String getWidgetPackage()
    {
        return ( "" );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected Object createLocalStore()
    {
        return ( new LocalStore() );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onSessionStarted( boolean isEditorMode, SessionType sessionType, LiveGameData gameData )
    {
        super.onSessionStarted( isEditorMode, sessionType, gameData );
        
        lap.reset();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onRealtimeEntered( boolean isEditorMode, LiveGameData gameData )
    {
        super.onRealtimeEntered( isEditorMode, gameData );
    }
    
    protected String getGapRowCaption()
    {
        return ( "g" );
    }
    
    private void updateLaps( int lap, LiveGameData gameData )
    {
        LocalStore store = (LocalStore)getLocalStore();
        
        VehicleScoringInfo vsi = gameData.getScoringInfo().getPlayersVehicleScoringInfo();
        
        int n = 0;
        float sumS1 = 0f;
        float sumS2 = 0f;
        float sumS3 = 0f;
        float sumL = 0f;
        for ( int i = lap - 1; i >= 1 && n < numDisplayedLaps; i-- )
        {
            Laptime lt = vsi.getLaptime( i );
            if ( ( lt != null ) && ( lt.isInlap() != Boolean.TRUE ) && ( lt.isOutlap() != Boolean.TRUE ) && ( lt.getLapTime() > 0f ) )
            {
                store.displayedLaps[numDisplayedLaps - n - 1] = lt;
                sumS1 += lt.getSector1();
                sumS2 += lt.getSector2();
                sumS3 += lt.getSector3();
                sumL += lt.getLapTime();
                
                n++;
            }
        }
        
        store.avgS1 = sumS1 / n;
        store.avgS2 = sumS2 / n;
        store.avgS3 = sumS3 / n;
        store.avgL = sumL / n;
        
        if ( n < numDisplayedLaps )
        {
            System.arraycopy( store.displayedLaps, numDisplayedLaps - n, store.displayedLaps, 0, n );
            
            for ( int i = n; i < numDisplayedLaps; i++ )
                store.displayedLaps[i] = null;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onPlayerLapStarted( boolean isEditorMode, LiveGameData gameData )
    {
        super.onPlayerLapStarted( isEditorMode, gameData );
        
        VehicleScoringInfo vsi = gameData.getScoringInfo().getPlayersVehicleScoringInfo();
        
        lap.update( vsi.getCurrentLap() );
        
        updateLaps( lap.getValue(), gameData );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onBoundInputStateChanged( boolean isEditorMode, InputAction action, boolean state, int modifierMask )
    {
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void initialize( boolean clock1, boolean clock2, LiveGameData gameData, EditorPresets editorPresets, TextureImage2D texture, int offsetX, int offsetY, int width, int height )
    {
        final java.awt.Font font = getFont();
        final boolean fontAntiAliased = isFontAntiAliased();
        final java.awt.Color fontColor = getFontColor();
        
        LocalStore store = (LocalStore)getLocalStore();
        
        headerString = new DrawnString( 0, 0, Alignment.LEFT, false, font, fontAntiAliased, fontColor );
        
        int h = height + getBorder().getInnerBottomHeight() - getBorder().getOpaqueBottomHeight();
        int rowHeight = headerString.getMaxHeight( texture, false );
        numDisplayedLaps = Math.max( 1, ( h - rowHeight - rowHeight - 5 ) / rowHeight );
        
        store.displayedLaps = new Laptime[ numDisplayedLaps ];
        int numStrings = displaySectors.getBooleanValue() ? 5 : 2;
        this.colWidths = new int[ numStrings ];
        
        this.timeStrings = new DrawnString[ numDisplayedLaps + 1 ];
        
        DrawnString relY = headerString;
        for ( int j = 0; j < numDisplayedLaps; j++ )
        {
            timeStrings[j] = new DrawnString( null, relY, 0, 0, Alignment.LEFT, false, font, fontAntiAliased, fontColor );
            relY = timeStrings[j];
        }
        
        timeStrings[numDisplayedLaps] = new DrawnString( null, relY, 0, 5, Alignment.LEFT, false, font, fontAntiAliased, fontColor );
        
        if ( displaySectors.getBooleanValue() )
        {
            if ( abbreviate.getBooleanValue() )
                headerString.getMaxColWidths( new String[] { "#", "Sec1", "Sec2", "Sec3", "Lap" }, colAligns, colPadding, texture, colWidths );
            else
                headerString.getMaxColWidths( new String[] { "#", "Sector1", "Sector2", "Sector3", "Lap" }, colAligns, colPadding, texture, colWidths );
            timeStrings[0].getMaxColWidths( new String[] { "00", "-00.000", "-00.000", "-00.000", "-0:00.000" }, colAligns, colPadding, texture, colWidths );
        }
        else
        {
            headerString.getMaxColWidths( new String[] { "#", "Lap" }, colAligns, colPadding, texture, colWidths );
            timeStrings[0].getMaxColWidths( new String[] { "00", "-00.000" }, colAligns, colPadding, texture, colWidths );
        }
        
        int currentLap = gameData.getScoringInfo().getPlayersVehicleScoringInfo().getCurrentLap();
        if ( currentLap > 0 )
            updateLaps( currentLap, gameData );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void drawWidget( boolean clock1, boolean clock2, boolean needsCompleteRedraw, LiveGameData gameData, EditorPresets editorPresets, TextureImage2D texture, int offsetX, int offsetY, int width, int height )
    {
        final java.awt.Color backgroundColor = getBackgroundColor();
        
        LocalStore store = (LocalStore)getLocalStore();
        
        int padding = 10;
        
        if ( needsCompleteRedraw )
        {
            if ( displaySectors.getBooleanValue() )
            {
                if ( abbreviate.getBooleanValue() )
                    headerString.drawColumns( offsetX, offsetY, new String[] { "#", "Sec1", "Sec2", "Sec3", "Lap" }, colAligns, padding, colWidths, backgroundColor, texture );
                else
                    headerString.drawColumns( offsetX, offsetY, new String[] { "#", "Sector1", "Sector2", "Sector3", "Lap" }, colAligns, padding, colWidths, backgroundColor, texture );
            }
            else
            {
                headerString.drawColumns( offsetX, offsetY, new String[] { "#", "Lap" }, colAligns, padding, colWidths, backgroundColor, texture );
            }
        }
        
        if ( needsCompleteRedraw || lap.hasChanged() )
        {
            int last = -1;
            for ( int i = 0; i < numDisplayedLaps; i++ )
            {
                String[] s;
                if ( store.displayedLaps[i] == null )
                {
                    if ( displaySectors.getBooleanValue() )
                        s = new String[] { "--", "--.---", "--.---", "--.---", "-:--.---" };
                    else
                        s = new String[] { "--", "-:--.---" };
                }
                else
                {
                    if ( displaySectors.getBooleanValue() )
                        s = new String[] { String.valueOf( store.displayedLaps[i].getLap() ), TimingUtil.getTimeAsString( store.displayedLaps[i].getSector1(), true ), TimingUtil.getTimeAsString( store.displayedLaps[i].getSector2(), true ), TimingUtil.getTimeAsString( store.displayedLaps[i].getSector3(), true ), TimingUtil.getTimeAsString( store.displayedLaps[i].getLapTime(), true ) };
                    else
                        s = new String[] { String.valueOf( store.displayedLaps[i].getLap() ), TimingUtil.getTimeAsString( store.displayedLaps[i].getLapTime(), true ) };
                    
                    last = i;
                }
                
                timeStrings[i].drawColumns( offsetX, offsetY, s, colAligns, padding, colWidths, backgroundColor, texture );
            }
            
            String[] s;
            if ( last < 0 )
            {
                if ( displaySectors.getBooleanValue() )
                    s = new String[] { getGapRowCaption(), "--.---", "--.---", "--.---", "-:--.---" };
                else
                    s = new String[] { getGapRowCaption(), "-:--.---" };
            }
            else
            {
                Laptime lt = store.displayedLaps[last];
                
                if ( displaySectors.getBooleanValue() )
                    s = new String[] { getGapRowCaption(), TimingUtil.getTimeAsGapString( lt.getSector1() - store.avgS1 ), TimingUtil.getTimeAsGapString( lt.getSector2() - store.avgS2 ), TimingUtil.getTimeAsGapString( lt.getSector3() - store.avgS3 ), TimingUtil.getTimeAsGapString( lt.getLapTime() - store.avgL ) };
                else
                    s = new String[] { getGapRowCaption(), TimingUtil.getTimeAsGapString( lt.getLapTime() - store.avgL ) };
            }
            
            timeStrings[numDisplayedLaps].drawColumns( offsetX, offsetY, s, colAligns, padding, colWidths, backgroundColor, texture );
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void saveProperties( WidgetsConfigurationWriter writer ) throws IOException
    {
        super.saveProperties( writer );
        
        writer.writeProperty( abbreviate, "Whether to abbreviate \"Sector\" to \"Sec\", or not." );
        writer.writeProperty( displaySectors, "Display sector times?" );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void loadProperty( String key, String value )
    {
        super.loadProperty( key, value );
        
        if ( abbreviate.loadProperty( key, value ) );
        else if ( displaySectors.loadProperty( key, value ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void getProperties( WidgetPropertiesContainer propsCont )
    {
        super.getProperties( propsCont );
        
        propsCont.addGroup( "Specific" );
        
        propsCont.addProperty( abbreviate );
        propsCont.addProperty( displaySectors );
    }
    
    public TimeCompareWidget( String name )
    {
        super( name, Size.PERCENT_OFFSET + 0.240625f, Size.PERCENT_OFFSET + 0.13916667f );
    }
}
