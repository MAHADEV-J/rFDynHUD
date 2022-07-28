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
package net.ctdp.rfdynhud.values;

/**
 * This class is a container for runtime values.
 * You can update the value every time, a Widget is redrawn
 * and compare it with the old (previous) value.
 * 
 * @author Marvin Froehlich (CTDP)
 */
public class IntValue
{
    public static final int DEFAULT_RESET_VALUE = -1;
    public static final String N_A_VALUE = "N/A";
    
    private final int resetValue;
    private final ValidityTest validityTest;
    private final int validityCompareValue;
    private boolean oldValidity;
    private int oldValue;
    private int value;
    
    public final int getResetValue()
    {
        return ( resetValue );
    }
    
    public final ValidityTest getValdidityTest()
    {
        return ( validityTest );
    }
    
    public final int getValidityCompareValue()
    {
        return ( validityCompareValue );
    }
    
    public final int getOldValue()
    {
        return ( oldValue );
    }
    
    public final int getValue()
    {
        return ( value );
    }
    
    public final float getFloatValue()
    {
        return ( value );
    }
    
    public final boolean hasChanged( boolean setUnchanged )
    {
        boolean result = ( value != oldValue );
        
        if ( result && setUnchanged )
            update( value, true );
        
        return ( result );
    }
    
    public final boolean hasChanged()
    {
        return ( hasChanged( true ) );
    }
    
    private final boolean update( int newValue, boolean setUnchanged )
    {
        this.oldValidity = isValid();
        
        if ( setUnchanged )
            this.oldValue = value;
        this.value = newValue;
        
        return ( hasChanged( false ) );
    }
    
    public final boolean update( int newValue )
    {
        return ( update( newValue, false ) );
    }
    
    public final void setUnchanged()
    {
        update( value, true );
    }
    
    public final IntValue reset( boolean resetOldValue )
    {
        this.value = resetValue;
        
        if ( resetOldValue )
            oldValue = resetValue;
        
        this.oldValidity = false;
        
        return ( this );
    }
    
    public final IntValue reset()
    {
        return ( reset( false ) );
    }
    
    public final boolean isValid()
    {
        switch ( validityTest )
        {
            case EQUALS:
                return ( value == validityCompareValue );
            case NOT_EQUALS:
                return ( value != validityCompareValue );
            case GREATER_THAN:
                return ( value > validityCompareValue );
            case GRATER_THAN_OR_EQUALS:
                return ( value >= validityCompareValue );
            case LESS_THAN:
                return ( value < validityCompareValue );
            case LESS_THAN_OR_EQUALS:
                return ( value <= validityCompareValue );
        }
        
        return ( false );
    }
    
    public final boolean hasValidityChanged()
    {
        return ( isValid() != oldValidity );
    }
    
    public final String getValueAsStringWithSign()
    {
        if ( value == resetValue )
            return ( N_A_VALUE );
        
        if ( value > 0 )
            return ( "+" + String.valueOf( value ) );
        
        if ( value == -0 )
            return ( "0" ); // avoid "-0"
        
        return ( String.valueOf( value ) );
    }
    
    public final String getValueAsString()
    {
        if ( value == resetValue )
            return ( N_A_VALUE );
        
        if ( value == -0 )
            return ( "0" ); // avoid "-0"
        
        return ( String.valueOf( value ) );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return ( getValueAsString() );
    }
    
    public IntValue( int resetValue, ValidityTest validityTest, int validityCompareValue )
    {
        this.resetValue = resetValue;
        
        this.validityTest = validityTest;
        this.validityCompareValue = validityCompareValue;
        this.oldValidity = false;
        
        this.oldValue = resetValue;
        this.value = resetValue;
    }
    
    public IntValue( int resetValue )
    {
        this( resetValue, ValidityTest.GRATER_THAN_OR_EQUALS, 0 );
    }
    
    public IntValue( ValidityTest validityTest, int validityCompareValue )
    {
        this( DEFAULT_RESET_VALUE, validityTest, validityCompareValue );
    }
    
    public IntValue()
    {
        this( DEFAULT_RESET_VALUE );
    }
}
