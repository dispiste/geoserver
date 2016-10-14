/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.ncwms;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;

public class GetTimeSeries extends Response{
    public GetTimeSeries() {
        super(GetTimeSeries.class, "text/csv");
    }
    /*
    public GetTimeSeries(Class<?> binding, Set<String> outputFormats) {
        super(binding, outputFormats);
    }*/

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "text/csv";
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        // TODO Auto-generated method stub
    }

}
