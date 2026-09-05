package gr.aueb.cf.barberapp.service;

import gr.aueb.cf.barberapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.barberapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.barberapp.dto.PromotionInsertDTO;
import gr.aueb.cf.barberapp.dto.PromotionReadOnlyDTO;
import gr.aueb.cf.barberapp.dto.PromotionUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface IPromotionService {

    // What the public landing page advertises today.
    List<PromotionReadOnlyDTO> getActivePromotions();

    Page<PromotionReadOnlyDTO> getPaginatedPromotions(Pageable pageable);

    PromotionReadOnlyDTO savePromotion(PromotionInsertDTO dto)
            throws EntityNotFoundException, EntityInvalidArgumentException;

    PromotionReadOnlyDTO updatePromotion(PromotionUpdateDTO dto)
            throws EntityNotFoundException, EntityInvalidArgumentException;

    PromotionReadOnlyDTO deletePromotion(UUID uuid) throws EntityNotFoundException;
}
