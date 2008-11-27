/*
 * $Id: $
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.eumetsat.metop.binio;

import java.util.Map;

public class EpsAsciiMetatData {

    private String type;
    private String description;
    private String units;
    private String scalingFactor;
    private Map<String, String> itemMap;

    public void setType(String typeString) {
        this.type = typeString;
    }
    
    public String getType() {
        return type;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
    
    public void setUnits(String units) {
        this.units = units;
    }
    
    public String getUnits() {
        return units;
    }

    public void setScalingFactor(String scalingFactor) {
        this.scalingFactor = scalingFactor;
    }
    
    public String getScalingFactor() {
        return scalingFactor;
    }

    public void setItems(Map<String, String> itemMap) {
        this.itemMap = itemMap;
    }
    
    public Map<String, String> getItems() {
        return itemMap;
    }
    

    

    

    
   

}
