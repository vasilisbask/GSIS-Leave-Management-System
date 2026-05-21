package com.company.lms.service;

import com.company.lms.model.AuditLog;
import com.company.lms.model.Employee;
import com.company.lms.model.LeaveBalance;
import com.company.lms.model.LeaveRequest;
import com.company.lms.model.LeaveStatus;
import com.company.lms.repository.EmployeeRepository;
import com.company.lms.repository.LeaveRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@ApplicationScoped
public class PdfExportService {

    @Inject
    private EmployeeRepository employeeRepo;

    @Inject
    private LeaveRepository leaveRepo;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Resolves and loads a font that supports Greek Unicode characters.
     */
    private Font getGreekFont(float size, int style, BaseColor color) {
        String[] fontPaths = {
            "C:\\Windows\\Fonts\\arial.ttf",
            "C:\\Windows\\Fonts\\arialbd.ttf",
            "C:\\Windows\\Fonts\\tahoma.ttf",
            "C:\\Windows\\Fonts\\tahomabd.ttf",
            "C:\\Windows\\Fonts\\dejavusans.ttf",
            "C:\\Windows\\Fonts\\cour.ttf"
        };

        for (String path : fontPaths) {
            java.io.File file = new java.io.File(path);
            if (file.exists()) {
                try {
                    BaseFont bf = BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                    return new Font(bf, size, style, color);
                } catch (Exception e) {
                    // Try next path
                }
            }
        }

        // Fallback to standard core font
        int itextStyle = Font.NORMAL;
        if (style == Font.BOLD) itextStyle = Font.BOLD;
        else if (style == Font.ITALIC) itextStyle = Font.ITALIC;
        return new Font(Font.FontFamily.HELVETICA, size, itextStyle, color);
    }

