package com.company.lms.service;

import com.company.lms.model.Employee;
import com.company.lms.model.LeaveBalance;
import com.company.lms.model.LeaveRequest;
import com.company.lms.model.LeaveStatus;
import com.company.lms.repository.EmployeeRepository;
import com.company.lms.repository.LeaveBalanceRepository;
import com.company.lms.repository.LeaveRepository;
import com.company.lms.util.GreekHolidayUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class EmployeeLeaveService {

    @Inject
    private EmailService emailService;

    @Inject
    private LeaveRepository leaveRepo;

    @Inject
    private EmployeeRepository employeeRepo;

    @Inject
    private LeaveBalanceRepository leaveBalanceRepo;

    public Employee getEmployee(Integer employeeId) {
        return employeeRepo.findById(employeeId);
    }

    public List<LeaveRequest> getLeaveHistory(Integer employeeId) {
        return leaveRepo.findByEmployeeId(employeeId);
    }

    @Transactional
    public void submitLeaveRequest(Integer employeeId, LocalDate startDate, LocalDate endDate, String leaveType, String reason, String attachmentFileName, String attachmentContentType, byte[] attachmentData) {
        Employee employee = employeeRepo.findById(employeeId);

        if (employee == null) {
            throw new IllegalArgumentException("Ο υπάλληλος δεν βρέθηκε.");
        }

        if (employee.getManager() == null) {
            throw new IllegalStateException(
                    "Δεν μπορείτε να υποβάλετε αίτηση άδειας, επειδή δεν έχει οριστεί προϊστάμενος για τον λογαριασμό σας."
            );
        }

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Παρακαλώ επιλέξτε ημερομηνία έναρξης και λήξης.");
        }

        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Η ημερομηνία έναρξης δεν μπορεί να είναι στο παρελθόν.");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Η ημερομηνία λήξης δεν μπορεί να προηγείται της ημερομηνίας έναρξης.");
        }

        if (leaveType == null || leaveType.isBlank()) {
            throw new IllegalArgumentException("Παρακαλώ επιλέξτε τύπο άδειας.");
        }

        int workingDays = calculateWorkingDays(startDate, endDate);

        if (workingDays <= 0) {
            throw new IllegalArgumentException("Το διάστημα άδειας πρέπει να περιλαμβάνει τουλάχιστον μία εργάσιμη ημέρα.");
        }

        int balance = leaveBalanceRepo.getBalance(employeeId, leaveType);
        if (balance < workingDays) {
            throw new IllegalStateException("Οι εργάσιμες ημέρες υπερβαίνουν το διαθέσιμο υπόλοιπο άδειας.");
        }

        if (leaveRepo.existsOverlappingRequest(employeeId, startDate, endDate)) {
            throw new IllegalStateException("Υπάρχει ήδη εκκρεμές ή εγκεκριμένο αίτημα για το επιλεγμένο διάστημα.");
        }

        LeaveRequest request = new LeaveRequest();
        request.setEmployee(employee);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setLeaveType(leaveType);
        request.setReason(reason);
        request.setStatus(LeaveStatus.PENDING);

        if (attachmentData != null && attachmentData.length > 0) {
            request.setAttachmentFileName(attachmentFileName);
            request.setAttachmentContentType(attachmentContentType);
            request.setAttachmentData(attachmentData);
        }

        leaveRepo.save(request);

        emailService.sendLeaveSubmittedEmailToManager(employee, request);
    }

    public List<LeaveBalance> getLeaveBalances(Integer employeeId) {
        return leaveBalanceRepo.findByEmployeeId(employeeId);
    }

    public int getTotalLeaveBalance(Integer employeeId) {
        return leaveBalanceRepo.sumTotalForEmployee(employeeId);
    }

    public int getBalanceForType(Integer employeeId, String leaveType) {
        return leaveBalanceRepo.getBalance(employeeId, leaveType);
    }

    public int calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
        return GreekHolidayUtil.calculateWorkingDays(startDate, endDate);
    }
}