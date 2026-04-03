package com.stgian.repository;
import com.stgian.model.Drop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
@Repository
public interface DropRepository extends JpaRepository<Drop, Long> {
    Optional<Drop> findFirstByActiveTrueOrderByCreatedAtDesc();
}
