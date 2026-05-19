package com.company.lms.controller;

import com.company.lms.model.Employee;
import com.company.lms.model.LeaveRequest;
import com.company.lms.service.ManagerLeaveService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import java.io.ByteArrayInputStream;

@Named
@ViewScoped
public class ManagerController implements Serializable {

    @Inject
    private ManagerLeaveService managerService;

    @Inject
    private LoginController loginController;

    private List<LeaveRequest> pendingRequests;
    private List<LeaveRequest> selectedRequests;
    private LeaveRequest selectedRequest;
    private String managerComment;
    private String pendingAction;

    @PostConstruct
    public void init() {
        loadRequests();
    }

    private void loadRequests() {
        Employee currentManager = loginController.getLoggedInUser();

        if (currentManager == null) {
            pendingRequests = List.of();
            selectedRequests = List.of();
            return;
        }

        pendingRequests = managerService.getPendingRequests(currentManager);
        selectedRequests = new ArrayList<>();
    }

    public void approveLeave() {
        try {
            Employee currentManager = loginController.getLoggedInUser();

            managerService.approveLeave(selectedRequest.getId(), currentManager, managerComment);
            
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Επιτυχία", "Η άδεια εγκρίθηκε"));
            
            // Reset and reload
            managerComment = null;
            selectedRequest = null;
            pendingAction = null; 
            loadRequests();
            
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Σφάλμα", e.getMessage()));
        }
    }

    public void rejectLeave() {
        try {
            if (managerComment == null || managerComment.trim().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(
                                FacesMessage.SEVERITY_WARN,
                                "Προειδοποίηση",
                                "Απαιτείται σχόλιο για την απόρριψη."
                        ));

                FacesContext.getCurrentInstance().validationFailed();

                return;
            }

            Employee currentManager = loginController.getLoggedInUser();

            managerService.rejectLeave(selectedRequest.getId(), currentManager, managerComment);
            
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Επιτυχία", "Η άδεια απορρίφθηκε"));
            
            managerComment = null;
            selectedRequest = null;
            pendingAction = null;
            loadRequests();

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null, 
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Σφάλμα", e.getMessage()));
        }
    }

    public void bulkApprove() {
        try {
            Employee currentManager = loginController.getLoggedInUser();
            int count = managerService.approveLeaves(getSelectedRequestIds(), currentManager, managerComment);

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_INFO,
                            "Επιτυχία", getBulkApproveMessage(count)));

            managerComment = null;
            selectedRequests = new ArrayList<>();
            pendingAction = null;
            loadRequests();

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Σφάλμα", e.getMessage()));
        }
    }

    public void bulkReject() {
        try {
            if (managerComment == null || managerComment.trim().isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(
                                FacesMessage.SEVERITY_WARN,
                                "Προειδοποίηση",
                                "Απαιτείται σχόλιο για την απόρριψη."
                        ));

                FacesContext.getCurrentInstance().validationFailed();

                return;
            }

            Employee currentManager = loginController.getLoggedInUser();
            int count = managerService.rejectLeaves(getSelectedRequestIds(), currentManager, managerComment);

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_INFO,
                            "Επιτυχία", getBulkRejectMessage(count)));

            managerComment = null;
            selectedRequests = new ArrayList<>();
            pendingAction = null;
            loadRequests();

        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Σφάλμα", e.getMessage()));
        }
    }

    private String getRequestWord(int count) {
        return count == 1 ? "αίτημα" : "αιτήματα";
    }

    private String getBulkApproveMessage(int count) {
        return (count == 1 ? "Εγκρίθηκε " : "Εγκρίθηκαν ")
                + count + " " + getRequestWord(count);
    }

    private String getBulkRejectMessage(int count) {
        return (count == 1 ? "Απορρίφθηκε " : "Απορρίφθηκαν ")
                + count + " " + getRequestWord(count);
    }

    private List<Integer> getSelectedRequestIds() {
        if (selectedRequests == null) {
            return List.of();
        }
        return selectedRequests.stream()
                .map(LeaveRequest::getId)
                .collect(Collectors.toList());
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

    // Getters and Setters
    public List<LeaveRequest> getPendingRequests() { return pendingRequests; }
    public List<LeaveRequest> getSelectedRequests() { return selectedRequests; }
    public void setSelectedRequests(List<LeaveRequest> selectedRequests) { this.selectedRequests = selectedRequests; }
    public LeaveRequest getSelectedRequest() { return selectedRequest; }
    public void setSelectedRequest(LeaveRequest selectedRequest) { this.selectedRequest = selectedRequest; }
    public String getManagerComment() { return managerComment; }
    public void setManagerComment(String managerComment) { this.managerComment = managerComment; }
    public String getPendingAction() { return pendingAction; }
    public void setPendingAction(String pendingAction) { this.pendingAction = pendingAction; }
}