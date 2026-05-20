package com.company.lms.controller;

import com.company.lms.model.Employee;
import com.company.lms.model.LeaveBalance;
import com.company.lms.model.Role;
import com.company.lms.service.SecretaryService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.SortMeta;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Named
@ViewScoped
public class SecretaryController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SecretaryService secretaryService;

    @Inject
    private LoginController loginController;

    private LazyDataModel<Employee> lazyModel;
    private List<Role> allRoles;

    private String searchQuery = "";
    private String filterRole = "";

    // Dialog state
    private Employee selectedUser;
    private String newRoleName;
    
    // Balance editing state
    private List<LeaveBalance> selectedUserBalances;
    private String selectedLeaveType;
    private int newLeaveBalanceValue;

    // Manager assignment state
    private List<Employee> allManagers;
    private Integer newManagerId;

    @PostConstruct
    public void init() {
        lazyModel = new LazyDataModel<Employee>() {
            private static final long serialVersionUID = 1L;

            @Override
            public int count(Map<String, FilterMeta> filterBy) {
                Employee loggedInUser = loginController.getLoggedInUser();
                if (loggedInUser == null) return 0;
                return (int) secretaryService.countEmployees(loggedInUser, searchQuery, filterRole);
            }

            @Override
            public List<Employee> load(int firstRow, int numRows, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
                Employee loggedInUser = loginController.getLoggedInUser();
                if (loggedInUser == null) return List.of();
                return secretaryService.getPaginatedEmployees(loggedInUser, firstRow, numRows, searchQuery, filterRole);
            }

            @Override
            public String getRowKey(Employee employee) {
                return employee != null ? String.valueOf(employee.getId()) : null;
            }

            @Override
            public Employee getRowData(String rowKey) {
                if (rowKey == null) return null;
                Employee loggedInUser = loginController.getLoggedInUser();
                if (loggedInUser == null) return null;
                try {
                    int id = Integer.parseInt(rowKey);
                    return secretaryService.getEmployeeById(loggedInUser, id);
                } catch (Exception e) {
                    return null;
                }
            }
        };

        loadRoles();
    }

    public void loadRoles() {
        try {
            Employee loggedInUser = loginController.getLoggedInUser();
            if (loggedInUser != null) {
                allRoles = secretaryService.getAllRoles(loggedInUser);
            }
        } catch (Exception e) {
            allRoles = List.of();
        }
    }

    public void onSearch() {
        // Triggered by search input keyup
    }

    public void onFilter() {
        // Triggered by role filter change
    }

    public void selectUserForRoleEdit(Employee user) {
        this.selectedUser = user;
        if (user != null && user.getRole() != null) {
            this.newRoleName = user.getRole().getRoleName();
        }
    }

    public void saveUserRole() {
        try {
            Employee loggedInUser = loginController.getLoggedInUser();
            if (selectedUser == null) {
                throw new IllegalArgumentException("Δεν έχει επιλεγεί χρήστης.");
            }
            if (loggedInUser != null && selectedUser.getId().equals(loggedInUser.getId())) {
                throw new IllegalArgumentException("Δεν επιτρέπεται να αλλάξετε τον δικό σας ρόλο.");
            }
            secretaryService.updateUserRole(loggedInUser, selectedUser.getId(), newRoleName);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Επιτυχία", "Ο ρόλος του χρήστη ενημερώθηκε επιτυχώς."));
            selectedUser = null;
            newRoleName = null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Σφάλμα", e.getMessage()));
        }
    }

    public void selectUserForBalances(Employee user) {
        this.selectedUser = user;
        this.selectedLeaveType = null;
        this.newLeaveBalanceValue = 0;
        loadUserBalances();
    }

    public void loadUserBalances() {
        try {
            Employee loggedInUser = loginController.getLoggedInUser();
            if (selectedUser != null && loggedInUser != null) {
                selectedUserBalances = secretaryService.getEmployeeLeaveBalances(loggedInUser, selectedUser.getId());
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Σφάλμα", e.getMessage()));
        }
    }

    public void saveLeaveBalance() {
        try {
            Employee loggedInUser = loginController.getLoggedInUser();
            if (selectedUser == null) {
                throw new IllegalArgumentException("Δεν έχει επιλεγεί χρήστης.");
            }
            if (selectedLeaveType == null || selectedLeaveType.isEmpty()) {
                throw new IllegalArgumentException("Παρακαλώ επιλέξτε τύπο άδειας.");
            }
            secretaryService.updateUserLeaveBalance(loggedInUser, selectedUser.getId(), selectedLeaveType, newLeaveBalanceValue);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Επιτυχία", "Το υπόλοιπο άδειας ενημερώθηκε επιτυχώς."));
            // Reload user balances for the dialog table
            loadUserBalances();
            selectedLeaveType = null;
            newLeaveBalanceValue = 0;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Σφάλμα", e.getMessage()));
        }
    }

    public void loadManagers() {
        try {
            Employee loggedInUser = loginController.getLoggedInUser();
            if (loggedInUser != null) {
                allManagers = secretaryService.getAllManagers(loggedInUser);
            }
        } catch (Exception e) {
            allManagers = List.of();
        }
    }

    public void selectUserForManagerEdit(Employee user) {
        this.selectedUser = user;
        if (user != null && user.getManager() != null) {
            this.newManagerId = user.getManager().getId();
        } else {
            this.newManagerId = null;
        }
        loadManagers();
    }

    public void saveUserManager() {
        try {
            Employee loggedInUser = loginController.getLoggedInUser();
            if (selectedUser == null) {
                throw new IllegalArgumentException("Δεν έχει επιλεγεί χρήστης.");
            }
            secretaryService.updateUserManager(loggedInUser, selectedUser.getId(), newManagerId);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Επιτυχία", "Ο προϊστάμενος του χρήστη ενημερώθηκε επιτυχώς."));
            selectedUser = null;
            newManagerId = null;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Σφάλμα", e.getMessage()));
        }
    }

    // Getters and Setters
    public LazyDataModel<Employee> getLazyModel() { return lazyModel; }
    public List<Role> getAllRoles() { return allRoles; }
    public String getSearchQuery() { return searchQuery; }
    public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }
    public String getFilterRole() { return filterRole; }
    public void setFilterRole(String filterRole) { this.filterRole = filterRole; }
    public Employee getSelectedUser() { return selectedUser; }
    public void setSelectedUser(Employee selectedUser) { this.selectedUser = selectedUser; }
    public String getNewRoleName() { return newRoleName; }
    public void setNewRoleName(String newRoleName) { this.newRoleName = newRoleName; }
    public List<LeaveBalance> getSelectedUserBalances() { return selectedUserBalances; }
    public String getSelectedLeaveType() { return selectedLeaveType; }
    public void setSelectedLeaveType(String selectedLeaveType) { this.selectedLeaveType = selectedLeaveType; }
    public int getNewLeaveBalanceValue() { return newLeaveBalanceValue; }
    public void setNewLeaveBalanceValue(int newLeaveBalanceValue) { this.newLeaveBalanceValue = newLeaveBalanceValue; }
    public List<Employee> getAllManagers() { return allManagers; }
    public Integer getNewManagerId() { return newManagerId; }
    public void setNewManagerId(Integer newManagerId) { this.newManagerId = newManagerId; }
}
