/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.ncwms;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.ows.Response;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.logging.Logging;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.springframework.util.Assert;

import com.vividsolutions.jts.geom.Coordinate;

import net.opengis.wfs.FeatureCollectionType;

public class GetTimeSeriesResponse extends Response{
    private static final Logger LOGGER = Logging.getLogger(GetTimeSeriesResponse.class);
    protected static final Set<String> outputFormats = new HashSet<String>();
    static {
        outputFormats.add("text/csv");
        outputFormats.add("image/png");
        outputFormats.add("image/jpg");
        outputFormats.add("image/jpeg");
    }
    
    private WMS wms;
    private final static String ISO8601_2000_UTC_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private final static int IMAGE_HEIGHT = 600, IMAGE_WIDTH = 700;
    
    public GetTimeSeriesResponse(final WMS wms) {
        super(FeatureCollectionType.class, outputFormats);
        this.wms = wms;
    }
    /**
     * @see org.geoserver.ows.Response#canHandle(org.geoserver.platform.Operation)
     */
    @Override
    public boolean canHandle(Operation operation) {
        return "GetTimeSeries".equalsIgnoreCase(operation.getId());
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        GetFeatureInfoRequest request = (GetFeatureInfoRequest) OwsUtils.parameter(
                operation.getParameters(), GetFeatureInfoRequest.class);
        String infoFormat = (String) request.getRawKvp().get("INFO_FORMAT");
        if (infoFormat!=null && outputFormats.contains(infoFormat.toLowerCase())) {
            return infoFormat;
        }
        // default
        return "text/csv";
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        Assert.notNull(value, "value is null");
        Assert.notNull(operation, "operation is null");
        Assert.isTrue(value instanceof FeatureCollectionType, "unrecognized result type:");
        Assert.isTrue(operation.getParameters() != null && operation.getParameters().length == 1
                && operation.getParameters()[0] instanceof GetFeatureInfoRequest);

        GetFeatureInfoRequest request = (GetFeatureInfoRequest) operation.getParameters()[0];
        FeatureCollectionType results = (FeatureCollectionType) value;

        String mime = getMimeType(value, operation);
        if (mime.startsWith("image/")) {
            writeChart(request, results, output, mime);
        }
        else {
            writeCsv(request, results, output);
        }
    }
    
    @SuppressWarnings("rawtypes")
    private void writeChart(GetFeatureInfoRequest request, FeatureCollectionType results, OutputStream output, String mimeType) {
        final TimeSeries series = new TimeSeries("time", Millisecond.class);
        String valueAxisLabel = "Value";
        
        Charset charSet = wms.getCharSet();
        OutputStreamWriter osw = new OutputStreamWriter(output, charSet);
        
        FeatureIterator reader = null;
        try {
            final List collections = results.getFeature();
            if (collections.size()==1) { // we only consider the result if a single layer was queried
                FeatureCollection fr;
                SimpleFeature f;
                fr = (FeatureCollection) collections.get(0);
                valueAxisLabel = fr.getSchema().getName().getLocalPart();
                
                reader = fr.features();
                while (reader.hasNext()) {
                    Feature feature = reader.next();
                    if (feature instanceof SimpleFeature)
                    {
                        f = (SimpleFeature) feature;
                        Date date = (Date) f.getAttribute("date");
                        Double value  = (Double) f.getAttribute("value");
                        
                        series.add(new Millisecond(date), value);
                    }
                }
            }
        } catch (Exception ife) {
            LOGGER.log(Level.WARNING, "Error generating getFeaturInfo, HTML format", ife);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        XYDataset dataset =  new TimeSeriesCollection(series);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(             
                "Time series of M", 
                "Date / time",
                valueAxisLabel,
                dataset,
                false,
                false,
                false);
        try {
            if (mimeType.startsWith("image/png")) {
                ChartUtilities.writeChartAsPNG(output, chart, IMAGE_WIDTH, IMAGE_HEIGHT);
            }
            else if (mimeType.equals("image/jpg") || mimeType.equals("image/jpeg")) {
                ChartUtilities.writeChartAsJPEG(output, chart, IMAGE_WIDTH, IMAGE_HEIGHT);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @SuppressWarnings("rawtypes")
    private void writeCsv(GetFeatureInfoRequest request, FeatureCollectionType results, OutputStream output) {
        Charset charSet = wms.getCharSet();
        OutputStreamWriter osw = new OutputStreamWriter(output, charSet);
        PrintWriter writer = new PrintWriter(osw);
        
        final Coordinate middle = WMS.pixelToWorld(request.getXPixel(),
                request.getYPixel(),
                new ReferencedEnvelope(request.getGetMapRequest().getBbox(), request.getGetMapRequest().getCrs()),
                request.getGetMapRequest().getWidth(),
                request.getGetMapRequest().getHeight());
        
        writer.println("# Latitude: "+middle.y);
        writer.println("# Longitude: "+middle.x);
        FeatureIterator reader = null;
        try {
            final List collections = results.getFeature();
            if (collections.size()==1) { // we only consider the result if a single layer was queried
                FeatureCollection fr;
                SimpleFeature f;
                fr = (FeatureCollection) collections.get(0);
                writer.println("Time (UTC),"+fr.getSchema().getName().getLocalPart());
                
                reader = fr.features();
                while (reader.hasNext()) {
                    Feature feature = reader.next();
                    if (feature instanceof SimpleFeature)
                    {
                        f = (SimpleFeature) feature;
                        Date date = (Date) f.getAttribute("date");
                        Double value  = (Double) f.getAttribute("value");
                        writer.println(formatAsISO8601_2000_UTC(date) + "," + value);
                    }
                }
            }
        } catch (Exception ife) {
            LOGGER.log(Level.WARNING, "Error generating getFeaturInfo, HTML format", ife);
            writer.println("Unable to generate information " + ife);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        writer.flush();
    }
    
    private String formatAsISO8601_2000_UTC(Date date) {
        DateFormat df = new SimpleDateFormat(ISO8601_2000_UTC_PATTERN);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(date);
    }

}
