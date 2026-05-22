package com.company.lms.controller;

import com.company.lms.model.Employee;
import com.company.lms.model.LeaveRequest;
import com.company.lms.model.LeaveType;
import com.company.lms.service.ManagerLeaveService;
import com.company.lms.service.StatisticsService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.DefaultScheduleEvent;
import org.primefaces.model.DefaultScheduleModel;
import org.primefaces.model.ScheduleModel;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartOptions;
import org.primefaces.model.charts.line.LineChartDataSet;
import org.primefaces.model.charts.line.LineChartModel;
import org.primefaces.model.charts.line.LineChartOptions;
import org.primefaces.model.charts.pie.PieChartDataSet;
import org.primefaces.model.charts.pie.PieChartModel;
import org.primefaces.model.charts.pie.PieChartOptions;
import org.primefaces.model.charts.optionconfig.legend.Legend;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Named
@ViewScoped
public class DashboardController implements Serializable {

    @Inject
    private StatisticsService statsService;

    @Inject
    private ManagerLeaveService managerService;

    @Inject
    private LoginController loginController;

    private PieChartModel pieModel;
    private BarChartModel barModel;
    private LineChartModel trendModel;
    private ScheduleModel teamScheduleModel;
    
    private LocalDateTime lastUpdated;
    private long totalRequests;
    private long pendingCount;
    private long approvedCount;
    private long rejectedCount;
    private int leaveUtilizationPercent;
    private long longestPendingRequestAge;
    private List<LeaveRequest> onLeaveThisWeek;
    private List<LeaveType> activeBarLeaveTypes = new ArrayList<>();
    private List<ChartLegendItem> statusLegendItems = new ArrayList<>();
    private List<LeaveType> trendLeaveTypes = new ArrayList<>();

    @PostConstruct
    public void init() {
        this.lastUpdated = LocalDateTime.now();
        createPieModelAndKPIs();
        createBarModel();
        createManagerWidgets();
        createTrendModel();
        createTeamScheduleModel();
    }

    private void createPieModelAndKPIs() {
        pieModel = new PieChartModel();
        statusLegendItems = new ArrayList<>();
        Employee manager = loginController.getLoggedInUser();
        if (manager == null) {
            return;
        }
        ChartData data = new ChartData();
        PieChartDataSet dataSet = new PieChartDataSet();

        Map<String, Long> stats = statsService.getLeavesByStatus(manager.getId());
        List<Number> values = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<String> bgColors = new ArrayList<>();

        long total = 0;
        long approved = 0;
        long pending = 0;
        long rejected = 0;

        for (Map.Entry<String, Long> entry : stats.entrySet()) {
            String status = entry.getKey();
            Long count = entry.getValue();
            
            labels.add(status);
            values.add(count);
            total += count;

            String color;

            if ("Εγκρίθηκε".equals(status)) {
                color = "#28a745";
                approved = count;
            } else if ("Εκκρεμεί".equals(status)) {
                color = "#ed5929";
                pending = count;
            } else {
                color = "#dc3545";
                rejected += count;
            }

            bgColors.add(color);
            statusLegendItems.add(new ChartLegendItem(status, color));
        }

        this.totalRequests = total;
        this.approvedCount = approved;
        this.pendingCount = pending;
        this.rejectedCount = rejected;

        dataSet.setData(values);
        dataSet.setBackgroundColor(bgColors);
        data.addChartDataSet(dataSet);
        data.setLabels(labels);
        pieModel.setData(data);

        PieChartOptions options = new PieChartOptions();
        options.setMaintainAspectRatio(false);

        Legend legend = new Legend();
        legend.setDisplay(false);
        options.setLegend(legend);

        pieModel.setOptions(options);
    }

    private void createBarModel() {
        barModel = new BarChartModel();

        Employee manager = loginController.getLoggedInUser();

        if (manager == null) {
            return;
        }

        ChartData data = new ChartData();

        BarChartDataSet barDataSet = new BarChartDataSet();

        Map<String, Long> stats = statsService.getLeavesByType(manager.getId());

        List<String> labels = new ArrayList<>();
        List<Number> values = new ArrayList<>();
        List<String> backgroundColors = new ArrayList<>();
        List<String> borderColors = new ArrayList<>();

        activeBarLeaveTypes = new ArrayList<>();

        for (LeaveType type : LeaveType.values()) {
            long count = stats.getOrDefault(type.getDisplayName(), 0L);

            if (count > 0) {
                labels.add(type.getDisplayName());
                values.add(count);

                backgroundColors.add(type.getColor());
                borderColors.add(type.getColor());

                activeBarLeaveTypes.add(type);
            }
        }

        barDataSet.setData(values);
        barDataSet.setBackgroundColor(backgroundColors);
        barDataSet.setBorderColor(borderColors);
        barDataSet.setBorderWidth(1);

        data.addChartDataSet(barDataSet);
        data.setLabels(labels);

        barModel.setData(data);

        BarChartOptions options = new BarChartOptions();
        options.setMaintainAspectRatio(false);

        Legend legend = new Legend();
        legend.setDisplay(false);
        options.setLegend(legend);

        barModel.setOptions(options);
    }

