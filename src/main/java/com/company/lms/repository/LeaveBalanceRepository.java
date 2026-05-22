package com.company.lms.repository;

import com.company.lms.model.LeaveBalance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@ApplicationScoped
public class LeaveBalanceRepository {

    @PersistenceContext
    private EntityManager em;

    public List<LeaveBalance> findByEmployeeId(Integer employeeId) {
        return em.createQuery(
                "SELECT lb FROM LeaveBalance lb WHERE lb.employee.id = :employeeId ORDER BY lb.leaveType",
                LeaveBalance.class)
                .setParameter("employeeId", employeeId)
                .getResultList();
    }

    public int getBalance(Integer employeeId, String leaveType) {
        List<Integer> result = em.createQuery(
                "SELECT lb.balance FROM LeaveBalance lb WHERE lb.employee.id = :employeeId AND lb.leaveType = :leaveType",
                Integer.class)
                .setParameter("employeeId", employeeId)
                .setParameter("leaveType", leaveType)
                .getResultList();
        return result.isEmpty() ? 0 : result.get(0);
    }

    public int deductBalance(Integer employeeId, String leaveType, int days) {
        return em.createQuery(
                "UPDATE LeaveBalance lb SET lb.balance = lb.balance - :days " +
                "WHERE lb.employee.id = :employeeId AND lb.leaveType = :leaveType AND lb.balance >= :days")
                .setParameter("days", days)
                .setParameter("employeeId", employeeId)
                .setParameter("leaveType", leaveType)
                .executeUpdate();
    }

    public int sumTotalForManager(Integer managerId) {
        Long sum = em.createQuery(
                "SELECT COALESCE(SUM(lb.balance), 0) FROM LeaveBalance lb WHERE lb.employee.manager.id = :managerId",
                Long.class)
                .setParameter("managerId", managerId)
                .getSingleResult();
        return sum == null ? 0 : sum.intValue();
    }
}
