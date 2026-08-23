package com.example.le_games_app.repository;

import com.example.le_games_app.models.PlayerIdentity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerIdentityRepository extends JpaRepository<PlayerIdentity, UUID> {
}