    private void createManagerWidgets() {
        Employee manager = loginController.getLoggedInUser();
        onLeaveThisWeek = new ArrayList<>();
        leaveUtilizationPercent = 0;
        longestPendingRequestAge = 0;

        if (manager == null) {
            return;
        }

        int usedDays = managerService.getTeamApprovedDaysThisYear(manager);
        int remainingDays = managerService.getTeamRemainingBalance(manager);
        int totalDays = usedDays + remainingDays;
        leaveUtilizationPercent = totalDays == 0 ? 0 : Math.round((usedDays * 100f) / totalDays);
        longestPendingRequestAge = managerService.getOldestPendingDays(manager);
        onLeaveThisWeek = managerService.getOnLeaveThisWeek(manager);
    }

    private void createTrendModel() {
        trendLeaveTypes = new ArrayList<>();
        trendModel = new LineChartModel();
        ChartData data = new ChartData();
        Employee manager = loginController.getLoggedInUser();

        List<String> labels = Arrays.asList("Ιαν", "Φεβ", "Μαρ", "Απρ", "Μαι", "Ιουν", "Ιουλ", "Αυγ", "Σεπ", "Οκτ", "Νοε", "Δεκ");
        data.setLabels(labels);

        if (manager != null) {
            Map<Integer, Map<String, Long>> trend = statsService.getMonthlyTrendByType(manager.getId(), LocalDate.now().getYear());
            for (LeaveType type : LeaveType.values()) {
                LineChartDataSet dataSet = new LineChartDataSet();
                dataSet.setLabel(type.getDisplayName());
                dataSet.setBorderColor(type.getColor());
                dataSet.setBackgroundColor(type.getColor());
                dataSet.setFill(false);

                List<Object> values = new ArrayList<>();
                for (int month = 1; month <= 12; month++) {
                    values.add(trend.getOrDefault(month, Map.of()).getOrDefault(type.getDisplayName(), 0L));
                }
                dataSet.setData(values);
                data.addChartDataSet(dataSet);
                trendLeaveTypes.add(type);
            }
        }

        trendModel.setData(data);
        LineChartOptions options = new LineChartOptions();
        options.setMaintainAspectRatio(false);

        Legend legend = new Legend();
        legend.setDisplay(false);
        options.setLegend(legend);

        trendModel.setOptions(options);
    }

    private void createTeamScheduleModel() {
        teamScheduleModel = new DefaultScheduleModel();
        Employee manager = loginController.getLoggedInUser();

        if (manager == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate from = LocalDate.of(today.getYear(), 1, 1);
        LocalDate to = LocalDate.of(today.getYear() + 1, 12, 31);
        List<LeaveRequest> approved = managerService.getApprovedRequestsForTeam(manager, from, to);

        for (LeaveRequest request : approved) {
            String title = request.getEmployee().getFullName() + " - " + request.getLeaveType();
            teamScheduleModel.addEvent(DefaultScheduleEvent.builder()
                    .title(title)
                    .startDate(request.getStartDate().atStartOfDay())
                    .endDate(request.getEndDate().plusDays(1).atStartOfDay())
                    .allDay(true)
                    .styleClass(getLeaveTypeStyleClass(request.getLeaveType()))
                    .build());
        }
    }

    private String getLeaveTypeStyleClass(String leaveType) {
        if ("Κανονική Άδεια".equals(leaveType)) {
            return "leave-type-annual";
        }
        if ("Αναρρωτική Άδεια".equals(leaveType)) {
            return "leave-type-sick";
        }
        if ("Άδεια άνευ Αποδοχών".equals(leaveType)) {
            return "leave-type-unpaid";
        }
        if ("Γονική Άδεια".equals(leaveType)) {
            return "leave-type-parental";
        }
        if ("Εκπαιδευτική Άδεια".equals(leaveType) || "Ειδική Άδεια".equals(leaveType)) {
            return "leave-type-study";
        }
        return "leave-type-annual";
    }

    public PieChartModel getPieModel() { return pieModel; }
    public BarChartModel getBarModel() { return barModel; }
    public LineChartModel getTrendModel() { return trendModel; }
    public ScheduleModel getTeamScheduleModel() { return teamScheduleModel; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public long getTotalRequests() { return totalRequests; }
    public long getPendingCount() { return pendingCount; }
    public long getApprovedCount() { return approvedCount; }
    public long getRejectedCount() { return rejectedCount; }
    public int getLeaveUtilizationPercent() { return leaveUtilizationPercent; }
    public long getLongestPendingRequestAge() { return longestPendingRequestAge; }
    public List<LeaveRequest> getOnLeaveThisWeek() { return onLeaveThisWeek; }
    public List<LeaveType> getActiveBarLeaveTypes() { return activeBarLeaveTypes; }
    public List<ChartLegendItem> getStatusLegendItems() { return statusLegendItems; }

    public List<LeaveType> getTrendLeaveTypes() { return trendLeaveTypes; }

    public static class ChartLegendItem implements Serializable {
        private String label;
        private String color;

        public ChartLegendItem(String label, String color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public String getColor() {
            return color;
        }
    }
}