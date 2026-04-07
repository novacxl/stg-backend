package com.stgian.repository;
import com.stgian.model.Drop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface DropRepository extends JpaRepository<Drop, Long> {
    Optional<Drop> findFirstByActiveTrueOrderByCreatedAtDesc();

    // Uma única query UPDATE ao invés de N saves no loop
    @Modifying
    @Query("UPDATE Drop d SET d.active = false WHERE d.active = true")
    void deactivateAll();
}
