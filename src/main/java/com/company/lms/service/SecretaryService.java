package com.company.lms.service;

import com.company.lms.model.Employee;
import com.company.lms.model.LeaveBalance;
import com.company.lms.model.Role;
import com.company.lms.repository.EmployeeRepository;
import com.company.lms.repository.LeaveBalanceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class SecretaryService {

    @Inject
    private EmployeeRepository employeeRepo;

    @Inject
    private LeaveBalanceRepository leaveBalanceRepo;

    @Inject
    private AuditService auditService;

    @PersistenceContext(unitName = "lmsPU")
    private EntityManager em;

    private void validateSecretary(Employee loggedInUser) {
        if (loggedInUser == null) {
            throw new IllegalStateException("Απαιτείται σύνδεση χρήστη.");
        }
        if (loggedInUser.getRole() == null || !"SECRETARY".equals(loggedInUser.getRole().getRoleName())) {
            throw new SecurityException("Δεν έχετε δικαίωμα πρόσβασης σε αυτή τη λειτουργία.");
        }
    }

    public List<Employee> getPaginatedEmployees(Employee loggedInUser, int first, int pageSize, String search, String roleFilter) {
        validateSecretary(loggedInUser);
        return employeeRepo.findEmployeesPaginated(first, pageSize, search, roleFilter);
    }

    public long countEmployees(Employee loggedInUser, String search, String roleFilter) {
        validateSecretary(loggedInUser);
        return employeeRepo.countEmployees(search, roleFilter);
    }

    public List<Role> getAllRoles(Employee loggedInUser) {
        validateSecretary(loggedInUser);
        return employeeRepo.findAllRoles();
    }

    @Transactional
    public void updateUserRole(Employee loggedInUser, Integer targetEmployeeId, String newRoleName) {
        validateSecretary(loggedInUser);

        if (loggedInUser.getId().equals(targetEmployeeId)) {
            throw new IllegalArgumentException("Δεν επιτρέπεται να αλλάξετε τον δικό σας ρόλο.");
        }

        Employee target = employeeRepo.findById(targetEmployeeId);
        if (target == null) {
            throw new IllegalArgumentException("Ο υπάλληλος δεν βρέθηκε.");
        }

        Role newRole = employeeRepo.findRoleByName(newRoleName);
        if (newRole == null) {
            throw new IllegalArgumentException("Ο ρόλος δεν βρέθηκε.");
        }

        String oldRoleName = target.getRole().getRoleName();
        if (oldRoleName.equals(newRoleName)) {
            return; // No change
        }

        target.setRole(newRole);
        employeeRepo.update(target);

        // If the person is no longer a MANAGER, remove them as manager from all subordinates
        if (!"MANAGER".equals(newRoleName)) {
            List<Employee> subordinates = employeeRepo.findByManagerId(targetEmployeeId);
            for (Employee subordinate : subordinates) {
                subordinate.setManager(null);
                employeeRepo.update(subordinate);
            }
        }

        // Audit log action
        auditService.logAction(
                loggedInUser,
                "UPDATE_ROLE",
                target.getId(),
                "Αλλαγή ρόλου από " + oldRoleName + " σε " + newRoleName
        );
    }

    @Transactional
    public void updateUserLeaveBalance(Employee loggedInUser, Integer targetEmployeeId, String leaveType, int newBalance) {
        validateSecretary(loggedInUser);

        Employee target = employeeRepo.findById(targetEmployeeId);
        if (target == null) {
            throw new IllegalArgumentException("Ο υπάλληλος δεν βρέθηκε.");
        }

        if (newBalance < 0) {
            throw new IllegalArgumentException("Το υπόλοιπο άδειας δεν μπορεί να είναι αρνητικό.");
        }

        // Fetch leave balances for the employee
        List<LeaveBalance> balances = leaveBalanceRepo.findByEmployeeId(targetEmployeeId);
        LeaveBalance targetBalance = null;
        for (LeaveBalance lb : balances) {
            if (lb.getLeaveType().equalsIgnoreCase(leaveType)) {
                targetBalance = lb;
                break;
            }
        }

        int oldBalance = 0;
        if (targetBalance == null) {
            // If the balance row doesn't exist for some reason, create it
            targetBalance = new LeaveBalance(target, leaveType, newBalance);
            em.persist(targetBalance);
        } else {
            oldBalance = targetBalance.getBalance();
            targetBalance.setBalance(newBalance);
            em.merge(targetBalance);
        }

        // Audit log action
        auditService.logAction(
                loggedInUser,
                "UPDATE_BALANCE",
                target.getId(),
                "Ενημέρωση υπολοίπου άδειας (" + leaveType + ") από " + oldBalance + " σε " + newBalance
        );
    }

    public List<LeaveBalance> getEmployeeLeaveBalances(Employee loggedInUser, Integer targetEmployeeId) {
        validateSecretary(loggedInUser);
        return leaveBalanceRepo.findByEmployeeId(targetEmployeeId);
    }

    public Employee getEmployeeById(Employee loggedInUser, Integer id) {
        validateSecretary(loggedInUser);
        return employeeRepo.findById(id);
    }

    public List<Employee> getAllManagers(Employee loggedInUser) {
        validateSecretary(loggedInUser);
        return employeeRepo.findAllManagers();
    }

    @Transactional
    public void updateUserManager(Employee loggedInUser, Integer targetEmployeeId, Integer managerId) {
        validateSecretary(loggedInUser);

        if (targetEmployeeId == null) {
            throw new IllegalArgumentException("Ο υπάλληλος δεν έχει καθοριστεί.");
        }

        if (managerId != null && targetEmployeeId.equals(managerId)) {
            throw new IllegalArgumentException("Ένας υπάλληλος δεν μπορεί να οριστεί ως προϊστάμενος του εαυτού του.");
        }

        Employee target = employeeRepo.findById(targetEmployeeId);
        if (target == null) {
            throw new IllegalArgumentException("Ο υπάλληλος δεν βρέθηκε.");
        }

        if (managerId == null) {
            String oldManagerName = target.getManager() != null ? target.getManager().getFullName() : "Κανένας";
            target.setManager(null);
            employeeRepo.update(target);

            auditService.logAction(
                    loggedInUser,
                    "UPDATE_MANAGER",
                    target.getId(),
                    "Αφαίρεση προϊσταμένου (προηγούμενος: " + oldManagerName + ")"
            );
        } else {
            Employee manager = employeeRepo.findById(managerId);
            if (manager == null) {
                throw new IllegalArgumentException("Ο προϊστάμενος δεν βρέθηκε.");
            }
            if (manager.getRole() == null || !"MANAGER".equals(manager.getRole().getRoleName())) {
                throw new IllegalArgumentException("Ο επιλεγμένος χρήστης δεν έχει ρόλο Προϊσταμένου.");
            }

            String oldManagerName = target.getManager() != null ? target.getManager().getFullName() : "Κανένας";
            target.setManager(manager);
            employeeRepo.update(target);

            auditService.logAction(
                    loggedInUser,
                    "UPDATE_MANAGER",
                    target.getId(),
                    "Ορισμός προϊσταμένου σε: " + manager.getFullName() + " (προηγούμενος: " + oldManagerName + ")"
            );
        }
    }
}
