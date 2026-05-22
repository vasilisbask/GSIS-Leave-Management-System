# GSIS Leave Management System

A Jakarta EE web application for managing employee leave requests in an enterprise-style environment.

Employees can submit leave requests and view their leave history. Managers can review, approve or reject requests from their assigned employees, view statistics, and inspect the audit log. Secretaries can manage users, assign roles, set manager assignments, and adjust leave balances across the organization.

## Internship

This project is being developed as part of an internship at the **General Secretariat of Information Systems (GSIS — Γενική Γραμματεία Πληροφοριακών Συστημάτων)**.

## Screenshots

### Login
![Login page](docs/screenshots/login.png)

### Employee Dashboard
![Employee dashboard](docs/screenshots/employee_dashboard.png)

### Leave History
![Leave history](docs/screenshots/employee_history.png)

### Manager — Pending Requests
![Manager requests](docs/screenshots/manager_requests.png)

### Manager — Statistics
![Manager statistics](docs/screenshots/manager_stats.png)

### Manager — Audit Log
![Audit log](docs/screenshots/manager_audit.png)

### Secretary — User Management
![Secretary user management](docs/screenshots/secretary_users.png)


## Tech Stack

- **Java 21**
- **Jakarta EE 10**
- **JSF / Facelets**
- **PrimeFaces**
- **CDI & JPA (Hibernate)**
- **MySQL**
- **Payara Server**
- **Maven**
- **BCrypt**
- **iText PDF**

## Main Features

### Employee

- **Register and Sign In**: Secure registration and login using BCrypt password hashing.
- **Leave Request Submission**:
  - Submit leave requests specifying start/end date, leave type, and reason.
  - **PDF Attachments**: Support uploading PDF files as supporting documents (e.g., medical certificates for sick leave), strictly validated by file extension and MIME type.
- **Leave History & Balances**:
  - View comprehensive leave history with interactive status and type filtering.
  - Download previously uploaded PDF attachments.
  - Track annual, sick, and other leave balance categories in real time.
- **Official PDF Export**:
  - **Leave Certificate**: Download a highly formatted official leave decision document (`Επίσημη Απόφαση Έγκρισης Άδειας`) for any approved request, incorporating official GSIS letterhead, detailed working vs. calendar day breakdowns, manager comments, and a digital signature block.
  - **Leave History Statement**: Export a professional service record (`Υπηρεσιακή Κατάσταση Αδειών`) listing active balance allowances, remaining days, and detailed request histories with color-coded status badges.
- **Validations**: Real-time checking for overlapping leave requests, past dates, and available balance limits.
- **Greek Holiday Integration**: Automatic calculation of actual working days, excluding weekends and official Greek public holidays.

### Manager

- **Employee Management**: View pending requests from assigned employees only.
- **Review & Decisions**: Approve or reject requests with review comments.
- **Document Review**: Download and inspect employee-attached PDF supporting documents directly from the pending request view.
- **Interactive Dashboard & Statistics**:
  - **Status Distribution**: Pie Chart illustrating the ratio of approved, pending, and rejected requests.
  - **Leave Type Metrics**: Bar Chart detailing request frequency across different leave types.
  - **Yearly Trend Analytics**: Multi-series Line Chart mapping monthly leave distribution throughout the calendar year.
  - **KPI Widgets**: Live trackers for *Leave Utilization %*, *Oldest Pending Request Age* (in days), and an *On Leave This Week* team roster.
- **Team Schedule Calendar**: Interactive calendar view (built on PrimeFaces Schedule) showing approved team leaves in color-coded blocks for efficient shift planning.
- **Audit Logging**: View, filter, and inspect comprehensive action logs of administrative actions.
- **Landscape PDF Export**: Export the manager's audit and safety log into a wide, landscape-oriented PDF table for compliance reviews.
- **Personal Submissions**: Submit and view personal leave requests as an employee.

### Secretary

- **User Administration**: View all registered users with paginated search and role filtering.
- **Role Assignment**: Elevate or demote user roles (`Employee`, `Manager`, `Secretary`).
- **Manager Assignment**: Set or modify reporting manager relationships for employees.
- **Leave Balance Adjustments**: Manually adjust leave balances (annual, sick, other) per employee with all updates recorded in the audit log.
- **Self-Modification Restrictions**: System guards preventing secretaries from altering their own role or manager assignments.

## Database

The only manual step required is creating the empty database. JPA will generate all tables automatically on first deployment, and the application will seed the required roles (`EMPLOYEE`, `MANAGER`, `SECRETARY`) at startup via `DatabaseSeeder`.

```sql
CREATE DATABASE employee_leaves;
```

## Payara Setup

Create a JDBC resource with:

```text
JNDI Name: jdbc/lms
Database: employee_leaves
```

Recommended MySQL connection properties:

```text
allowPublicKeyRetrieval=true
useSSL=false
serverTimezone=UTC
```

## Build and Deploy

Build the project:

```bash
mvn clean package
```

Deploy the generated WAR file from:

```text
target/leave-management.war
```

Application URL example:

```text
http://localhost:8080/leave-management-system
```

## Business Rules

- **Manager Mandate**: Employees without an assigned manager cannot submit leave requests.
- **Security Scopes**: Managers can only see and review requests from employees assigned to them.
- **Self-Approval Guard**: Managers cannot approve or reject their own leave requests.
- **Overlap Prevention**: Leave requests cannot overlap with existing pending or approved requests.
- **Working Day Calculations**: Leave duration is calculated using working days, excluding weekends and official Greek public holidays.
- **Role Alteration Safeguards**: Secretaries cannot change their own roles.
- **Cyclic Assignment Prevention**: A user cannot be assigned as their own manager.
- **Subordinate Unassignment**: If a manager's role is changed to employee or secretary, all their subordinates are automatically unassigned.
- **Attachment Constraints**: Uploaded supporting documents must be strictly valid PDF files (both file extension `.pdf` and content type `application/pdf`).
- **Greek Unicode Compliance**: All exported PDFs dynamically resolve and embed local system fonts (e.g., `Arial`, `Tahoma`, `DejaVu Sans`) to ensure proper rendering of Greek characters.

## Security

- Passwords are hashed with BCrypt.
- Role-based access control (RBAC) separates employee, manager, and secretary scopes.
- Cryptographically secure session management.
- Comprehensive security and compliance audit logs capturing manager decisions, role adjustments, balance alterations, and manager changes.

## Email Notifications

The application supports SMTP email notifications for:
- new user registration
- leave request submission
- leave request approval
- leave request rejection

Email credentials are not stored in the source code. SMTP settings are configured through Payara JVM Options.

## Authors

Developed during an internship at GSIS (General Secretariat of Information Systems) as part of a university academic software development project.

- Dimitris Koutsompinas
- Vasilis Baskairon