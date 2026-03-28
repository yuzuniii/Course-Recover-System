package com.epda.crs.bean;

import com.epda.crs.dto.DashboardAnalyticsDTO;
import com.epda.crs.model.AuditLog;
import com.epda.crs.service.DashboardService;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;

// PrimeFaces 14 Chart Imports
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.donut.DonutChartDataSet;
import org.primefaces.model.charts.donut.DonutChartModel;
import org.primefaces.model.charts.donut.DonutChartOptions;
import org.primefaces.model.charts.optionconfig.legend.Legend;
import org.primefaces.model.charts.optionconfig.title.Title;
import org.primefaces.model.DefaultScheduleEvent;
import org.primefaces.model.DefaultScheduleModel;
import org.primefaces.model.ScheduleModel;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartOptions;
import org.primefaces.model.charts.axes.AxesGridLines;
import org.primefaces.model.charts.axes.cartesian.CartesianScales;
import org.primefaces.model.charts.axes.cartesian.category.CartesianCategoryAxes;
import org.primefaces.model.charts.axes.cartesian.category.CartesianCategoryTicks;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearAxes;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearTicks;
import org.primefaces.model.charts.line.LineChartModel;
import org.primefaces.model.charts.line.LineChartDataSet;
import org.primefaces.model.charts.line.LineChartOptions;
import org.primefaces.model.charts.optionconfig.tooltip.Tooltip;

@Named
@SessionScoped
public class DashboardBean implements Serializable {

    @EJB
    private DashboardService dashboardService;

    private DashboardAnalyticsDTO analytics;
    private DonutChartModel eligibilityChartModel;
    private ScheduleModel recoveryScheduleModel;
    private List<AuditLog> recentActivity;
    private BarChartModel cgpaChartModel;
    private LineChartModel usageTrendModel;

    // Component Performance Metrics
    private Long assignmentFailures = 0L;
    private Long midtermFailures = 0L;
    private Long finalExamFailures = 0L;
    private Long totalPasses = 0L;

    @PostConstruct
    public void init() {
        analytics = dashboardService.getAnalytics();
        createEligibilityChart();
        createRecoverySchedule();
        recentActivity = dashboardService.getRecentActivity();
        createCgpaChart();
        createUsageTrendModel();
    }

    public String getCourseName(Integer resultId) {
        return dashboardService.getCourseNameByResultId(resultId);
    }

    // Getters for UI
    public DashboardAnalyticsDTO getAnalytics() { return analytics; }
    public DonutChartModel getEligibilityChartModel() { return eligibilityChartModel; }
    public ScheduleModel getRecoveryScheduleModel() { return recoveryScheduleModel; }
    public List<AuditLog> getRecentActivity() { return recentActivity; }
    public BarChartModel getCgpaChartModel() { return cgpaChartModel; }
    public LineChartModel getUsageTrendModel() { return usageTrendModel; }
    public Long getAssignmentFailures() { return assignmentFailures; }
    public Long getMidtermFailures() { return midtermFailures; }
    public Long getFinalExamFailures() { return finalExamFailures; }
    public Long getTotalPasses() { return totalPasses; }

    private void createUsageTrendModel() {
        usageTrendModel = new LineChartModel();
        ChartData data = new ChartData();
        List<Map<String, Object>> trendData = dashboardService.getMultiLineUsageTrend();
        
        Map<String, Integer> authSeries = new LinkedHashMap<>();
        Map<String, Integer> dataSeries = new LinkedHashMap<>();
        Map<String, Integer> alertSeries = new LinkedHashMap<>();
        List<String> labels = new ArrayList<>();
        
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEE");
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dayName = date.format(dtf);
            labels.add(dayName);
            authSeries.put(dayName, 0);
            dataSeries.put(dayName, 0);
            alertSeries.put(dayName, 0);
        }
        
        for (Map<String, Object> row : trendData) {
            String day = (String) row.get("day");
            String cat = (String) row.get("category");
            int count = (int) row.get("count");
            if ("AUTH".equals(cat)) authSeries.put(day, authSeries.getOrDefault(day, 0) + count);
            else if ("DATA".equals(cat)) dataSeries.put(day, dataSeries.getOrDefault(day, 0) + count);
            else if ("ALERT".equals(cat)) alertSeries.put(day, alertSeries.getOrDefault(day, 0) + count);
        }

