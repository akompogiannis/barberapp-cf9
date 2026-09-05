package gr.aueb.cf.barberapp.service;

import gr.aueb.cf.barberapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.barberapp.dto.BarberProfileDTO;

public interface IBarberProfileService {

    // Everything the public landing page needs, in one call.
    BarberProfileDTO getProfile() throws EntityNotFoundException;
}
