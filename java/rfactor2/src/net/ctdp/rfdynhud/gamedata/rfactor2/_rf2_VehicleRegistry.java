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
package net.ctdp.rfdynhud.gamedata.rfactor2;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ctdp.rfdynhud.gamedata.VehicleInfo;

/**
 * Provides {@link VehicleInfo} instances.
 * 
 * @author Marvin Froehlich (CTDP)
 */
class _rf2_VehicleRegistry
{
    private final List<VehicleInfo> vehicles = new ArrayList<VehicleInfo>();
    private final Map<String, VehicleInfo> driverVehicleMap = new HashMap<String, VehicleInfo>();
    
    /**
     * 
     * @param vehicleFilter
     * @param vehiclesFolder
     */
    public void update( String[] vehicleFilter, File vehiclesFolder )
    {
        vehicles.clear();
        driverVehicleMap.clear();
        
        //if ( __EDPrivilegedAccess.editorClassLoader == null )
        //    findVehicleFiles( vehicleFilter, vehiclesFolder );
        //else
        //    loadFactoryDefaultVehicles();
        
        for ( int i = 0; i < vehicles.size(); i++ )
        {
            driverVehicleMap.put( vehicles.get( i ).getDriverDescription(), vehicles.get( i ) );
        }
    }
    
    public final VehicleInfo getVehicleForDriver( String vehicleName )
    {
        return ( driverVehicleMap.get( vehicleName ) );
    }
    
    public _rf2_VehicleRegistry()
    {
    }
}