        // FIX: LineChartDataSet in PF14 uses List<Object>
        LineChartDataSet authSet = new LineChartDataSet();
        authSet.setData(new ArrayList<Object>(authSeries.values()));
        authSet.setLabel("Authentication");
        authSet.setBorderColor("#22c55e");
        authSet.setFill(false);

        LineChartDataSet dataSet = new LineChartDataSet();
        dataSet.setData(new ArrayList<Object>(dataSeries.values()));
        dataSet.setLabel("Data Entry");
        dataSet.setBorderColor("#3b82f6");
        dataSet.setFill(false);

        LineChartDataSet alertSet = new LineChartDataSet();
        alertSet.setData(new ArrayList<Object>(alertSeries.values()));
        alertSet.setLabel("System Alerts");
        alertSet.setBorderColor("#ef4444");
        alertSet.setFill(false);

        data.addChartDataSet(authSet);
        data.addChartDataSet(dataSet);
        data.addChartDataSet(alertSet);
        data.setLabels(labels);

        LineChartOptions options = new LineChartOptions();
        options.setMaintainAspectRatio(false);
        Legend legend = new Legend();
        legend.setDisplay(true);
        legend.setPosition("bottom");
        options.setLegend(legend);
        
        usageTrendModel.setOptions(options);
        usageTrendModel.setData(data);
    }

    private void createEligibilityChart() {
        eligibilityChartModel = new DonutChartModel();
        ChartData data = new ChartData();
        DonutChartDataSet dataSet = new DonutChartDataSet();
        Map<String, Long> statusCounts = dashboardService.getComponentChartData();
        
        assignmentFailures = statusCounts.getOrDefault("Assignment", 0L);
        midtermFailures = statusCounts.getOrDefault("Mid-Term", 0L);
        finalExamFailures = statusCounts.getOrDefault("Final Exam", 0L);
        totalPasses = statusCounts.getOrDefault("PASS", 0L);

        // FIX: DonutChartDataSet in PF14 strictly uses List<Number>
        List<Number> values = new ArrayList<>();
        values.add(assignmentFailures);
        values.add(midtermFailures);
        values.add(finalExamFailures);
        values.add(totalPasses);

        List<String> labels = new ArrayList<>();
        labels.add("Assignment");
        labels.add("Mid-Term");
        labels.add("Final Exam");
        labels.add("Pass");

        List<String> bgColors = new ArrayList<>();
        bgColors.add("#3b82f6"); bgColors.add("#f59e0b"); bgColors.add("#ef4444"); bgColors.add("#22c55e");

        dataSet.setData(values);
        dataSet.setBackgroundColor(bgColors);
        data.addChartDataSet(dataSet);
        data.setLabels(labels);

        DonutChartOptions options = new DonutChartOptions();
        options.setMaintainAspectRatio(false);
        eligibilityChartModel.setOptions(options);
        eligibilityChartModel.setData(data);
    }

    private void createRecoverySchedule() {
        recoveryScheduleModel = new DefaultScheduleModel();
        List<Map<String, Object>> dailyCounts = dashboardService.getDailyActionCounts();
        for (Map<String, Object> row : dailyCounts) {
            LocalDate logDate = (LocalDate) row.get("log_date");
            String actionType = (String) row.get("action_type");
            int count = (int) row.get("act_count");
            
            String color = "#6b7280";
            if (actionType.contains("LOGIN")) color = "#22c55e";
            else if (actionType.contains("CREATE") || actionType.contains("UPDATE")) color = "#3b82f6";
            else if (actionType.contains("DELETE") || actionType.contains("ERROR") || actionType.contains("FAIL")) color = "#ef4444";
            
            DefaultScheduleEvent<?> event = DefaultScheduleEvent.builder()
                    .title(count + " " + actionType)
                    .startDate(logDate.atStartOfDay())
                    .endDate(logDate.atStartOfDay())
                    .allDay(true)
                    .backgroundColor(color)
                    .borderColor(color)
                    .build();
            recoveryScheduleModel.addEvent(event);
        }
    }

    private void createCgpaChart() {
        cgpaChartModel = new BarChartModel();
        ChartData data = new ChartData();
        BarChartDataSet dataSet = new BarChartDataSet();

        Map<String, Double> sortedCgpaData = new LinkedHashMap<>();
        dashboardService.getAverageCgpaByMajor().entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(entry -> sortedCgpaData.put(entry.getKey(), entry.getValue()));

        List<Object> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<String> backgroundColors = new ArrayList<>();
        List<String> borderColors = new ArrayList<>();

        for (Map.Entry<String, Double> entry : sortedCgpaData.entrySet()) {
            double cgpa = entry.getValue() == null ? 0.0 : entry.getValue();
            labels.add(formatProgrammeLabel(entry.getKey()));
            values.add(cgpa);
            backgroundColors.add(getCgpaBarColor(cgpa));
            borderColors.add(getCgpaBorderColor(cgpa));
        }

        dataSet.setData(values);
        dataSet.setBackgroundColor(backgroundColors);
        dataSet.setBorderColor(borderColors);
        dataSet.setBorderWidth(1);
        dataSet.setBorderRadius(10);
        dataSet.setHoverBorderWidth(1);
        data.addChartDataSet(dataSet);
        data.setLabels(labels);
        cgpaChartModel.setData(data);

        BarChartOptions options = new BarChartOptions();
        options.setMaintainAspectRatio(false);
        options.setBarThickness(28);
        options.setMaxBarThickness(34);
        options.setCategoryPercentage(0.68);
        options.setBarPercentage(0.78);

        Legend legend = new Legend();
        legend.setDisplay(false);
        options.setLegend(legend);

        Tooltip tooltip = new Tooltip();
        tooltip.setEnabled(true);
        tooltip.setDisplayColors(false);
        tooltip.setBackgroundColor("rgba(15, 23, 42, 0.92)");
        tooltip.setTitleFontColor("#f8fafc");
        tooltip.setBodyFontColor("#e2e8f0");
        tooltip.setCornerRadius(10);
        tooltip.setXpadding(12);
        tooltip.setYpadding(10);
        options.setTooltip(tooltip);

        CartesianScales scales = new CartesianScales();

        CartesianCategoryAxes xAxis = new CartesianCategoryAxes();
        AxesGridLines xGrid = new AxesGridLines();
        xGrid.setDisplay(false);
        xGrid.setDrawBorder(false);
        xGrid.setDrawTicks(false);
        xAxis.setGrid(xGrid);

        CartesianCategoryTicks xTicks = new CartesianCategoryTicks();
        xTicks.setPadding(10);
        xTicks.setAutoSkip(false);
        xTicks.setMirror(false);
        xTicks.setMinRotation(18);
        xTicks.setMaxRotation(18);
        xAxis.setTicks(xTicks);

        CartesianLinearAxes yAxis = new CartesianLinearAxes();
        yAxis.setMin(0);
        yAxis.setMax(4.0);
        yAxis.setOffset(false);
        AxesGridLines yGrid = new AxesGridLines();
        yGrid.setDisplay(true);
        yGrid.setColor("rgba(148, 163, 184, 0.18)");
        yGrid.setDrawBorder(false);
        yGrid.setDrawTicks(false);
        yGrid.setZeroLineColor("rgba(59, 130, 246, 0.25)");
        yGrid.setZeroLineWidth(1);
        yAxis.setGrid(yGrid);

        CartesianLinearTicks yTicks = new CartesianLinearTicks();
        yTicks.setStepSize(1);
        yTicks.setPrecision(1);
        yTicks.setPadding(8);
        yTicks.setMaxTicksLimit(5);
        yAxis.setTicks(yTicks);

        scales.addXAxesData(xAxis);
        scales.addYAxesData(yAxis);
        options.setScales(scales);
        cgpaChartModel.setOptions(options);
    }

    private String formatProgrammeLabel(String programmeName) {
        if (programmeName == null || programmeName.isBlank()) {
            return "Unknown Programme";
        }

        String normalized = programmeName
                .replace("Bachelor of Information Technology", "Bachelor of IT")
                .replace("Bachelor of Computer Science", "Bachelor of Computer Science")
                .replace("Bachelor of ", "B. ");

        return normalized.length() > 30 ? normalized.substring(0, 27) + "..." : normalized;
    }

    private String getCgpaBarColor(double cgpa) {
        if (cgpa < 2.0) {
            return "rgba(239, 68, 68, 0.82)";
        }
        if (cgpa < 2.5) {
            return "rgba(245, 158, 11, 0.82)";
        }
        if (cgpa < 3.0) {
            return "rgba(59, 130, 246, 0.82)";
        }
        return "rgba(34, 197, 94, 0.82)";
    }

    private String getCgpaBorderColor(double cgpa) {
        if (cgpa < 2.0) {
            return "#dc2626";
        }
        if (cgpa < 2.5) {
            return "#d97706";
        }
        if (cgpa < 3.0) {
            return "#2563eb";
        }
        return "#16a34a";
    }
}
