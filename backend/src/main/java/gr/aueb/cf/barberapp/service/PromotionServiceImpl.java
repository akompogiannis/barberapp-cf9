package gr.aueb.cf.barberapp.service;

import gr.aueb.cf.barberapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.barberapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.barberapp.dto.PromotionInsertDTO;
import gr.aueb.cf.barberapp.dto.PromotionReadOnlyDTO;
import gr.aueb.cf.barberapp.dto.PromotionUpdateDTO;
import gr.aueb.cf.barberapp.mapper.Mapper;
import gr.aueb.cf.barberapp.model.Barber;
import gr.aueb.cf.barberapp.model.BarberService;
import gr.aueb.cf.barberapp.model.Promotion;
import gr.aueb.cf.barberapp.repository.BarberRepository;
import gr.aueb.cf.barberapp.repository.BarberServiceRepository;
import gr.aueb.cf.barberapp.repository.PromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service                        // IoC Container
@RequiredArgsConstructor        // DI
@Slf4j                          // Logger
public class PromotionServiceImpl implements IPromotionService {

    private final PromotionRepository promotionRepository;
    private final BarberServiceRepository barberServiceRepository;
    private final BarberRepository barberRepository;
    private final Mapper mapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<PromotionReadOnlyDTO> getActivePromotions() {
        return promotionRepository.findActiveOn(LocalDate.now(clock)).stream()
                .map(mapper::mapToPromotionReadOnlyDTO)
                .toList();
    }

    @Override
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS')")
    @Transactional(readOnly = true)
    public Page<PromotionReadOnlyDTO> getPaginatedPromotions(Pageable pageable) {
        return promotionRepository.findAllByDeletedFalse(pageable)
                .map(mapper::mapToPromotionReadOnlyDTO);
    }

    @Override
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS')")
    @Transactional(rollbackFor = {EntityNotFoundException.class, EntityInvalidArgumentException.class})
    public PromotionReadOnlyDTO savePromotion(PromotionInsertDTO dto)
            throws EntityNotFoundException, EntityInvalidArgumentException {

        // Bean validation cannot express a cross-field rule, so the date order is
        // checked here as well as by the CHECK constraint in the schema.
        if (dto.validTo().isBefore(dto.validFrom())) {
            throw new EntityInvalidArgumentException("Promotion", "The end date cannot precede the start date");
        }

        Barber barber = barberRepository.findFirstByDeletedFalseOrderByIdAsc()
                .orElseThrow(() -> new EntityInvalidArgumentException("Barber",
                        "No barber profile exists - check the seed migration"));

        Promotion promotion = new Promotion();
        promotion.setTitle(dto.title());
        promotion.setDescription(dto.description());
        promotion.setDiscountPercent(dto.discountPercent());
        promotion.setValidFrom(dto.validFrom());
        promotion.setValidTo(dto.validTo());
        promotion.setActive(true);
        barber.addPromotion(promotion);

        for (BarberService service : resolveServices(dto.serviceUuids())) {
            promotion.addService(service);
        }

        promotionRepository.save(promotion);
        log.info("Promotion title={} created at {}%", dto.title(), dto.discountPercent());
        return mapper.mapToPromotionReadOnlyDTO(promotion);
    }

    @Override
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS')")
    @Transactional(rollbackFor = {EntityNotFoundException.class, EntityInvalidArgumentException.class})
    public PromotionReadOnlyDTO updatePromotion(PromotionUpdateDTO dto)
            throws EntityNotFoundException, EntityInvalidArgumentException {

        if (dto.validTo().isBefore(dto.validFrom())) {
            throw new EntityInvalidArgumentException("Promotion", "The end date cannot precede the start date");
        }

        Promotion promotion = promotionRepository.findByUuidAndDeletedFalse(dto.uuid())
                .orElseThrow(() -> new EntityNotFoundException("Promotion",
                        "Promotion with uuid=" + dto.uuid() + " not found"));

        promotion.setTitle(dto.title());
        promotion.setDescription(dto.description());
        promotion.setDiscountPercent(dto.discountPercent());
        promotion.setValidFrom(dto.validFrom());
        promotion.setValidTo(dto.validTo());
        promotion.setActive(dto.active());

        // Replace the whole set instead of diffing - the DTO carries the full list, and
        // booked appointments already froze their price.
        Set<BarberService> current = new HashSet<>(promotion.getServices());
        current.forEach(promotion::removeService);
        for (BarberService service : resolveServices(dto.serviceUuids())) {
            promotion.addService(service);
        }

        log.info("Promotion uuid={} updated", dto.uuid());
        return mapper.mapToPromotionReadOnlyDTO(promotion);
    }

    @Override
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS')")
    @Transactional(rollbackFor = EntityNotFoundException.class)
    public PromotionReadOnlyDTO deletePromotion(UUID uuid) throws EntityNotFoundException {
        Promotion promotion = promotionRepository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Promotion",
                        "Promotion with uuid=" + uuid + " not found"));

        promotion.softDelete();
        promotion.setActive(false);

        log.info("Promotion uuid={} soft-deleted", uuid);
        return mapper.mapToPromotionReadOnlyDTO(promotion);
    }

    // Resolves the uuids, failing loudly if any of them is unknown.
    private List<BarberService> resolveServices(List<UUID> uuids) throws EntityNotFoundException {
        List<BarberService> services = new java.util.ArrayList<>();
        for (UUID uuid : uuids) {
            services.add(barberServiceRepository.findByUuidAndDeletedFalse(uuid)
                    .orElseThrow(() -> new EntityNotFoundException("Service",
                            "Service with uuid=" + uuid + " not found")));
        }
        return services;
    }
}
