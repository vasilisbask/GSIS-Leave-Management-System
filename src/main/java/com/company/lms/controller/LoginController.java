package com.company.lms.controller;

import com.company.lms.model.Employee;
import com.company.lms.service.AuthService;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.regex.Pattern;

@Named
@SessionScoped
public class LoginController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    @Inject
    private AuthService authService;

    private String email;
    private String password;

    private String firstName;
    private String lastName;
    private String registerEmail;
    private String registerPassword;
    private String confirmPassword;

    private Employee loggedInUser;

    public String login() {

        if (!isValidEmail(email)) {

            this.email = null;

            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Παρακαλώ εισάγετε έγκυρη διεύθυνση email.");

            return null;
        }

        if (!authService.emailExists(email)) {
            addMessage(FacesMessage.SEVERITY_WARN,
                    "Δεν υπάρχει λογαριασμός με αυτό το email. Παρακαλώ κάντε εγγραφή.");
            return null;
        }

        loggedInUser = authService.login(email, password);

        if (loggedInUser == null) {
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Λάθος κωδικός πρόσβασης.");
            return null;
        }

        clearLoginForm();

        String role = loggedInUser.getRole().getRoleName();

        if ("MANAGER".equals(role)) {
            return "/manager/requests.xhtml?faces-redirect=true";
        } else if ("SECRETARY".equals(role)) {
            return "/secretary/users.xhtml?faces-redirect=true";
        }

        return "/employee/dashboard.xhtml?faces-redirect=true";
    }

    public String register() {

        if (!isValidEmail(registerEmail)) {

            this.registerEmail = null;

            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Παρακαλώ εισάγετε έγκυρη διεύθυνση email.");

            return null;
        }

        if (!isStrongPassword(registerPassword)) {
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Ο κωδικός πρέπει να έχει τουλάχιστον 8 χαρακτήρες, ένα κεφαλαίο γράμμα, ένα πεζό γράμμα και έναν αριθμό.");
            return null;
        }

        if (!registerPassword.equals(confirmPassword)) {
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Οι κωδικοί πρόσβασης δεν ταιριάζουν.");
            return null;
        }

        if (authService.emailExists(registerEmail)) {
            keepMessagesAfterRedirect();

            addMessage(FacesMessage.SEVERITY_WARN,
                    "Υπάρχει ήδη λογαριασμός με αυτό το email. Παρακαλώ κάντε σύνδεση.");

            clearRegisterForm();
            return "/login.xhtml?faces-redirect=true";
        }

        try {
            authService.register(firstName, lastName, registerEmail, registerPassword);

            clearRegisterForm();
            keepMessagesAfterRedirect();

            addMessage(FacesMessage.SEVERITY_INFO,
                    "Η εγγραφή ολοκληρώθηκε. Μπορείς να συνδεθείς.");

            return "/login.xhtml?faces-redirect=true";

        } catch (IllegalArgumentException e) {
            addMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
            return null;
        }
    }

    public String logout() {
        clearLoginForm();
        clearRegisterForm();

        FacesContext.getCurrentInstance()
                .getExternalContext()
                .invalidateSession();

        return "/login.xhtml?faces-redirect=true";    }

    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean isStrongPassword(String password) {

        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasLowercase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");

        return hasUppercase && hasLowercase && hasDigit;
    }

    private void addMessage(FacesMessage.Severity severity, String message) {
        FacesContext.getCurrentInstance().addMessage(
                null,
                new FacesMessage(severity, message, null)
        );
    }

    private void keepMessagesAfterRedirect() {
        FacesContext.getCurrentInstance()
                .getExternalContext()
                .getFlash()
                .setKeepMessages(true);
    }

    private void clearLoginForm() {
        email = null;
        password = null;
    }

    private void clearRegisterForm() {
        firstName = null;
        lastName = null;
        registerEmail = null;
        registerPassword = null;
        confirmPassword = null;
    }

    public boolean isLoggedIn() {
        return loggedInUser != null;
    }

    public String getLoggedInUserRole() {
        if (loggedInUser == null || loggedInUser.getRole() == null) {
            return null;
        }

        return loggedInUser.getRole().getRoleName();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getRegisterEmail() { return registerEmail; }
    public void setRegisterEmail(String registerEmail) { this.registerEmail = registerEmail; }

    public String getRegisterPassword() { return registerPassword; }
    public void setRegisterPassword(String registerPassword) { this.registerPassword = registerPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public Employee getLoggedInUser() { return loggedInUser; }
}