package com.epda.crs.bean;

import com.epda.crs.dto.DashboardAnalyticsDTO;
import com.epda.crs.service.DashboardService;
import java.io.Serializable;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named
@ViewScoped
public class DashboardBean implements Serializable {
    @EJB
    private DashboardService dashboardService;

    private DashboardAnalyticsDTO analytics;

    @PostConstruct
    public void init() {
        analytics = dashboardService.getAnalytics();
    }

    public DashboardAnalyticsDTO getAnalytics() { return analytics; }
}
