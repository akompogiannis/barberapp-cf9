package gr.aueb.cf.barberapp.service;

import gr.aueb.cf.barberapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.barberapp.dto.BarberProfileDTO;
import gr.aueb.cf.barberapp.mapper.Mapper;
import gr.aueb.cf.barberapp.model.Barber;
import gr.aueb.cf.barberapp.repository.BarberRepository;
import gr.aueb.cf.barberapp.repository.WorkingHoursRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Assembles the public shop window.
@Service                        // IoC Container
@RequiredArgsConstructor        // DI
@Slf4j                          // Logger
public class BarberProfileServiceImpl implements IBarberProfileService {

    private final BarberRepository barberRepository;
    private final WorkingHoursRepository workingHoursRepository;
    private final IServiceCatalogService serviceCatalogService;
    private final IPromotionService promotionService;
    private final Mapper mapper;

    @Override
    @Transactional(readOnly = true)
    public BarberProfileDTO getProfile() throws EntityNotFoundException {
        Barber barber = barberRepository.findFirstByDeletedFalseOrderByIdAsc()
                .orElseThrow(() -> new EntityNotFoundException("Barber",
                        "No barber profile has been set up yet"));

        return mapper.mapToBarberProfileDTO(
                barber,
                serviceCatalogService.getActiveServices(),
                promotionService.getActivePromotions(),
                workingHoursRepository
                        .findAllByBarber_IdAndDeletedFalseOrderByDayOfWeekAscStartTimeAsc(barber.getId())
        );
    }
}
