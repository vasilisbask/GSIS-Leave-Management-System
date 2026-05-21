package com.company.lms.controller;

import com.company.lms.model.Employee;
import com.company.lms.model.LeaveBalance;
import com.company.lms.model.LeaveRequest;
import com.company.lms.model.LeaveStatus;
import com.company.lms.model.LeaveType;
import com.company.lms.service.EmployeeLeaveService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.file.UploadedFile;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

import com.company.lms.service.PdfExportService;

@Named
@ViewScoped
public class EmployeeController implements Serializable {

    @Inject
    private EmployeeLeaveService employeeService;

    @Inject
    private LoginController loginController;

    @Inject
    private PdfExportService pdfService;

    private Employee employee;
    private List<LeaveRequest> leaveHistory;
    private List<LeaveBalance> leaveBalances;
    private LocalDate startDate;
    private LocalDate endDate;
    private String leaveType;
    private String reason;
    private LeaveRequest selectedRequest;
    private UploadedFile uploadedFile;
    private String historyStatusFilter = "";
    private String historyLeaveTypeFilter = "";

    @PostConstruct
    public void init() {
        loadEmployeeData();
    }

    private void loadEmployeeData() {
        Employee loggedInUser = loginController.getLoggedInUser();

        if (loggedInUser == null) {
            return;
        }

        employee = employeeService.getEmployee(loggedInUser.getId());
        leaveHistory = employeeService.getLeaveHistory(loggedInUser.getId());
        leaveBalances = employeeService.getLeaveBalances(loggedInUser.getId());
    }

    public void submitLeaveRequest() {
        try {
            Employee loggedInUser = loginController.getLoggedInUser();

            if (loggedInUser == null) {
                throw new IllegalStateException("Απαιτείται σύνδεση χρήστη.");
            }

            String attachmentFileName = null;
            String attachmentContentType = null;
            byte[] attachmentData = null;

            if (uploadedFile != null && uploadedFile.getSize() > 0) {
                String fileName = uploadedFile.getFileName();
                String contentType = uploadedFile.getContentType();

                boolean isPdfByName = fileName != null && fileName.toLowerCase().endsWith(".pdf");
                boolean isPdfByContentType = "application/pdf".equalsIgnoreCase(contentType);

                if (!isPdfByName && !isPdfByContentType) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(
                                    FacesMessage.SEVERITY_ERROR,
                                    "Σφάλμα",
                                    "Επιτρέπονται μόνο αρχεία PDF."
                            ));
                    return;
                }

                attachmentFileName = fileName;
                attachmentContentType = contentType != null ? contentType : "application/pdf";
                attachmentData = uploadedFile.getContent();
            }

            employeeService.submitLeaveRequest(loggedInUser.getId(), startDate, endDate, leaveType, reason, attachmentFileName, attachmentContentType, attachmentData);

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Επιτυχία", "Το αίτημα άδειας υποβλήθηκε."));

