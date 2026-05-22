package com.company.lms.repository;

import com.company.lms.model.LeaveRequest;
import com.company.lms.model.LeaveStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class LeaveRepository {

    @PersistenceContext
    private EntityManager em;

    public LeaveRequest findById(Integer id) {
        return em.find(LeaveRequest.class, id);
    }

    public List<LeaveRequest> findByEmployeeId(Integer employeeId) {
        return em.createQuery(
                "SELECT l FROM LeaveRequest l JOIN FETCH l.employee WHERE l.employee.id = :employeeId ORDER BY l.startDate DESC",
                LeaveRequest.class)
                .setParameter("employeeId", employeeId)
                .getResultList();
    }

    public List<LeaveRequest> findPendingByManagerId(Integer managerId) {
        return em.createQuery(
                        "SELECT l FROM LeaveRequest l " +
                                "JOIN FETCH l.employee e " +
                                "WHERE l.status = :status " +
                                "AND e.manager.id = :managerId " +
                                "ORDER BY l.startDate ASC",
                        LeaveRequest.class
                )
                .setParameter("status", LeaveStatus.PENDING)
                .setParameter("managerId", managerId)
                .getResultList();
    }

    public boolean existsOverlappingRequest(Integer employeeId, LocalDate startDate, LocalDate endDate) {
        Long count = em.createQuery(
                "SELECT COUNT(l) FROM LeaveRequest l WHERE l.employee.id = :employeeId " +
                "AND (l.status = :pending OR l.status = :approved) " +
                "AND l.startDate <= :endDate AND l.endDate >= :startDate",
                Long.class)
                .setParameter("employeeId", employeeId)
                .setParameter("pending", LeaveStatus.PENDING)
                .setParameter("approved", LeaveStatus.APPROVED)
                .setParameter("startDate", startDate)
                .setParameter("endDate", endDate)
                .getSingleResult();

        return count > 0;
    }

    public void save(LeaveRequest request) {
        em.persist(request);
    }

    public void update(LeaveRequest request) {
        em.merge(request);
    }

    public List<LeaveRequest> findApprovedByManagerIdInRange(Integer managerId, LocalDate from, LocalDate to) {
        return em.createQuery(
                "SELECT l FROM LeaveRequest l " +
                        "JOIN FETCH l.employee e " +
                        "WHERE l.status = :status " +
                        "AND e.manager.id = :managerId " +
                        "AND l.startDate <= :to AND l.endDate >= :from " +
                        "ORDER BY l.startDate ASC",
                LeaveRequest.class)
                .setParameter("status", LeaveStatus.APPROVED)
                .setParameter("managerId", managerId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    public Optional<LocalDateTime> findOldestPendingCreatedAt(Integer managerId) {
        List<LocalDateTime> result = em.createQuery(
                "SELECT l.createdAt FROM LeaveRequest l " +
                        "WHERE l.status = :status AND l.employee.manager.id = :managerId " +
                        "ORDER BY l.createdAt ASC",
                LocalDateTime.class)
                .setParameter("status", LeaveStatus.PENDING)
                .setParameter("managerId", managerId)
                .setMaxResults(1)
                .getResultList();
        return result.isEmpty() ? Optional.empty() : Optional.ofNullable(result.get(0));
    }

    public List<Object[]> monthlyTrendByType(Integer managerId, int year) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = from.plusYears(1);

        return em.createQuery(
                "SELECT FUNCTION('MONTH', l.startDate), l.leaveType, COUNT(l) FROM LeaveRequest l " +
                        "WHERE l.status = :status AND l.employee.manager.id = :managerId " +
                        "AND l.startDate >= :from AND l.startDate < :to " +
                        "GROUP BY FUNCTION('MONTH', l.startDate), l.leaveType " +
                        "ORDER BY FUNCTION('MONTH', l.startDate)",
                Object[].class)
                .setParameter("status", LeaveStatus.APPROVED)
                .setParameter("managerId", managerId)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    public LeaveRequest findByIdWithEmployee(Integer id) {
        if (id == null) {
            return null;
        }

        List<LeaveRequest> results = em.createQuery(
                        "SELECT l FROM LeaveRequest l " +
                                "JOIN FETCH l.employee " +
                                "WHERE l.id = :id",
                        LeaveRequest.class)
                .setParameter("id", id)
                .getResultList();

        return results.isEmpty() ? null : results.get(0);
    }
}