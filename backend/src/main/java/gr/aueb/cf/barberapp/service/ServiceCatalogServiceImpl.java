package gr.aueb.cf.barberapp.service;

import gr.aueb.cf.barberapp.core.exceptions.EntityAlreadyExistsException;
import gr.aueb.cf.barberapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.barberapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.barberapp.dto.BarberServiceInsertDTO;
import gr.aueb.cf.barberapp.dto.BarberServiceReadOnlyDTO;
import gr.aueb.cf.barberapp.dto.BarberServiceUpdateDTO;
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
import java.util.List;
import java.util.UUID;

@Service                        // IoC Container
@RequiredArgsConstructor        // DI
@Slf4j                          // Logger
public class ServiceCatalogServiceImpl implements IServiceCatalogService {

    private final BarberServiceRepository barberServiceRepository;
    private final BarberRepository barberRepository;
    private final PromotionRepository promotionRepository;
    private final Mapper mapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<BarberServiceReadOnlyDTO> getActiveServices() {
        LocalDate today = LocalDate.now(clock);
        return barberServiceRepository.findAllByDeletedFalseAndActiveTrueOrderByNameAsc().stream()
                .map(service -> mapper.mapToServiceReadOnlyDTO(service, bestPromotionFor(service, today)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BarberServiceReadOnlyDTO getServiceByUuid(UUID uuid) throws EntityNotFoundException {
        BarberService service = barberServiceRepository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Service",
                        "Service with uuid=" + uuid + " not found"));
        return mapper.mapToServiceReadOnlyDTO(service, bestPromotionFor(service, LocalDate.now(clock)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BarberServiceReadOnlyDTO> getPaginatedServices(Pageable pageable) {
        return barberServiceRepository.findAllByDeletedFalse(pageable)
                .map(mapper::mapToServiceReadOnlyDTO);
    }

    @Override
    @PreAuthorize("hasAuthority('MANAGE_SERVICES')")
    @Transactional(rollbackFor = {EntityAlreadyExistsException.class, EntityInvalidArgumentException.class})
    public BarberServiceReadOnlyDTO saveService(BarberServiceInsertDTO dto)
            throws EntityAlreadyExistsException, EntityInvalidArgumentException {

        try {
            Barber barber = requireBarber();

            if (barberServiceRepository
                    .existsByBarber_IdAndNameIgnoreCaseAndDeletedFalse(barber.getId(), dto.name())) {
                throw new EntityAlreadyExistsException("Service",
                        "A service named " + dto.name() + " already exists");
            }

            BarberService service = new BarberService();
            service.setName(dto.name());
            service.setDescription(dto.description());
            service.setDurationMinutes(dto.durationMinutes());
            service.setPrice(dto.price());
            service.setActive(true);
            barber.addService(service);

            barberServiceRepository.save(service);
            log.info("Service name={} created", dto.name());
            return mapper.mapToServiceReadOnlyDTO(service);

        } catch (EntityAlreadyExistsException e) {
            log.warn("Create failed for service name={}. Already exists.", dto.name());
            throw e;
        }
    }

    @Override
    @PreAuthorize("hasAuthority('MANAGE_SERVICES')")
    @Transactional(rollbackFor = {EntityNotFoundException.class, EntityAlreadyExistsException.class})
    public BarberServiceReadOnlyDTO updateService(BarberServiceUpdateDTO dto)
            throws EntityNotFoundException, EntityAlreadyExistsException {

        BarberService service = barberServiceRepository.findByUuidAndDeletedFalse(dto.uuid())
                .orElseThrow(() -> new EntityNotFoundException("Service",
                        "Service with uuid=" + dto.uuid() + " not found"));

        if (!service.getName().equalsIgnoreCase(dto.name())
                && barberServiceRepository.existsByBarber_IdAndNameIgnoreCaseAndDeletedFalse(
                        service.getBarber().getId(), dto.name())) {
            throw new EntityAlreadyExistsException("Service",
                    "A service named " + dto.name() + " already exists");
        }

        // Changing the duration only affects future bookings: existing appointments
        // stored their own end time when they were created.
        service.setName(dto.name());
        service.setDescription(dto.description());
        service.setDurationMinutes(dto.durationMinutes());
        service.setPrice(dto.price());
        service.setActive(dto.active());

        log.info("Service uuid={} updated", dto.uuid());
        return mapper.mapToServiceReadOnlyDTO(service);
    }

    @Override
    @PreAuthorize("hasAuthority('MANAGE_SERVICES')")
    @Transactional(rollbackFor = EntityNotFoundException.class)
    public BarberServiceReadOnlyDTO deleteService(UUID uuid) throws EntityNotFoundException {
        BarberService service = barberServiceRepository.findByUuidAndDeletedFalse(uuid)
                .orElseThrow(() -> new EntityNotFoundException("Service",
                        "Service with uuid=" + uuid + " not found"));

        // Soft delete and deactivate together: the row survives for the sake of
        // appointment history, but it stops being offered.
        service.softDelete();
        service.setActive(false);

        log.info("Service uuid={} soft-deleted", uuid);
        return mapper.mapToServiceReadOnlyDTO(service);
    }

    // The promotion that gives the customer the lowest price today, or null.
    // The query orders by discount descending, so the first hit is the best one.
    private Promotion bestPromotionFor(BarberService service, LocalDate date) {
        List<Promotion> applicable = promotionRepository.findApplicable(service.getId(), date);
        return applicable.isEmpty() ? null : applicable.get(0);
    }

    private Barber requireBarber() throws EntityInvalidArgumentException {
        return barberRepository.findFirstByDeletedFalseOrderByIdAsc()
                .orElseThrow(() -> new EntityInvalidArgumentException("Barber",
                        "No barber profile exists - check the seed migration"));
    }
}
