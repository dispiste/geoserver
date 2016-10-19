/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wms.ncwms;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.FeatureInfoRequestParameters;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.featureinfo.LayerIdentifier;
import org.geotools.data.DataUtilities;
import org.geotools.data.Query;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.util.DateRange;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;

/**
 * @author Cesar Martinez Izquierdo
 *
 */
public class NcWmsService {
    public static final String TIME_SERIES_INFO_FORMAT_PARAM_NAME = "TIME_SERIES_INFO_FORMAT";
    public static final String GET_TIME_SERIES_REQUEST="GetTimeSeries";
    
    private WMS wms;
    
    public NcWmsService(final WMS wms) {
        this.wms = wms;
    }

    private LayerIdentifier getLayerIdentifier(MapLayerInfo layer) {
        List<LayerIdentifier> identifiers = GeoServerExtensions.extensions(LayerIdentifier.class);
        for (LayerIdentifier identifier : identifiers) {
            if (identifier.canHandle(layer)) {
                return identifier;
            }
        }

        throw new ServiceException("Could not find any identifier that can handle layer "
                + layer.getLayerInfo().prefixedName() + " among these identifiers: " + identifiers);
    }
    
    @SuppressWarnings("rawtypes")
    public FeatureCollectionType getTimeSeries(GetFeatureInfoRequest request) {
        FeatureCollectionType result = WfsFactory.eINSTANCE.createFeatureCollectionType();
        
        WfsFactory.eINSTANCE.createFeatureCollectionType();
        result.setTimeStamp(Calendar.getInstance());
        if (request.getGetMapRequest().getTime()!=null
                && request.getGetMapRequest().getTime().size()==1) {
            Object queryRangePlain = (DateRange) request.getGetMapRequest().getTime().get(0);
            if (queryRangePlain!=null && queryRangePlain instanceof DateRange) {
                DateRange queryRange = (DateRange) queryRangePlain;
                // FIXME: should we consider feature count parameter???
                // int numDays = request.getFeatureCount();
                int numDays = Query.DEFAULT_MAX;

                final List<MapLayerInfo> requestedLayers = request.getQueryLayers();
                FeatureInfoRequestParameters requestParams = new FeatureInfoRequestParameters(request);
                List<SimpleFeature> features = new ArrayList<>();

                // FIXME: should we also consider the request if it includes several layers ??
                if (requestedLayers.size()==1) {
                    final MapLayerInfo layer = requestedLayers.get(0);
                    SimpleFeatureBuilder featureBuilder = getResultFeatureBuilder(layer.getName());
                    try {
                        TreeSet availableDates = wms.queryCoverageTimes(layer.getCoverage(), queryRange, numDays);
                        for (Object d: availableDates.descendingSet()) {
                            System.out.println(d);
                            System.out.println(d.getClass());
                            Date date = (Date) d;
                            LayerIdentifier identifier = getLayerIdentifier(layer);
                            List<FeatureCollection> identifiedCollections = identifier.identify(requestParams,
                                    1);
                            for (FeatureCollection c: identifiedCollections) {
                                FeatureIterator featIter = c.features();
                                if (featIter.hasNext()) { // no need to loop, we just want one value
                                    Feature inFeat = featIter.next();
                                    Iterator<Property> propIter = inFeat.getProperties().iterator();
                                    if (propIter.hasNext()) {
                                        Property prop = propIter.next();
                                        featureBuilder.add(date);
                                        featureBuilder.add(prop.getValue());
                                        SimpleFeature newFeat = featureBuilder.buildFeature(null);
                                        features.add(newFeat);
                                    }
                                    
                                }
                                
                            }
                        }
                        result.getFeature().add(DataUtilities.collection(features));
                    } catch (IOException e) {
                    } catch (Exception e) {
                    }
                }
            }
        }

        return result;
    }
    
    private SimpleFeatureBuilder getResultFeatureBuilder(String name) {
        //create the builder
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        
        //set global state
        builder.setName(name);
        builder.setNamespaceURI( "http://www.geoserver.org/" );
        builder.setSRS( "EPSG:4326" );
        
        //add attributes
        builder.add( "date", Date.class );
        builder.add( "value", Double.class );
        SimpleFeatureType featureType = builder.buildFeatureType();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        return featureBuilder;
    }

}
