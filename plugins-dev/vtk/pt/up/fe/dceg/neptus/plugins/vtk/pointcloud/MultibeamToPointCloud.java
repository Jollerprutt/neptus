/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: hfq
 * Apr 26, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk.pointcloud;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Date;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.mra.NeptusMRA;
import pt.up.fe.dceg.neptus.mra.api.BathymetryInfo;
import pt.up.fe.dceg.neptus.mra.api.BathymetryParser;
import pt.up.fe.dceg.neptus.mra.api.BathymetryParserFactory;
import pt.up.fe.dceg.neptus.mra.api.BathymetryPoint;
import pt.up.fe.dceg.neptus.mra.api.BathymetrySwath;
import pt.up.fe.dceg.neptus.mra.importers.IMraLog;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import pt.up.fe.dceg.neptus.plugins.vtk.utils.MultibeamDeltaTHeader;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.bathymetry.LocalData;

/**
 * @author hfq
 *
 */
public class MultibeamToPointCloud {
    
    public IMraLogGroup source;
    public IMraLog state;

    public BathymetryInfo batInfo;
    
//    private File file;                          // *.83P file
//    private FileInputStream fileInputStream;    // 83P file input stream
//    private FileChannel channel;                // SeekableByteChanel connected to the file (83P)
//    private ByteBuffer buf;
    
    public BathymetryParser multibeamDeltaTParser;
    public PointCloud<PointXYZ> pointCloud;    
    private LocalData ld;

    /**
     * @param log
     * @param pointCloud
     */
    public MultibeamToPointCloud(IMraLogGroup log, PointCloud<PointXYZ> pointCloud) {
        this.source = log;
        this.pointCloud = pointCloud;
    }

    private double getTideOffset(long timestampMillis) {
        try {
            return ld.getTidePrediction(new Date(timestampMillis), false);
        }
        catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    public void parseMultibeamPointCloud (boolean approachToIgnorePts, int ptsToIgnore, long timestampMultibeamIncrement, boolean yawMultibeamIncrement) {
        multibeamDeltaTParser = BathymetryParserFactory.build(this.source);
        ld = new LocalData(this.source.getFile("mra/tides.txt"));
        
        multibeamDeltaTParser.rewind();
        BathymetrySwath bs;
        
        int countPoints = 0;
        
        while ((bs = multibeamDeltaTParser.nextSwath()) != null) {
//            LocationType loc = bs.getPose().getPosition().convertToAbsoluteLatLonDepth();
//            double lat = loc.getLatitudeAsDoubleValue();
//            double lon = loc.getLongitudeAsDoubleValue();                     
            LocationType loc = bs.getPose().getPosition();

            double tideOffset = getTideOffset(bs.getTimestamp());
            
            if (!approachToIgnorePts) {
                for (int c = 0; c < bs.numBeams; c += ptsToIgnore) {
                    BathymetryPoint p = bs.getData()[c];
                    if (p == null)
                        continue;

                    // gets offset north and east and adds with bathymetry point tempPoint.north and tempoPoint.east respectively
                    LocationType tempLoc = new LocationType(loc);
                    // add data to pointcloud
                    pointCloud.getVerts().InsertNextCell(1);
                    tempLoc.translatePosition(p.north, p.east, 0);
                    pointCloud.getVerts().InsertCellPoint(
                            pointCloud.getPoints().InsertNextPoint(tempLoc.getOffsetNorth(), tempLoc.getOffsetEast(),
                                    p.depth - tideOffset));

                    ++countPoints;
                }
            }
            else {
                for (int c = 0; c < bs.numBeams; c++) {
                    //if (Math.random() > 1.0 / ptsToIgnore)
                    if (Math.random() > 1.0 / NeptusMRA.ptsToIgnore)
                        continue;

                    BathymetryPoint p = bs.getData()[c];
                    if (p == null)
                        continue;
                        // gets offset north and east and adds with bathymetry point tempPoint.north and tempoPoint.east respectively
                    LocationType tempLoc = new LocationType(loc);                         
                    pointCloud.getVerts().InsertNextCell(1);
                    tempLoc.translatePosition(p.north, p.east, 0);
                    pointCloud.getVerts().InsertCellPoint(pointCloud.getPoints().InsertNextPoint(tempLoc.getOffsetNorth(), 
                            tempLoc.getOffsetEast(), 
                            p.depth - tideOffset));
                    
//                  pointCloud.getVerts().InsertCellPoint(pointCloud.getPoints().InsertNextPoint(tempLoc.getAbsoluteNEDInMeters()[0],
//                  tempLoc.getAbsoluteNEDInMeters()[1],
//                  p.depth));
                
                    ++countPoints;
                }
            }
        }
        
        multibeamDeltaTParser.getBathymetryInfo().totalNumberOfPoints = countPoints;
        batInfo = multibeamDeltaTParser.getBathymetryInfo();

        pointCloud.setNumberOfPoints(multibeamDeltaTParser.getBathymetryInfo().totalNumberOfPoints);
    }
    
//    /**
//     * 
//     */
//    private void getMyDeltaTHeader() {
//        file = source.getFile("multibeam.83P");  
//        //System.out.println("print parent: " + file.toString());
//        try {
//            fileInputStream = new FileInputStream(file);
//        }
//        catch (FileNotFoundException e) {
//            NeptusLog.pub().info("File not found: " + e);        
//            e.printStackTrace();
//        }
//        catch (IOException ioe) {
//            NeptusLog.pub().info("Exception while reading the file: " + ioe);
//            ioe.printStackTrace();
//        }
//    
//        channel = fileInputStream.getChannel();      
//        long posOnFile = 0;
//        long sizeOfRegionToMap = 256;   // 256 bytes currespondent to the header of each ping         
//        try {
//            buf = channel.map(MapMode.READ_ONLY, posOnFile, sizeOfRegionToMap);
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        } 
//        
//        MultibeamDeltaTHeader deltaTHeader = new MultibeamDeltaTHeader(buf);
//        deltaTHeader.parseHeader();     
//    }
}
