package com.company.lms.controller;

import com.company.lms.model.AuditLog;
import com.company.lms.service.AuditService;
import com.company.lms.repository.LeaveRepository;
import com.company.lms.model.LeaveRequest;
import com.company.lms.model.Employee;
import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import java.time.format.DateTimeFormatter;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import com.company.lms.service.PdfExportService;

@Named
@ViewScoped
public class AuditController implements Serializable {

    @Inject
    private LeaveRepository leaveRepo;

    @Inject
    private com.company.lms.repository.EmployeeRepository employeeRepo;

    @Inject
    private AuditService auditService;

    @Inject
    private PdfExportService pdfService;

    private List<AuditLog> auditLogs;

    private static final DateTimeFormatter EXPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @PostConstruct
    public void init() {
        auditLogs = auditService.getAllLogs();
    }

    // Getter
    public List<AuditLog> getAuditLogs() { return auditLogs; }

    public String exportTimestamp(AuditLog log) {
        if (log == null || log.getTimestamp() == null) {
            return "";
        }

        return log.getTimestamp().format(EXPORT_DATE_FORMATTER);
    }

    public String exportUser(AuditLog log) {
        if (log == null || log.getUser() == null) {
            return "";
        }

        String firstName = log.getUser().getFirstName() != null
                ? log.getUser().getFirstName()
                : "";

        String lastName = log.getUser().getLastName() != null
                ? log.getUser().getLastName()
                : "";

        String email = log.getUser().getEmail() != null
                ? log.getUser().getEmail()
                : "";

        String fullName = (firstName + " " + lastName).trim();

        if (email.isBlank()) {
            return fullName;
        }

        if (fullName.isBlank()) {
            return email;
        }

        return fullName + " - " + email;
    }

    public String exportAction(AuditLog log) {
        if (log == null || log.getAction() == null) {
            return "";
        }

        return getActionDisplayName(log.getAction());
    }

    public String exportTargetId(AuditLog log) {
        if (log == null || log.getTargetId() == null || log.getAction() == null) {
            return "";
        }

        String action = log.getAction().toUpperCase();
        if (action.contains("ROLE") || action.contains("BALANCE") || action.contains("MANAGER")) {
            return "EMP-" + log.getTargetId();
        } else {
            return "LVR-" + log.getTargetId();
        }
    }

    public String exportComment(AuditLog log) {
        if (log == null || log.getComment() == null || log.getComment().isBlank()) {
            return "-";
        }

        return log.getComment();
    }

    public void exportHtmlExcel() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();

        String fileName = "audit_logs_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                ".xls";

        externalContext.responseReset();
        externalContext.setResponseContentType("application/vnd.ms-excel; charset=UTF-8");
        externalContext.setResponseHeader(
                "Content-Disposition",
                "attachment; filename=\"" + fileName + "\""
        );

        try (OutputStream outputStream = externalContext.getResponseOutputStream()) {

            StringBuilder html = new StringBuilder();

            html.append("<!DOCTYPE html>");
            html.append("<html>");
            html.append("<head>");
            html.append("<meta charset=\"UTF-8\" />");
            html.append("<style>");
            html.append("table { border-collapse: collapse; width: 100%; }");
            html.append("th { background-color: #0188ca; color: white; font-weight: bold; }");
            html.append("th, td { border: 1px solid #999; padding: 6px; font-family: Arial, sans-serif; font-size: 12px; }");
            html.append("</style>");
            html.append("</head>");
            html.append("<body>");

            html.append("<table>");
            html.append("<thead>");
            html.append("<tr>");
            html.append("<th>ΗΜΕΡΟΜΗΝΙΑ &amp; ΩΡΑ</th>");
            html.append("<th>ΠΡΟΪΣΤΑΜΕΝΟΣ</th>");
            html.append("<th>ΥΠΑΛΛΗΛΟΣ</th>");
            html.append("<th>ΕΝΕΡΓΕΙΑ</th>");
            html.append("<th>ΑΝΑΓΝΩΡΙΣΤΙΚΟ</th>");
            html.append("<th>ΣΧΟΛΙΟ</th>");
            html.append("</tr>");
            html.append("</thead>");

            html.append("<tbody>");

            if (auditLogs != null) {
                for (AuditLog log : auditLogs) {
                    html.append("<tr>");
                    html.append("<td>").append(escapeHtml(exportTimestamp(log))).append("</td>");
                    html.append("<td>").append(escapeHtml(exportUser(log))).append("</td>");
                    html.append("<td>").append(escapeHtml(exportTargetEmployee(log))).append("</td>");
                    html.append("<td>").append(escapeHtml(exportAction(log))).append("</td>");
                    html.append("<td>").append(escapeHtml(exportTargetId(log))).append("</td>");
                    html.append("<td>").append(escapeHtml(exportComment(log))).append("</td>");
                    html.append("</tr>");
                }
            }

            html.append("</tbody>");
            html.append("</table>");

            html.append("</body>");
            html.append("</html>");

            outputStream.write(html.toString().getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            facesContext.responseComplete();

        } catch (IOException e) {
            throw new RuntimeException("Σφάλμα κατά την εξαγωγή Excel.", e);
        }
    }

    public void exportPdf() {
        byte[] pdfBytes = pdfService.generateAuditLogReport(auditLogs);
        
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();

        String fileName = "audit_logs_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) +
                ".pdf";

        externalContext.responseReset();
        externalContext.setResponseContentType("application/pdf");
        externalContext.setResponseHeader(
                "Content-Disposition",
                "attachment; filename=\"" + fileName + "\""
        );
        externalContext.setResponseContentLength(pdfBytes.length);

        try (OutputStream outputStream = externalContext.getResponseOutputStream()) {
            outputStream.write(pdfBytes);
            outputStream.flush();
            facesContext.responseComplete();
        } catch (IOException e) {
            throw new RuntimeException("Σφάλμα κατά την εξαγωγή PDF.", e);
        }
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public Employee getTargetEmployee(AuditLog log) {
        if (log == null || log.getTargetId() == null || log.getAction() == null) {
            return null;
        }

        String action = log.getAction().toUpperCase();
        if (action.contains("ROLE") || action.contains("BALANCE") || action.contains("MANAGER")) {
            return employeeRepo.findById(log.getTargetId());
        } else {
            LeaveRequest request = leaveRepo.findByIdWithEmployee(log.getTargetId());
            if (request == null) {
                return null;
            }
            return request.getEmployee();
        }
    }

    public String exportTargetEmployee(AuditLog log) {
        Employee employee = getTargetEmployee(log);

        if (employee == null) {
            return "-";
        }

        String fullName = employee.getFullName() != null
                ? employee.getFullName()
                : "";

        String email = employee.getEmail() != null
                ? employee.getEmail()
                : "";

        if (email.isBlank()) {
            return fullName;
        }

        if (fullName.isBlank()) {
            return email;
        }

        return fullName + " - " + email;
    }

    public String getActionDisplayName(String action) {
        if (action == null || action.isBlank()) {
            return "";
        }

        return switch (action) {
            case "APPROVE" -> "Εγκρίθηκε";
            case "REJECT" -> "Απορρίφθηκε";
            default -> action;
        };
    }
}