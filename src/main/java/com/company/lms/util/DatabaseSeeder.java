package com.company.lms.util;

import com.company.lms.model.Role;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.logging.Logger;

@ApplicationScoped
public class DatabaseSeeder {

    private static final Logger LOGGER = Logger.getLogger(DatabaseSeeder.class.getName());

    @PersistenceContext(unitName = "lmsPU")
    private EntityManager em;

    @Transactional
    public void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        LOGGER.info("Starting database seeding check...");
        try {
            seedRole("EMPLOYEE");
            seedRole("MANAGER");
            seedRole("SECRETARY");
            LOGGER.info("Database seeding check completed successfully.");
        } catch (Exception e) {
            LOGGER.severe("Failed to seed database roles: " + e.getMessage());
        }
    }

    private void seedRole(String roleName) {
        Long count = em.createQuery("SELECT COUNT(r) FROM Role r WHERE r.roleName = :roleName", Long.class)
                .setParameter("roleName", roleName)
                .getSingleResult();
        if (count == 0) {
            LOGGER.info("Seeding role: " + roleName);
            Role role = new Role();
            role.setRoleName(roleName);
            em.persist(role);
        }
    }
}
