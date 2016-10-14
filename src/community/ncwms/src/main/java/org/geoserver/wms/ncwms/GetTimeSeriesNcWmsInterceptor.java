/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.ncwms;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WebMapService;

/**
 * 
 * @author Cesar Martinez Izquierdo
 *
 */
public class GetTimeSeriesNcWmsInterceptor implements MethodInterceptor {

    private WMS wms;
    private WebMapService webMapService;

    public GetTimeSeriesNcWmsInterceptor(WMS wms, WebMapService webMapService) {
        this.wms = wms;
        this.webMapService = webMapService;
    }
    
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if("GetTimeSeries".equals(invocation.getMethod().getName())) {
            /*
            try {
                GetMapRequest getMap = (GetMapRequest) invocation.getArguments()[0];
                return KMLReflector.doWms(getMap, webMapService, wms);
            } catch (Exception e) {
                if(e instanceof ServiceException) {
                    throw e;
                } else {
                    throw new ServiceException(e);
                }
            }*/
            System.out.println("Hello GetTimeSeries");
            return new GetTimeSeries();
        } else {
            return invocation.proceed();
        }
    }


}