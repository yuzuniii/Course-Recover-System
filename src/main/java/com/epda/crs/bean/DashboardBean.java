package com.epda.crs.bean;

import com.epda.crs.dto.DashboardAnalyticsDTO;
import com.epda.crs.model.AuditLog;
import com.epda.crs.model.RecoveryPlan;
import com.epda.crs.service.DashboardService;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

// Correct PrimeFaces 14 Chart Imports
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.donut.DonutChartDataSet;
import org.primefaces.model.charts.donut.DonutChartModel;
import org.primefaces.model.charts.donut.DonutChartOptions;
import org.primefaces.model.charts.optionconfig.title.Title;

// Correct PrimeFaces 14 Schedule Imports (Removed the '.schedule' part)
import org.primefaces.model.DefaultScheduleEvent;
import org.primefaces.model.DefaultScheduleModel;
import org.primefaces.model.ScheduleModel;

// Correct PrimeFaces 14 Bar Chart Imports
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartOptions;
import org.primefaces.model.charts.axes.cartesian.CartesianScales;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearAxes;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearTicks;
import org.primefaces.model.charts.optionconfig.legend.Legend;
import org.primefaces.model.charts.line.LineChartModel;
import org.primefaces.model.charts.line.LineChartDataSet;
import org.primefaces.model.charts.line.LineChartOptions;

@Named
@ViewScoped
public class DashboardBean implements Serializable {

    @Inject
    private DashboardService dashboardService;

    private DashboardAnalyticsDTO analytics;
    private DonutChartModel eligibilityChartModel;
    private ScheduleModel recoveryScheduleModel;
    private List<AuditLog> recentActivity;
    private BarChartModel cgpaChartModel;
    private LineChartModel usageTrendModel;

    @PostConstruct
    public void init() {
        analytics = dashboardService.getAnalytics();
        createEligibilityChart();
        createRecoverySchedule();
        recentActivity = dashboardService.getRecentActivity();
        createCgpaChart();
        createUsageTrendChart();
    }

    private void createUsageTrendChart() {
        usageTrendModel = new LineChartModel();
        ChartData data = new ChartData();

        LineChartDataSet dataSet = new LineChartDataSet();
        List<Object> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        java.util.Map<String, Integer> trendMap = dashboardService.getUsageTrend();
        if (trendMap != null) {
            for (java.util.Map.Entry<String, Integer> entry : trendMap.entrySet()) {
                labels.add(entry.getKey());
                values.add(entry.getValue());
            }
        }

        dataSet.setData(values);
        dataSet.setLabel("System Actions");
        dataSet.setFill(true);
        dataSet.setTension(0.4);
        dataSet.setBorderColor("rgb(139, 92, 246)");
        dataSet.setBackgroundColor("rgba(139, 92, 246, 0.1)");
        
        data.addChartDataSet(dataSet);
        data.setLabels(labels);

        LineChartOptions options = new LineChartOptions();
        Title title = new Title();
        title.setDisplay(true);
        title.setText("7-Day System Usage Trend");
        options.setTitle(title);
        
        usageTrendModel.setOptions(options);
        usageTrendModel.setData(data);
    }

    private void createEligibilityChart() {
        eligibilityChartModel = new DonutChartModel();
        
        // 1. Setup Options & Title
        DonutChartOptions options = new DonutChartOptions();
        options.setMaintainAspectRatio(false);
        Title title = new Title();
        title.setDisplay(true);
        title.setText("Eligibility Status");
        options.setTitle(title);
        eligibilityChartModel.setOptions(options);

        // 2. Map Database Data to PrimeFaces ChartData
        ChartData data = new ChartData();
        DonutChartDataSet dataSet = new DonutChartDataSet();
        List<Number> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<String> bgColors = new ArrayList<>();

        Map<String, Long> chartDataMap = dashboardService.getEligibilityChartData();
        if (chartDataMap != null) {
            for (Map.Entry<String, Long> entry : chartDataMap.entrySet()) {
                labels.add(entry.getKey());
                values.add(entry.getValue());
                
                // Color Code: Green for Eligible, Red for Not Eligible
                if ("Eligible".equalsIgnoreCase(entry.getKey())) {
                    bgColors.add("rgb(16, 185, 129)"); 
                } else {
                    bgColors.add("rgb(239, 68, 68)");  
                }
            }
        }
        
        dataSet.setData(values);
        dataSet.setBackgroundColor(bgColors);
        data.addChartDataSet(dataSet);
        data.setLabels(labels);

        eligibilityChartModel.setData(data);
    }

    private void createRecoverySchedule() {
        recoveryScheduleModel = new DefaultScheduleModel();
        List<RecoveryPlan> completedPlans = dashboardService.getCompletedRecoveryPlans();
        
        if (completedPlans != null) {
            for (RecoveryPlan plan : completedPlans) {
                if (plan.getEndDate() != null) {
                    // PrimeFaces 14 uses a Builder pattern for events
                    DefaultScheduleEvent<?> event = DefaultScheduleEvent.builder()
                        .title(plan.getStudent().getFullName() + " - " + plan.getCourse().getCourseCode())
                        .startDate(plan.getEndDate().atStartOfDay())
                        .endDate(plan.getEndDate().atStartOfDay())
                        .allDay(true)
                        .styleClass("bg-green-500 text-white")
                        .build();
                    recoveryScheduleModel.addEvent(event);
                }
            }
        }
    }

    private void createCgpaChart() {
        cgpaChartModel = new BarChartModel();
        ChartData data = new ChartData();

        BarChartDataSet barDataSet = new BarChartDataSet();
        barDataSet.setLabel("Average CGPA");
        barDataSet.setBackgroundColor("rgba(59, 130, 246, 0.8)"); // Modern Tech Blue
        barDataSet.setBorderColor("rgb(59, 130, 246)");
        barDataSet.setBorderWidth(1);

        // FIX 1: Change List<Number> to List<Object>
        List<Object> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        // Fetch data from service
        Map<String, Double> avgData = dashboardService.getAverageCgpaByMajor();
        if (avgData != null) {
            for (Map.Entry<String, Double> entry : avgData.entrySet()) {
                labels.add(entry.getKey());
                values.add(entry.getValue());
            }
        }

        barDataSet.setData(values);
        data.addChartDataSet(barDataSet);
        data.setLabels(labels);
        cgpaChartModel.setData(data);

        // Chart Options
        BarChartOptions options = new BarChartOptions();
        CartesianScales cScales = new CartesianScales();
        CartesianLinearAxes linearAxes = new CartesianLinearAxes();
        
        // FIX 2: Apply setMin and setMax directly to the Axis, not the Ticks
        linearAxes.setMin(0);
        linearAxes.setMax(4.0);
        
        cScales.addYAxesData(linearAxes);
        options.setScales(cScales);

        // Hide legend to keep it clean
        org.primefaces.model.charts.optionconfig.legend.Legend legend = new org.primefaces.model.charts.optionconfig.legend.Legend();
        legend.setDisplay(false);
        options.setLegend(legend);

        cgpaChartModel.setOptions(options);
    }

    // Getters required by JSF
    public DashboardAnalyticsDTO getAnalytics() { return analytics; }
    public DonutChartModel getEligibilityChartModel() { return eligibilityChartModel; }
    public ScheduleModel getRecoveryScheduleModel() { return recoveryScheduleModel; }
    public List<AuditLog> getRecentActivity() { return recentActivity; }
    public BarChartModel getCgpaChartModel() { return cgpaChartModel; }
    public LineChartModel getUsageTrendModel() { return usageTrendModel; }
}