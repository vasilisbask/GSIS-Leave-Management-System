package com.company.lms.repository;

import com.company.lms.model.Employee;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@ApplicationScoped
public class EmployeeRepository {

    @PersistenceContext
    private EntityManager em;

    public Employee findById(Integer id) {
        return em.find(Employee.class, id);
    }

    public void update(Employee employee) {
        em.merge(employee);
    }

    public List<Employee> findByManagerId(Integer managerId) {
        return em.createQuery(
                "SELECT e FROM Employee e WHERE e.manager.id = :managerId",
                Employee.class)
                .setParameter("managerId", managerId)
                .getResultList();
    }

    public List<Employee> findEmployeesPaginated(int first, int pageSize, String search, String roleFilter) {
        String jpql = "SELECT e FROM Employee e JOIN FETCH e.role LEFT JOIN FETCH e.manager WHERE 1=1";
        if (search != null && !search.trim().isEmpty()) {
            jpql += " AND (LOWER(e.firstName) LIKE :search OR LOWER(e.lastName) LIKE :search OR LOWER(e.email) LIKE :search)";
        }
        if (roleFilter != null && !roleFilter.trim().isEmpty()) {
            jpql += " AND e.role.roleName = :roleFilter";
        }
        jpql += " ORDER BY e.lastName ASC, e.firstName ASC";

        var query = em.createQuery(jpql, Employee.class);
        if (search != null && !search.trim().isEmpty()) {
            query.setParameter("search", "%" + search.trim().toLowerCase() + "%");
        }
        if (roleFilter != null && !roleFilter.trim().isEmpty()) {
            query.setParameter("roleFilter", roleFilter.trim());
        }
        query.setFirstResult(first);
        query.setMaxResults(pageSize);
        return query.getResultList();
    }

    public long countEmployees(String search, String roleFilter) {
        String jpql = "SELECT COUNT(e) FROM Employee e JOIN e.role r WHERE 1=1";
        if (search != null && !search.trim().isEmpty()) {
            jpql += " AND (LOWER(e.firstName) LIKE :search OR LOWER(e.lastName) LIKE :search OR LOWER(e.email) LIKE :search)";
        }
        if (roleFilter != null && !roleFilter.trim().isEmpty()) {
            jpql += " AND e.role.roleName = :roleFilter";
        }

        var query = em.createQuery(jpql, Long.class);
        if (search != null && !search.trim().isEmpty()) {
            query.setParameter("search", "%" + search.trim().toLowerCase() + "%");
        }
        if (roleFilter != null && !roleFilter.trim().isEmpty()) {
            query.setParameter("roleFilter", roleFilter.trim());
        }
        return query.getSingleResult();
    }

    public com.company.lms.model.Role findRoleByName(String roleName) {
        try {
            return em.createQuery("SELECT r FROM Role r WHERE r.roleName = :roleName", com.company.lms.model.Role.class)
                    .setParameter("roleName", roleName)
                    .getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public List<com.company.lms.model.Role> findAllRoles() {
        return em.createQuery("SELECT r FROM Role r ORDER BY r.roleName", com.company.lms.model.Role.class).getResultList();
    }

    public List<Employee> findAllManagers() {
        return em.createQuery(
                "SELECT e FROM Employee e JOIN FETCH e.role WHERE e.role.roleName = 'MANAGER' ORDER BY e.lastName ASC, e.firstName ASC",
                Employee.class)
                .getResultList();
    }
}