    /**
     * Generates an official Leave Certificate PDF for an approved leave request.
     */
    public byte[] generateLeaveCertificate(LeaveRequest request) {
        if (request == null) {
            return new byte[0];
        }

        Document document = new Document(PageSize.A4, 45, 45, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Colors
            BaseColor primaryColor = new BaseColor(1, 136, 202);
            BaseColor darkGray = new BaseColor(51, 51, 51);
            BaseColor lightGray = new BaseColor(240, 240, 240);
            BaseColor borderColor = new BaseColor(220, 220, 220);

            // Fonts
            Font headerDeptFont = getGreekFont(12, Font.BOLD, primaryColor);
            Font headerSubFont = getGreekFont(9, Font.NORMAL, darkGray);
            Font docTitleFont = getGreekFont(16, Font.BOLD, primaryColor);
            Font subTitleFont = getGreekFont(10, Font.BOLD, darkGray);
            Font bodyBoldFont = getGreekFont(10, Font.BOLD, darkGray);
            Font bodyNormalFont = getGreekFont(10, Font.NORMAL, darkGray);
            Font smallMutedFont = getGreekFont(8, Font.NORMAL, BaseColor.GRAY);

            // 1. GSIS Letterhead
            Paragraph deptPara = new Paragraph("ΓΕΝΙΚΗ ΓΡΑΜΜΑΤΕΙΑ ΠΛΗΡΟΦΟΡΙΑΚΩΝ ΣΥΣΤΗΜΑΤΩΝ (Γ.Γ.Π.Σ.)", headerDeptFont);
            deptPara.setAlignment(Element.ALIGN_CENTER);
            document.add(deptPara);

            Paragraph systemPara = new Paragraph("ΣΥΣΤΗΜΑ ΔΙΑΧΕΙΡΙΣΗΣ ΑΔΕΙΩΝ ΠΡΟΣΩΠΙΚΟΥ", headerSubFont);
            systemPara.setAlignment(Element.ALIGN_CENTER);
            systemPara.setSpacingAfter(15f);
            document.add(systemPara);

            // Divider Line
            Paragraph line = new Paragraph();
            line.add(new Chunk(new com.itextpdf.text.pdf.draw.LineSeparator(1.5f, 100, primaryColor, Element.ALIGN_CENTER, -2)));
            line.setSpacingAfter(25f);
            document.add(line);

            // Document Title
            Paragraph titlePara = new Paragraph("ΕΠΙΣΗΜΗ ΑΠΟΦΑΣΗ ΕΓΚΡΙΣΗΣ ΑΔΕΙΑΣ", docTitleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            document.add(titlePara);

            Paragraph idPara = new Paragraph("Κωδικός Απόφασης: DEC-00" + request.getId(), subTitleFont);
            idPara.setAlignment(Element.ALIGN_CENTER);
            idPara.setSpacingAfter(30f);
            document.add(idPara);

            // Official Certificate Body Text
            Paragraph introPara = new Paragraph();
            introPara.add(new Chunk("Βεβαιώνεται ότι, σύμφωνα με τις κείμενες διατάξεις και τους εσωτερικούς κανονισμούς λειτουργίας της Γενικής Γραμματείας Πληροφοριακών Συστημάτων, εγκρίθηκε το αίτημα άδειας του υπαλλήλου με τα ακόλουθα στοιχεία:", bodyNormalFont));
            introPara.setLeading(16f);
            introPara.setSpacingAfter(20f);
            document.add(introPara);

            // Employee Information Grid Table
            PdfPTable empTable = new PdfPTable(2);
            empTable.setWidthPercentage(100);
            empTable.setSpacingAfter(20f);
            empTable.setWidths(new float[]{30f, 70f});

            addGridCell(empTable, "Ονοματεπώνυμο:", bodyBoldFont, lightGray, true);
            addGridCell(empTable, request.getEmployee().getFullName(), bodyNormalFont, BaseColor.WHITE, false);

            addGridCell(empTable, "Ηλεκτρονικό Ταχυδρομείο (Email):", bodyBoldFont, lightGray, true);
            addGridCell(empTable, request.getEmployee().getEmail(), bodyNormalFont, BaseColor.WHITE, false);

            addGridCell(empTable, "Ρόλος / Θέση:", bodyBoldFont, lightGray, true);
            String roleName = request.getEmployee().getRole() != null ? request.getEmployee().getRole().getRoleName() : "EMPLOYEE";
            String roleDisplayName = "Υπάλληλος";
            if ("MANAGER".equals(roleName)) roleDisplayName = "Προϊστάμενος";
            else if ("SECRETARY".equals(roleName)) roleDisplayName = "Γραμματεία (Διαχειριστής)";
            addGridCell(empTable, roleDisplayName, bodyNormalFont, BaseColor.WHITE, false);

            addGridCell(empTable, "Εγκρίνων Προϊστάμενος:", bodyBoldFont, lightGray, true);
            String managerName = request.getEmployee().getManager() != null ? request.getEmployee().getManager().getFullName() : "Κεντρική Διοίκηση";
            addGridCell(empTable, managerName, bodyNormalFont, BaseColor.WHITE, false);

            document.add(empTable);

            // Leave Request Grid Table
            Paragraph leaveTitle = new Paragraph("ΣΤΟΙΧΕΙΑ ΧΟΡΗΓΗΘΕΙΣΑΣ ΑΔΕΙΑΣ", bodyBoldFont);
            leaveTitle.setSpacingAfter(8f);
            document.add(leaveTitle);

            PdfPTable leaveTable = new PdfPTable(2);
            leaveTable.setWidthPercentage(100);
            leaveTable.setSpacingAfter(25f);
            leaveTable.setWidths(new float[]{30f, 70f});

            addGridCell(leaveTable, "Τύπος Άδειας:", bodyBoldFont, lightGray, true);
            addGridCell(leaveTable, request.getLeaveType(), bodyNormalFont, BaseColor.WHITE, false);

            addGridCell(leaveTable, "Ημερομηνία Έναρξης:", bodyBoldFont, lightGray, true);
            addGridCell(leaveTable, request.getStartDate().format(DATE_FORMATTER), bodyNormalFont, BaseColor.WHITE, false);

            addGridCell(leaveTable, "Ημερομηνία Λήξης:", bodyBoldFont, lightGray, true);
            addGridCell(leaveTable, request.getEndDate().format(DATE_FORMATTER), bodyNormalFont, BaseColor.WHITE, false);

            addGridCell(leaveTable, "Συνολικές Ημέρες:", bodyBoldFont, lightGray, true);
            addGridCell(leaveTable, String.valueOf(request.getDurationDays()) + " Ημέρες (Ημερολογιακές)", bodyNormalFont, BaseColor.WHITE, false);

            addGridCell(leaveTable, "Εργάσιμες Ημέρες:", bodyBoldFont, lightGray, true);
            addGridCell(leaveTable, String.valueOf(request.getWorkingDays()) + " Ημέρες (Εξαιρούνται Σαββατοκύριακα & Αργίες)", bodyNormalFont, BaseColor.WHITE, false);

            addGridCell(leaveTable, "Αιτιολογία Υπαλλήλου:", bodyBoldFont, lightGray, true);
            String reasonText = request.getReason() != null && !request.getReason().isBlank() ? request.getReason() : "-";
            addGridCell(leaveTable, reasonText, bodyNormalFont, BaseColor.WHITE, false);

            addGridCell(leaveTable, "Σχόλιο Έγκρισης:", bodyBoldFont, lightGray, true);
            String commentText = request.getManagerComment() != null && !request.getManagerComment().isBlank() ? request.getManagerComment() : "-";
            addGridCell(leaveTable, commentText, bodyNormalFont, BaseColor.WHITE, false);

            addGridCell(leaveTable, "Κατάσταση Αιτήματος:", bodyBoldFont, lightGray, true);
            addGridCell(leaveTable, "ΕΓΚΡΙΘΗΚΕ", getGreekFont(10, Font.BOLD, new BaseColor(46, 125, 50)), BaseColor.WHITE, false);

            document.add(leaveTable);

            // Signatures and Timestamp
            PdfPTable footerTable = new PdfPTable(2);
            footerTable.setWidthPercentage(100);
            footerTable.setWidths(new float[]{50f, 50f});
            footerTable.setSpacingBefore(30f);

            // Generation details
            PdfPCell genCell = new PdfPCell();
            genCell.setBorder(Rectangle.NO_BORDER);
            Paragraph genPara = new Paragraph();
            genPara.add(new Chunk("Ημερομηνία Έκδοσης: " + java.time.LocalDateTime.now().format(TIMESTAMP_FORMATTER) + "\n", smallMutedFont));
            genPara.add(new Chunk("Το παρόν έγγραφο παράγεται ηλεκτρονικά και φέρει έγκυρη ψηφιακή σφραγίδα.", smallMutedFont));
            genCell.addElement(genPara);
            footerTable.addCell(genCell);

            // Approval Signature Block
            PdfPCell signCell = new PdfPCell();
            signCell.setBorder(Rectangle.NO_BORDER);
            signCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            Paragraph signPara = new Paragraph();
            signPara.setAlignment(Element.ALIGN_CENTER);
            signPara.add(new Chunk("Ο ΕΓΚΡΙΝΩΝ ΠΡΟΪΣΤΑΜΕΝΟΣ\n\n\n\n", bodyBoldFont));
            signPara.add(new Chunk("___________________________\n", bodyBoldFont));
            signPara.add(new Chunk(managerName, bodyBoldFont));
            signCell.addElement(signPara);
            footerTable.addCell(signCell);

            document.add(footerTable);

            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    /**
     * Generates a detailed Leave History statement PDF for an employee.
     */
    public byte[] generateLeaveHistoryReport(Employee employee, List<LeaveRequest> history, List<LeaveBalance> balances) {
        if (employee == null) {
            return new byte[0];
        }

        Document document = new Document(PageSize.A4, 36, 36, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Colors
            BaseColor primaryColor = new BaseColor(1, 136, 202);
            BaseColor darkGray = new BaseColor(51, 51, 51);
            BaseColor lightGray = new BaseColor(245, 245, 245);
            BaseColor textMuted = new BaseColor(100, 100, 100);

            // Fonts
            Font headerDeptFont = getGreekFont(11, Font.BOLD, primaryColor);
            Font headerSubFont = getGreekFont(8, Font.NORMAL, darkGray);
            Font docTitleFont = getGreekFont(14, Font.BOLD, primaryColor);
            Font tableHeaderFont = getGreekFont(9, Font.BOLD, BaseColor.WHITE);
            Font bodyBoldFont = getGreekFont(9, Font.BOLD, darkGray);
            Font bodyNormalFont = getGreekFont(9, Font.NORMAL, darkGray);
            Font smallFont = getGreekFont(8, Font.NORMAL, darkGray);
            Font smallMutedFont = getGreekFont(8, Font.NORMAL, textMuted);

            // GSIS Letterhead
            Paragraph deptPara = new Paragraph("ΓΕΝΙΚΗ ΓΡΑΜΜΑΤΕΙΑ ΠΛΗΡΟΦΟΡΙΑΚΩΝ ΣΥΣΤΗΜΑΤΩΝ (Γ.Γ.Π.Σ.)", headerDeptFont);
            deptPara.setAlignment(Element.ALIGN_CENTER);
            document.add(deptPara);

            Paragraph systemPara = new Paragraph("ΣΥΣΤΗΜΑ ΔΙΑΧΕΙΡΙΣΗΣ ΑΔΕΙΩΝ ΠΡΟΣΩΠΙΚΟΥ", headerSubFont);
            systemPara.setAlignment(Element.ALIGN_CENTER);
            systemPara.setSpacingAfter(10f);
            document.add(systemPara);

            // Divider Line
            Paragraph line = new Paragraph();
            line.add(new Chunk(new com.itextpdf.text.pdf.draw.LineSeparator(1.2f, 100, primaryColor, Element.ALIGN_CENTER, -2)));
            line.setSpacingAfter(15f);
            document.add(line);

            // Document Title
            Paragraph titlePara = new Paragraph("ΥΠΗΡΕΣΙΑΚΗ ΚΑΤΑΣΤΑΣΗ ΑΔΕΙΩΝ ΥΠΑΛΛΗΛΟΥ", docTitleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(20f);
            document.add(titlePara);

            // Employee Info Metadata Box
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setSpacingAfter(15f);
            infoTable.setWidths(new float[]{50f, 50f});

            addMetadataCell(infoTable, "Υπάλληλος:", employee.getFullName(), bodyBoldFont, bodyNormalFont);
            addMetadataCell(infoTable, "Email:", employee.getEmail(), bodyBoldFont, bodyNormalFont);
            
            String managerName = employee.getManager() != null ? employee.getManager().getFullName() : "Κεντρική Διοίκηση";
            addMetadataCell(infoTable, "Προϊστάμενος:", managerName, bodyBoldFont, bodyNormalFont);
            
            String dateGenerated = java.time.LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            addMetadataCell(infoTable, "Ημερομηνία Έκδοσης:", dateGenerated, bodyBoldFont, bodyNormalFont);

            document.add(infoTable);

            // Balances Overview Block
            Paragraph balanceTitle = new Paragraph("1. ΥΠΟΛΟΙΠΟ ΑΔΕΙΩΝ (ΗΜΕΡΕΣ)", bodyBoldFont);
            balanceTitle.setSpacingAfter(6f);
            document.add(balanceTitle);

            PdfPTable balTable = new PdfPTable(3);
            balTable.setWidthPercentage(100);
            balTable.setSpacingAfter(20f);
            balTable.setWidths(new float[]{40f, 30f, 30f});

            // Table headers
            addTableHeaderCell(balTable, "ΤΥΠΟΣ ΑΔΕΙΑΣ", tableHeaderFont, primaryColor);
            addTableHeaderCell(balTable, "ΔΙΚΑΙΟΥΜΕΝΕΣ ΗΜΕΡΕΣ", tableHeaderFont, primaryColor);
            addTableHeaderCell(balTable, "ΥΠΟΛΟΙΠΟ", tableHeaderFont, primaryColor);

            int totalRemaining = 0;
            if (balances != null && !balances.isEmpty()) {
                for (LeaveBalance bal : balances) {
                    addTableCell(balTable, bal.getLeaveType(), bodyNormalFont, BaseColor.WHITE, Element.ALIGN_LEFT);
                    
                    int initialAllowance = bal.getLeaveType().equalsIgnoreCase("Ετήσια") ? 25 :
                                           bal.getLeaveType().equalsIgnoreCase("Αναρρωτική") ? 10 : 5;
                    
                    addTableCell(balTable, String.valueOf(initialAllowance), bodyNormalFont, BaseColor.WHITE, Element.ALIGN_CENTER);
                    addTableCell(balTable, String.valueOf(bal.getBalance()) + " Ημέρες", bodyBoldFont, BaseColor.WHITE, Element.ALIGN_CENTER);
                    totalRemaining += bal.getBalance();
                }
            } else {
                PdfPCell emptyBalCell = new PdfPCell(new Phrase("Δεν υπάρχουν διαθέσιμα υπόλοιπα αδειών.", bodyNormalFont));
                emptyBalCell.setColspan(3);
                emptyBalCell.setPadding(8f);
                emptyBalCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                balTable.addCell(emptyBalCell);
            }
            
            // Total row
            addTableCell(balTable, "ΣΥΝΟΛΙΚΟ ΥΠΟΛΟΙΠΟ", bodyBoldFont, lightGray, Element.ALIGN_LEFT);
            addTableCell(balTable, "-", bodyNormalFont, lightGray, Element.ALIGN_CENTER);
            addTableCell(balTable, String.valueOf(totalRemaining) + " Ημέρες", bodyBoldFont, lightGray, Element.ALIGN_CENTER);

            document.add(balTable);

            // History Table
            Paragraph historyTitle = new Paragraph("2. ΑΝΑΛΥΤΙΚΟ ΙΣΤΟΡΙΚΟ ΑΙΤΗΜΑΤΩΝ", bodyBoldFont);
            historyTitle.setSpacingAfter(6f);
            document.add(historyTitle);

            PdfPTable histTable = new PdfPTable(6);
            histTable.setWidthPercentage(100);
            histTable.setWidths(new float[]{20f, 15f, 15f, 10f, 20f, 20f});

            addTableHeaderCell(histTable, "ΤΥΠΟΣ", tableHeaderFont, primaryColor);
            addTableHeaderCell(histTable, "ΑΠΟ", tableHeaderFont, primaryColor);
            addTableHeaderCell(histTable, "ΕΩΣ", tableHeaderFont, primaryColor);
            addTableHeaderCell(histTable, "ΗΜΕΡΕΣ", tableHeaderFont, primaryColor);
            addTableHeaderCell(histTable, "ΚΑΤΑΣΤΑΣΗ", tableHeaderFont, primaryColor);
            addTableHeaderCell(histTable, "ΣΧΟΛΙΟ/ΑΙΤΙΟΛΟΓΙΑ", tableHeaderFont, primaryColor);

            if (history != null && !history.isEmpty()) {
                for (LeaveRequest req : history) {
                    addTableCell(histTable, req.getLeaveType(), bodyNormalFont, BaseColor.WHITE, Element.ALIGN_LEFT);
                    addTableCell(histTable, req.getStartDate().format(DATE_FORMATTER), smallFont, BaseColor.WHITE, Element.ALIGN_CENTER);
                    addTableCell(histTable, req.getEndDate().format(DATE_FORMATTER), smallFont, BaseColor.WHITE, Element.ALIGN_CENTER);
                    addTableCell(histTable, String.valueOf(req.getWorkingDays()), bodyBoldFont, BaseColor.WHITE, Element.ALIGN_CENTER);

                    // Status
                    String statusText = req.getStatus() != null ? req.getStatus().getDisplayName() : "Εκκρεμεί";
                    Font statusFont = bodyNormalFont;
                    if (req.getStatus() == LeaveStatus.APPROVED) {
                        statusFont = getGreekFont(9, Font.BOLD, new BaseColor(46, 125, 50)); // Green
                    } else if (req.getStatus() == LeaveStatus.REJECTED) {
                        statusFont = getGreekFont(9, Font.BOLD, new BaseColor(198, 40, 40)); // Red
                    } else {
                        statusFont = getGreekFont(9, Font.BOLD, new BaseColor(230, 81, 0)); // Orange
                    }
                    addTableCell(histTable, statusText, statusFont, BaseColor.WHITE, Element.ALIGN_CENTER);

                    // Reason
                    String details = "";
                    if (req.getReason() != null && !req.getReason().isBlank()) {
                        details += "Αιτ: " + req.getReason();
                    }
                    if (req.getManagerComment() != null && !req.getManagerComment().isBlank()) {
                        if (!details.isEmpty()) details += " | ";
                        details += "Σχόλ: " + req.getManagerComment();
                    }
                    if (details.isEmpty()) details = "-";
                    
                    addTableCell(histTable, details, smallMutedFont, BaseColor.WHITE, Element.ALIGN_LEFT);
                }
            } else {
                PdfPCell emptyHistCell = new PdfPCell(new Phrase("Δεν βρέθηκαν αιτήματα άδειας.", bodyNormalFont));
                emptyHistCell.setColspan(6);
                emptyHistCell.setPadding(8f);
                emptyHistCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                histTable.addCell(emptyHistCell);
            }

            document.add(histTable);

            // Footer disclaimer
            Paragraph disclaimer = new Paragraph();
            disclaimer.setSpacingBefore(30f);
            disclaimer.setAlignment(Element.ALIGN_CENTER);
            disclaimer.add(new Chunk("Το παρόν αποτελεί επίσημο αντίγραφο υπηρεσιακού φακέλου από το Σύστημα LMS της Γ.Γ.Π.Σ.\nΗλεκτρονική Έκδοση - Έχει ισχύ πρωτοτύπου.", smallMutedFont));
            document.add(disclaimer);

            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    /**
     * Generates a PDF of the Manager Audit Log.
     */
    public byte[] generateAuditLogReport(List<AuditLog> logs) {
        Document document = new Document(PageSize.A4.rotate(), 30, 30, 40, 40); // Landscape orientation for wide table
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            BaseColor primaryColor = new BaseColor(1, 136, 202); // GSIS Blue
            BaseColor darkGray = new BaseColor(51, 51, 51);
            BaseColor lightGray = new BaseColor(245, 245, 245);

            Font headerDeptFont = getGreekFont(12, Font.BOLD, primaryColor);
            Font headerSubFont = getGreekFont(9, Font.NORMAL, darkGray);
            Font docTitleFont = getGreekFont(15, Font.BOLD, primaryColor);
            Font tableHeaderFont = getGreekFont(9, Font.BOLD, BaseColor.WHITE);
            Font bodyNormalFont = getGreekFont(9, Font.NORMAL, darkGray);
            Font smallFont = getGreekFont(8, Font.NORMAL, darkGray);

            // GSIS Letterhead
            Paragraph deptPara = new Paragraph("ΓΕΝΙΚΗ ΓΡΑΜΜΑΤΕΙΑ ΠΛΗΡΟΦΟΡΙΑΚΩΝ ΣΥΣΤΗΜΑΤΩΝ (Γ.Γ.Π.Σ.)", headerDeptFont);
            deptPara.setAlignment(Element.ALIGN_CENTER);
            document.add(deptPara);

            Paragraph systemPara = new Paragraph("ΣΥΣΤΗΜΑ ΔΙΑΧΕΙΡΙΣΗΣ ΑΔΕΙΩΝ ΠΡΟΣΩΠΙΚΟΥ", headerSubFont);
            systemPara.setAlignment(Element.ALIGN_CENTER);
            systemPara.setSpacingAfter(10f);
            document.add(systemPara);

            // Divider Line
            Paragraph line = new Paragraph();
            line.add(new Chunk(new com.itextpdf.text.pdf.draw.LineSeparator(1.2f, 100, primaryColor, Element.ALIGN_CENTER, -2)));
            line.setSpacingAfter(15f);
            document.add(line);

            // Document Title
            Paragraph titlePara = new Paragraph("ΑΡΧΕΙΟ ΚΑΤΑΓΡΑΦΗΣ ΕΝΕΡΓΕΙΩΝ & ΑΣΦΑΛΕΙΑΣ (AUDIT LOG)", docTitleFont);
            titlePara.setAlignment(Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(20f);
            document.add(titlePara);

            // Info panel
            Paragraph infoPara = new Paragraph("Συνολικά Καταγεγραμμένα Συμβάντα: " + (logs != null ? logs.size() : 0) + " | Ημερομηνία Εξαγωγής: " + java.time.LocalDateTime.now().format(TIMESTAMP_FORMATTER), smallFont);
            infoPara.setSpacingAfter(10f);
            document.add(infoPara);

            // Audit Table
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{18f, 20f, 20f, 12f, 10f, 20f});

            addTableHeaderCell(table, "ΗΜΕΡΟΜΗΝΙΑ & ΩΡΑ", tableHeaderFont, primaryColor);
            addTableHeaderCell(table, "ΧΡΗΣΤΗΣ (ΕΝΕΡΓΩΝ)", tableHeaderFont, primaryColor);
            addTableHeaderCell(table, "ΥΠΑΛΛΗΛΟΣ (ΣΤΟΧΟΣ)", tableHeaderFont, primaryColor);
            addTableHeaderCell(table, "ΕΝΕΡΓΕΙΑ", tableHeaderFont, primaryColor);
            addTableHeaderCell(table, "ΚΩΔΙΚΟΣ", tableHeaderFont, primaryColor);
            addTableHeaderCell(table, "ΣΧΟΛΙΟ / ΠΕΡΙΓΡΑΦΗ", tableHeaderFont, primaryColor);

            if (logs != null && !logs.isEmpty()) {
                for (AuditLog log : logs) {
                    addTableCell(table, log.getTimestamp() != null ? log.getTimestamp().format(TIMESTAMP_FORMATTER) : "-", smallFont, BaseColor.WHITE, Element.ALIGN_CENTER);
                    
                    // User
                    String userStr = "-";
                    if (log.getUser() != null) {
                        String fullName = log.getUser().getFullName() != null ? log.getUser().getFullName() : "";
                        String email = log.getUser().getEmail() != null ? log.getUser().getEmail() : "";
                        if (!fullName.isBlank() && !email.isBlank()) {
                            userStr = fullName + " - " + email;
                        } else if (!fullName.isBlank()) {
                            userStr = fullName;
                        } else if (!email.isBlank()) {
                            userStr = email;
                        }
                    }
                    addTableCell(table, userStr, smallFont, BaseColor.WHITE, Element.ALIGN_LEFT);

                    // Target Employee
                    String targetStr = "-";
                    try {
                        Employee targetEmp = getTargetEmployee(log);
                        if (targetEmp != null) {
                            String fullName = targetEmp.getFullName() != null ? targetEmp.getFullName() : "";
                            String email = targetEmp.getEmail() != null ? targetEmp.getEmail() : "";
                            if (!fullName.isBlank() && !email.isBlank()) {
                                targetStr = fullName + " - " + email;
                            } else if (!fullName.isBlank()) {
                                targetStr = fullName;
                            } else if (!email.isBlank()) {
                                targetStr = email;
                            }
                        }
                    } catch (Exception e) {
                        targetStr = "-";
                    }
                    addTableCell(table, targetStr, smallFont, BaseColor.WHITE, Element.ALIGN_LEFT);

                    // Action
                    String actionText = log.getAction();
                    if ("APPROVE".equalsIgnoreCase(actionText)) actionText = "Έγκριση";
                    else if ("REJECT".equalsIgnoreCase(actionText)) actionText = "Απόρριψη";
                    else if ("UPDATE_ROLE".equalsIgnoreCase(actionText)) actionText = "Αλλαγή Ρόλου";
                    else if ("UPDATE_BALANCE".equalsIgnoreCase(actionText)) actionText = "Αλλαγή Υπολοίπου";
                    else if ("UPDATE_MANAGER".equalsIgnoreCase(actionText)) actionText = "Αλλαγή Προϊστ.";
                    addTableCell(table, actionText, smallFont, BaseColor.WHITE, Element.ALIGN_CENTER);

                    // Target ID
                    String targetIdStr = "-";
                    if (log.getTargetId() != null) {
                        String act = log.getAction() != null ? log.getAction().toUpperCase() : "";
                        if (act.contains("ROLE") || act.contains("BALANCE") || act.contains("MANAGER")) {
                            targetIdStr = "EMP-" + log.getTargetId();
                        } else {
                            targetIdStr = "LVR-" + log.getTargetId();
                        }
                    }
                    addTableCell(table, targetIdStr, smallFont, BaseColor.WHITE, Element.ALIGN_CENTER);

                    // Comment
                    addTableCell(table, log.getComment() != null ? log.getComment() : "-", smallFont, BaseColor.WHITE, Element.ALIGN_LEFT);
                }
            } else {
                PdfPCell emptyCell = new PdfPCell(new Phrase("Δεν βρέθηκαν καταγραφές ελέγχου.", bodyNormalFont));
                emptyCell.setColspan(6);
                emptyCell.setPadding(8f);
                emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(emptyCell);
            }

            document.add(table);

            document.close();

        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    // Helper functions for formatting PDF tables

    private void addGridCell(PdfPTable table, String text, Font font, BaseColor bgColor, boolean isLabel) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(7f);
        cell.setPaddingLeft(10f);
        cell.setBorderColor(new BaseColor(220, 220, 220));
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        if (isLabel) {
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        } else {
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        }
        table.addCell(cell);
    }

    private void addMetadataCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3f);
        
        Paragraph para = new Paragraph();
        para.add(new Chunk(label + " ", labelFont));
        para.add(new Chunk(value, valueFont));
        
        cell.addElement(para);
        table.addCell(cell);
    }

    private void addTableHeaderCell(PdfPTable table, String text, Font font, BaseColor bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(new BaseColor(220, 220, 220));
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font, BaseColor bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(6f);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(new BaseColor(230, 230, 230));
        table.addCell(cell);
    }

    private Employee getTargetEmployee(AuditLog log) {
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
}
