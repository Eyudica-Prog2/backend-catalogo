package ar.edu.unlp.turnos.catalog.catedra.infrastructure.persistence.adapter;

import ar.edu.unlp.turnos.catalog.catedra.domain.model.CatedraIntegration;
import ar.edu.unlp.turnos.catalog.catedra.domain.ports.out.IntegrationRepository;
import ar.edu.unlp.turnos.catalog.catedra.infrastructure.persistence.entity.CatedraIntegrationEntity;
import ar.edu.unlp.turnos.catalog.catedra.infrastructure.persistence.mapper.CatedraIntegrationMapper;
import ar.edu.unlp.turnos.catalog.catedra.infrastructure.persistence.repository.JpaCatedraIntegrationRepository;
import ar.edu.unlp.turnos.catalog.shared.crypto.SecretCipher;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA adapter for the {@link IntegrationRepository} port.
 *
 * <p>It keeps the encryption of the redis password in a single place: the domain model only
 * ever sees the clear value, the database only ever sees the ciphertext.</p>
 */
@Component
@RequiredArgsConstructor
public class JpaIntegrationRepositoryAdapter implements IntegrationRepository {

    private final JpaCatedraIntegrationRepository repository;
    private final CatedraIntegrationMapper mapper;
    private final SecretCipher secretCipher;

    @Override
    public Optional<CatedraIntegration> findCurrent() {
        return repository.findFirstByOrderByIdDesc()
                .map(entity -> mapper.toDomain(entity, secretCipher.decrypt(entity.getRedisPasswordCiphertext())));
    }

    @Override
    @Transactional
    public CatedraIntegration save(CatedraIntegration integration) {
        CatedraIntegrationEntity entity = repository.findFirstByOrderByIdDesc()
                .orElseGet(CatedraIntegrationEntity::new);
        String encryptedPassword = secretCipher.encrypt(integration.getRedisPassword());
        mapper.updateEntity(entity, integration, encryptedPassword);
        // This port is only called after a successful refresh, so saving means refreshing.
        entity.setLastRefreshedAt(Instant.now());

        CatedraIntegrationEntity saved = repository.save(entity);
        return mapper.toDomain(saved, integration.getRedisPassword());
    }
}
