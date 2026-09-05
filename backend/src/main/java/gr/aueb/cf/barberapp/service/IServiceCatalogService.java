package gr.aueb.cf.barberapp.service;

import gr.aueb.cf.barberapp.core.exceptions.EntityAlreadyExistsException;
import gr.aueb.cf.barberapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.barberapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.barberapp.dto.BarberServiceInsertDTO;
import gr.aueb.cf.barberapp.dto.BarberServiceReadOnlyDTO;
import gr.aueb.cf.barberapp.dto.BarberServiceUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

// The catalogue of bookable services. Named for the catalogue rather than the
// entity to avoid a "BarberServiceService".
public interface IServiceCatalogService {

    // The public catalogue: active services, priced with any live promotion.
    List<BarberServiceReadOnlyDTO> getActiveServices();

    BarberServiceReadOnlyDTO getServiceByUuid(UUID uuid) throws EntityNotFoundException;

    Page<BarberServiceReadOnlyDTO> getPaginatedServices(Pageable pageable);

    BarberServiceReadOnlyDTO saveService(BarberServiceInsertDTO dto)
            throws EntityAlreadyExistsException, EntityInvalidArgumentException;

    BarberServiceReadOnlyDTO updateService(BarberServiceUpdateDTO dto)
            throws EntityNotFoundException, EntityAlreadyExistsException;

    // Soft delete. Past appointments keep pointing at the service.
    BarberServiceReadOnlyDTO deleteService(UUID uuid) throws EntityNotFoundException;
}