            startDate = null;
            endDate = null;
            leaveType = null;
            reason = null;
            uploadedFile = null;
            loadEmployeeData();

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Σφάλμα", e.getMessage()));
        }
    }

    public int getTotalLeaveBalance() {
        if (leaveBalances == null) return 0;
        return leaveBalances.stream().mapToInt(LeaveBalance::getBalance).sum();
    }

    public int getSelectedTypeBalance() {
        if (leaveType == null || leaveBalances == null) return 0;
        return leaveBalances.stream()
                .filter(lb -> leaveType.equals(lb.getLeaveType()))
                .mapToInt(LeaveBalance::getBalance)
                .findFirst()
                .orElse(0);
    }

    public int getRequestedWorkingDays() {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return 0;
        }

        return employeeService.calculateWorkingDays(startDate, endDate);
    }

    public long getPendingCount() {
        return countByStatus(LeaveStatus.PENDING);
    }

    public long getApprovedCount() {
        return countByStatus(LeaveStatus.APPROVED);
    }

    public long getRejectedCount() {
        return countByStatus(LeaveStatus.REJECTED);
    }

    private long countByStatus(LeaveStatus status) {
        return leaveHistory == null ? 0 : leaveHistory.stream()
                .filter(request -> request.getStatus() == status)
                .count();
    }

    public StreamedContent downloadAttachment(LeaveRequest request) {
        if (request == null || request.getAttachmentData() == null || request.getAttachmentData().length == 0) {
            return null;
        }

        return DefaultStreamedContent.builder()
                .name(request.getAttachmentFileName())
                .contentType(request.getAttachmentContentType())
                .stream(() -> new ByteArrayInputStream(request.getAttachmentData()))
                .build();
    }

    public List<LeaveRequest> getFilteredLeaveHistory() {
        if (leaveHistory == null) {
            return new ArrayList<>();
        }

        return leaveHistory.stream()
                .filter(req -> {
                    boolean matchesStatus = true;

                    if (historyStatusFilter != null && !historyStatusFilter.isBlank()) {
                        matchesStatus = req.getStatus() != null
                                && req.getStatus().name().equals(historyStatusFilter);
                    }

                    boolean matchesLeaveType = true;

                    if (historyLeaveTypeFilter != null && !historyLeaveTypeFilter.isBlank()) {
                        matchesLeaveType = req.getLeaveType() != null
                                && req.getLeaveType().equals(historyLeaveTypeFilter);
                    }

                    return matchesStatus && matchesLeaveType;
                })
                .toList();
    }

    public void downloadLeaveCertificate(LeaveRequest request) {
        if (request == null) return;
        byte[] pdfBytes = pdfService.generateLeaveCertificate(request);
        downloadPdfFile(pdfBytes, "Leave_Certificate_" + request.getId() + ".pdf");
    }

    public void downloadLeaveHistoryPdf() {
        if (employee == null) return;
        byte[] pdfBytes = pdfService.generateLeaveHistoryReport(employee, leaveHistory, leaveBalances);
        downloadPdfFile(pdfBytes, "Leave_History_" + employee.getLastName() + ".pdf");
    }

    private void downloadPdfFile(byte[] pdfBytes, String filename) {
        if (pdfBytes == null || pdfBytes.length == 0) return;
        FacesContext facesContext = FacesContext.getCurrentInstance();
        jakarta.faces.context.ExternalContext externalContext = facesContext.getExternalContext();

        externalContext.responseReset();
        externalContext.setResponseContentType("application/pdf");
        externalContext.setResponseHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        externalContext.setResponseContentLength(pdfBytes.length);

        try (java.io.OutputStream outputStream = externalContext.getResponseOutputStream()) {
            outputStream.write(pdfBytes);
            outputStream.flush();
            facesContext.responseComplete();
        } catch (IOException e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Σφάλμα", "Αδυναμία λήψης του αρχείου PDF: " + e.getMessage()));
        }
    }

    public LocalDate getToday() { return LocalDate.now(); }

    public Employee getEmployee() { return employee; }
    public List<LeaveRequest> getLeaveHistory() { return leaveHistory; }
    public List<LeaveBalance> getLeaveBalances() { return leaveBalances; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }
    public LeaveType[] getLeaveTypes() { return LeaveType.values(); }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LeaveRequest getSelectedRequest() { return selectedRequest; }
    public void setSelectedRequest(LeaveRequest selectedRequest) { this.selectedRequest = selectedRequest; }
    public UploadedFile getUploadedFile() { return uploadedFile; }
    public void setUploadedFile(UploadedFile uploadedFile) { this.uploadedFile = uploadedFile; }
    public String getHistoryStatusFilter() { return historyStatusFilter; }
    public void setHistoryStatusFilter(String historyStatusFilter) { this.historyStatusFilter = historyStatusFilter; }
    public String getHistoryLeaveTypeFilter() { return historyLeaveTypeFilter; }
    public void setHistoryLeaveTypeFilter(String historyLeaveTypeFilter) { this.historyLeaveTypeFilter = historyLeaveTypeFilter; }
}